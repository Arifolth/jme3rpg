/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.compass.CompassInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;

import java.util.*;

public class Compass implements CompassInterface {
    private SimpleApplication app;
    private Node compassNode;
    private Geometry compassWheel;
    private Geometry compassFrame;
    private Material compassMaterial;
    private Material frameMaterial;
    private Collection<POIInterface> pointsOfInterest;
    private Map<String, Geometry> targetIndicators;
    private float screenWidth;
    private float screenHeight;
    private float compassWidth;
    private float compassHeight;

    public Compass(SimpleApplication application) {
        this.app = application;
        this.pointsOfInterest = new HashSet<>();
        this.targetIndicators = new HashMap<>();

        initializeCompass();
    }

    private void initializeCompass() {
        compassNode = new Node("CompassHUD");
        screenWidth = app.getCamera().getWidth();
        screenHeight = app.getCamera().getHeight();

        // Percentage-based sizing instead of hardcoded one
        compassWidth = screenWidth * 0.175f;   // 17.5% of screen width
        compassHeight = screenHeight * 0.05f; // 5% of screen height

        initializeCompassWheel();
        initializeCompassFrame();
        positionCompass();
        createTopBarMiddlePointer();
    }


    private void positionCompass() {
        float screenQuarterSize = screenHeight * 0.33f; // height and width
        final float verticalSpacing = screenHeight * 0.046f;
        final float uiPadding = screenHeight * 0.02f;

        float screenQuarterLeftEdge = screenWidth - screenQuarterSize - uiPadding;

        // Calculate screen quarter horizontal center
        float screenQuarterCenterX = screenQuarterLeftEdge + screenQuarterSize / 2;

        float frameBorderWidth = screenHeight * 0.001f;

        float frameWidth = compassWidth + frameBorderWidth * 2;

        // Compute compass X so its center aligns with screenQuarterCenterX
        float compassX = screenQuarterCenterX - (frameWidth / 2);

        // Y position remains below screen quarter with vertical spacing
        float compassY = screenHeight - screenQuarterSize - uiPadding - verticalSpacing - compassHeight;

        compassNode.setLocalTranslation(compassX, compassY, 1f);
    }

    private void initializeCompassWheel() {
        // Create quad geometry for compass wheel
        Quad compassQuad = new Quad(compassWidth, compassHeight);
        compassWheel = new Geometry("CompassWheel", compassQuad);

        // Create custom material with scrolling capability
        compassMaterial = new Material(app.getAssetManager(),
                "Common/MatDefs/Compass/ScrollingCompass.j3md");

        // Load brass texture with cardinal directions
        Texture compassTexture = app.getAssetManager().loadTexture(
                "Textures/Compass/compass_brass_wheel.png");
        compassTexture.setWrap(Texture.WrapMode.Repeat);

        // Configure material properties for brass appearance
        compassMaterial.setTexture("ColorMap", compassTexture);
        compassMaterial.setFloat("Metallic", 0.8f);
        compassMaterial.setFloat("Roughness", 0.3f);
        compassMaterial.setColor("Specular", new ColorRGBA(1.0f, 0.9f, 0.7f, 1.0f));

        compassWheel.setMaterial(compassMaterial);
        compassNode.attachChild(compassWheel);
    }

    private void initializeCompassFrame() {
        float frameBorderWidth = Math.max(1.0f, screenHeight * 0.001f); // 0.1% of screen height, minimum 1px

        float frameWidth = compassWidth + (frameBorderWidth * 2);
        float frameHeight = compassHeight + (frameBorderWidth * 2);

        Quad frameQuad = new Quad(frameWidth, frameHeight);
        compassFrame = new Geometry("CompassFrame", frameQuad);
        frameMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        Texture borderTexture = app.getAssetManager().loadTexture("Textures/Compass/A_21_9_rectangular_frame_.png");
        frameMaterial.setTexture("ColorMap", borderTexture);
        compassFrame.setMaterial(frameMaterial);

        // Position frame with ultra-minimal offset
        compassFrame.setLocalTranslation(-frameBorderWidth, -frameBorderWidth, -0.1f);
        compassNode.attachChild(compassFrame);
    }

