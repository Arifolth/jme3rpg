/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arifolth.anjrpg.compass;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.compass.CompassInterface;
import ru.arifolth.anjrpg.interfaces.compass.CompassStateInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;

import java.util.ArrayList;
import java.util.List;

import static ru.arifolth.anjrpg.interfaces.Constants.INITIAL_MOUNTAINS_DIRECTION;

public class CompassState extends BaseAppState implements CompassStateInterface {
    private CompassInterface compass;
    private CharacterInterface playerCharacter;

    private Node guiNode;

    private POITarget npcPOI;
    private POIInterface staticPOI;
    private List<POIInterface> testPOIs;

    @Override
    protected void initialize(Application app) {
        SimpleApplication application = (SimpleApplication) app;
        this.compass = new Compass(application);
        this.playerCharacter = ((ANJRpgInterface) application).getGameLogicCore().getPlayerCharacter();

        // Initialize static POI (e.g., mountains)
        staticPOI = new POITarget("mountains", INITIAL_MOUNTAINS_DIRECTION.clone().normalizeLocal(), "Mountains", POIType.LANDMARK);

        // Initialize NPC POI with dummy position (will update dynamically)
        npcPOI = new POITarget("npc_target", Vector3f.ZERO, "NPC", POIType.NPC);

        // Set initial POIs to just the static one
        /*compass.setPointsOfInterest(new ArrayList<POITarget>() {{
            add(staticPOI);
        }});*/

        addTestStaticPOIs(playerCharacter, compass);

        guiNode = ((SimpleApplication)app).getGuiNode();
    }

    @Override
    public void update(float tpf) {
        if (compass != null && playerCharacter != null) {
            final List<POIInterface> activePOIs = new ArrayList<>(2);
//            activePOIs.add(staticPOI); // Always include static POI
            activePOIs.addAll(testPOIs);

            if(playerCharacter.getLockedOnCharacter() != null) {
                Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
                Vector3f npcPos = playerCharacter.getLockedOnCharacter().getValue().getNode().getWorldTranslation();

                // Calculate normalized direction vector from player to NPC
                Vector3f direction = npcPos.subtract(playerPos).normalizeLocal(); //Player always face the enemy while locked
                npcPOI.setPosition(direction);

                activePOIs.add(npcPOI); // Add NPC POI if target exists
            }

            compass.setPointsOfInterest(activePOIs);

            // Proceed with compass rotation and indicator updates
            Vector3f playerForward = playerCharacter.getNode().getWorldRotation().mult(Vector3f.UNIT_Z);

            // Calculate compass offset for texture scrolling
            float compassOffset = calculateCompassOffset(FastMath.atan2(playerForward.x, playerForward.z));
            compass.updateCompassRotation(compassOffset);

            // Update target indicators
            updateTargetIndicators(playerForward, tpf);
        }
    }

    private void addTestStaticPOIs(CharacterInterface playerCharacter, CompassInterface compass) {
        Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
        float distance = 50f; // distance from player

        testPOIs = new ArrayList<>();

        testPOIs.add(new POITarget("north",      playerPos.add(new Vector3f(0f, 0f,  -distance)), "NORTH",      POIType.LANDMARK));

        testPOIs.add(new POITarget("east",       playerPos.add(new Vector3f(distance, 0f, 0f)), "EAST",       POIType.LANDMARK));
        testPOIs.add(new POITarget("south",      playerPos.add(new Vector3f(0f, 0f, distance)), "SOUTH",      POIType.LANDMARK));

        testPOIs.add(new POITarget("west",       playerPos.add(new Vector3f(-distance, 0f, 0f)), "WEST",       POIType.LANDMARK));

        compass.setPointsOfInterest(testPOIs);

        // Add world markers
        addVerticalMarkersForPOIs(testPOIs);
    }

