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

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.ANJRpg;
import ru.arifolth.anjrpg.interfaces.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compass {
    private SimpleApplication app;
    private Node compassNode;
    private Geometry compassWheel;
    private Geometry compassFrame;
    private Material compassMaterial;
    private Material frameMaterial;
    private List<POITarget> pointsOfInterest;
    private Map<String, Geometry> targetIndicators;

    public Compass(SimpleApplication application) {
        this.app = application;
        this.pointsOfInterest = new ArrayList<>();
        this.targetIndicators = new HashMap<>();

        initializeCompass();
    }

    private void initializeCompass() {
        // Create the main compass node
        compassNode = new Node("CompassHUD");

        // Initialize compass wheel geometry
        initializeCompassWheel();

        // Initialize sandstone frame
        initializeCompassFrame();

        // Position compass in upper center of screen
        positionCompass();

        createTopBarMiddlePointer();
    }

    private void positionCompass() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float miniMapSize = ((float)((ANJRpg)app).getSettings().getHeight() / 3.0f);
        // Desired vertical spacing between minimap and compass
        final float verticalSpacing = 50f;

        // Compass dimensions (as defined previously)
        float compassWidth = Constants.COMPASS_WIDTH;
        float compassHeight = Constants.COMPASS_HEIGHT;

        // Calculate X position: align center with minimap
        float compassX = screenWidth - (miniMapSize / 10) - compassWidth;

        // Calculate Y position: place compass below minimap with spacing
        float compassY = screenHeight - miniMapSize - Constants.UI_PADDING - verticalSpacing - compassHeight;

        // Set local translation of the compass node
        compassNode.setLocalTranslation(compassX, compassY, 1f);
    }

    private void initializeCompassWheel() {
        // Create quad geometry for compass wheel
        Quad compassQuad = new Quad(Constants.COMPASS_WIDTH, Constants.COMPASS_HEIGHT);
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
        // Create slightly larger quad for frame
        float frameWidth = Constants.COMPASS_WIDTH + (Constants.FRAME_BORDER_WIDTH * 2);
        float frameHeight = Constants.COMPASS_HEIGHT + (Constants.FRAME_BORDER_WIDTH * 2);

        Quad frameQuad = new Quad(frameWidth, frameHeight);
        compassFrame = new Geometry("CompassFrame", frameQuad);

        frameMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        // Load your sandstone border texture (replace with your actual texture path)
        Texture borderTexture = app.getAssetManager().loadTexture("Textures/Compass/A_21_9_rectangular_frame_.png");

        // Set the texture as the ColorMap of the material
        frameMaterial.setTexture("ColorMap", borderTexture);
        compassFrame.setMaterial(frameMaterial);

        // Position frame behind compass wheel
        compassFrame.setLocalTranslation(-Constants.FRAME_BORDER_WIDTH, -Constants.FRAME_BORDER_WIDTH, -0.1f);
        compassNode.attachChild(compassFrame);
    }

    public List<POITarget> getPointsOfInterest() {
        return pointsOfInterest;
    }

    public void setPointsOfInterest(List<POITarget> pointsOfInterest) {
        this.pointsOfInterest = pointsOfInterest;
    }

    /**
     * Updates the horizontal scrolling offset of the compass wheel texture.
     * @param compassOffset normalized value [0..1] representing player's heading,
     *                      where 0 means North aligned at left edge of texture.
     */
    public void updateCompassRotation(float compassOffset) {
        // Clamp compassOffset between 0 and 1
        compassOffset = compassOffset - (float)Math.floor(compassOffset);

        // Update the material uniform controlling texture scroll
        // Assuming your material has a float parameter "ScrollOffset" used in the vertex shader
        compassMaterial.setFloat("ScrollOffset", compassOffset);
    }

    /**
     * Retrieves the target indicator Geometry for the given POI ID.
     * If the indicator does not exist yet, it creates one, adds it to the compass node,
     * and returns it.
     *
     * @param poiId Unique identifier of the POI
     * @return Geometry representing the target indicator on the compass
     */
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
        float x = (Constants.COMPASS_WIDTH + (Constants.FRAME_BORDER_WIDTH * 2)) / 2;
        float y = (Constants.COMPASS_HEIGHT + (Constants.COMPASS_HEIGHT)) / 2f;
        pointerGeom.setLocalTranslation(x, y, 0.0f); // Z to render above compass

        // Attach to compass node so it moves with compass UI
        compassNode.attachChild(pointerGeom);
    }


    public Node getCompassNode() {
        return compassNode;
    }
}