    public Collection<POIInterface> getPointsOfInterest() {
        return pointsOfInterest;
    }

    @Override
    public void setPointsOfInterest(Collection<POIInterface> pointsOfInterest) {
        this.pointsOfInterest = pointsOfInterest;
    }

    /**
     * Updates the horizontal scrolling offset of the compass wheel texture.
     * @param compassOffset normalized value [0..1] representing player's heading,
     *                      where 0 means North aligned at left edge of texture.
     */
    @Override
    public void updateCompassRotation(float compassOffset) {
        // Clamp compassOffset between 0 and 1
        compassOffset = compassOffset - (float)Math.floor(compassOffset);

        // Update the material uniform controlling texture scroll
        // Assuming your material has a float parameter "ScrollOffset" used in the vertex shader
        compassMaterial.setFloat("ScrollOffset", compassOffset);
    }

    public Map<String, Geometry> getTargetIndicators() {
        return targetIndicators;
    }

    /**
     * Retrieves the target indicator Geometry for the given POI ID.
     * If the indicator does not exist yet, it creates one, adds it to the compass node,
     * and returns it.
     *
     * @param poiId Unique identifier of the POI
     * @return Geometry representing the target indicator on the compass
     */
    @Override
    public Geometry getTargetIndicator(String poiId) {
        // Check if indicator already exists
        if (targetIndicators.containsKey(poiId)) {
            return targetIndicators.get(poiId);
        }

        // Create a new target indicator geometry (e.g., a small quad or icon)
        float indicatorSize = Constants.UI_PADDING; // size in pixels, adjust as needed
        Quad indicatorQuad = new Quad(indicatorSize, indicatorSize);
        Geometry indicatorGeom = new Geometry("TargetIndicator_" + poiId, indicatorQuad);

        // Load or create a material for the target indicator (e.g., a red arrow or marker)
        Material indicatorMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        Texture indicatorTex = app.getAssetManager().loadTexture("Textures/Compass/compass_target_marker.png");
        indicatorMat.setTexture("ColorMap", indicatorTex);
        indicatorMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        indicatorGeom.setMaterial(indicatorMat);

        // Set initial position; will be updated each frame
        indicatorGeom.setLocalTranslation(0, 0, 0.2f); // slightly in front of compass wheel

        // Attach to compass node so it renders on the HUD
        compassNode.attachChild(indicatorGeom);

        // Store in map for future retrieval
        targetIndicators.put(poiId, indicatorGeom);

        return indicatorGeom;
    }

    /**
     * Creates a static pointer geometry positioned at the middle of the top border of the compass.
     * The pointer is rotated upside down (180 degrees) and placed centered horizontally on the top edge.
     *
     * @return Geometry of the static top pointer
     */
    private void createTopBarMiddlePointer() {
        float pointerSize = Constants.UI_PADDING; // size of the pointer (same as bottom marker size)

        // Create a quad for the pointer
        Quad pointerQuad = new Quad(pointerSize, pointerSize);
        Geometry pointerGeom = new Geometry("StaticTopPointer", pointerQuad);

        // Load the pointer texture (reuse the target marker texture)
        Material pointerMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        Texture pointerTex = app.getAssetManager().loadTexture("Textures/Compass/compass_target_marker.png");
        pointerMat.setTexture("ColorMap", pointerTex);
        pointerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        pointerGeom.setMaterial(pointerMat);

        // Rotate 180 degrees around Z to flip it upside down
        pointerGeom.rotate(0, 0, FastMath.PI);

        // Position it centered horizontally on the top border of the compass quad
        float x = (compassWidth + (Constants.FRAME_BORDER_WIDTH * 2)) / 2;
        float y = (compassHeight + (compassHeight)) / 2f;
        pointerGeom.setLocalTranslation(x, y, 0.0f); // Z to render above compass

        // Attach to compass node so it moves with compass UI
        compassNode.attachChild(pointerGeom);
    }

    @Override
    public float getCompassWidth() {
        return compassWidth;
    }

    @Override
    public Node getCompassNode() {
        return compassNode;
    }
}