    private void addVerticalMarkersForPOIs(List<POIInterface> pois) {
        Node rootNode = ((SimpleApplication)getApplication()).getRootNode();
        Node markersNode = new Node("Markers");
        float height = 80f; // Tall so visible from afar
        float radius = 0.2f; // Thin
        for (POIInterface poi : pois) {
            Cylinder cyl = new Cylinder(8, 16, radius, height, true);
            Geometry geom = new Geometry("POIVertical_" + poi.getId(), cyl);

            // Rotate cylinder from Z (default) to Y (vertical)
            geom.rotate(FastMath.HALF_PI, 0, 0);

            Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", poi.getType() == POIType.NPC ? ColorRGBA.Red : ColorRGBA.Blue);
            geom.setMaterial(mat);

            // Place base at POI position, raise so it stands vertically
            Vector3f pos = poi.getPosition();
            geom.setLocalTranslation(pos.x, pos.y + height / 2f, pos.z);

            markersNode.attachChild(geom);
        }

        rootNode.attachChild(markersNode);
    }

    private float calculateCompassOffset(float angle) {
        // Convert angle to [-π, π] range if needed
        angle = FastMath.normalize(angle, -FastMath.PI, FastMath.PI);

        // Calculate offset:
        // - North (0) should be 0.5
        // - East (π/2) should be 0.75
        // - South (±π) should be 0.0
        // - West (-π/2) should be 0.25
        float offset = 0.5f - (angle / FastMath.TWO_PI);

        // Normalize to [0, 1)
        offset = offset - (float)Math.floor(offset);
        return offset;
    }

    private void updateTargetIndicators(Vector3f playerForward, float tpf) {
        Vector3f playerPosition = playerCharacter.getNode().getWorldTranslation();

        for (POIInterface poi : compass.getPointsOfInterest()) {
            Vector3f toTarget = poi.getPosition().subtract(playerPosition);
            toTarget.y = 0;

            if (toTarget.lengthSquared() < 1.0f) {
                // Hide when very close
                Geometry targetIndicator = compass.getTargetIndicator(poi.getId());
                if (targetIndicator != null) {
                    targetIndicator.setCullHint(Spatial.CullHint.Always);
                }
                continue;
            }

            Vector3f directionToTarget = toTarget.normalize();

            // Calculate angle between player forward and target direction
            float angle = calculateRelativeAngle(playerForward, directionToTarget);

            Geometry targetIndicator = compass.getTargetIndicator(poi.getId());
            if (targetIndicator != null) {
                targetIndicator.setCullHint(Spatial.CullHint.Inherit);

                float compassPosition = calculateTargetCompassPosition(angle);
                Vector3f currentPos = targetIndicator.getLocalTranslation();
                Vector3f targetPos = new Vector3f(compassPosition, currentPos.y, currentPos.z);

                // Use immediate positioning without interpolation
                targetIndicator.setLocalTranslation(targetPos);
            }
        }
    }

    // New method to calculate relative angle properly
    private float calculateRelativeAngle(Vector3f forward, Vector3f toTarget) {
        // Project vectors onto XZ plane
        Vector3f a = new Vector3f(forward.x, 0, forward.z).normalizeLocal();
        Vector3f b = new Vector3f(toTarget.x, 0, toTarget.z).normalizeLocal();

        // Calculate angle using atan2 for full 360-degree range
        float angle = FastMath.atan2(a.x*b.z - a.z*b.x, a.dot(b));
        return angle;
    }

    private float calculateTargetCompassPosition(float angle) {
        if (angle < 0) angle += FastMath.TWO_PI;

        float position = (angle / FastMath.TWO_PI) * Constants.COMPASS_WIDTH;

        // Account for frame border + additional offset
        position -= Constants.FRAME_BORDER_WIDTH * 2;

        return position;
    }

    @Override
    protected void cleanup(Application app) {
        // Remove compass visuals if needed
    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(compass.getCompassNode());
    }

    @Override
    protected void onDisable() {
        compass.getCompassNode().removeFromParent();
    }
}
