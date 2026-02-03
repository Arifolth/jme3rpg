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

package ru.arifolth.anjrpg.worldmap;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;

import java.util.logging.Logger;

public class WorldMapState extends BaseAppState implements ActionListener {
    private static final Logger LOGGER = Logger.getLogger(WorldMapState.class.getName());

    private static final String TOGGLE_MAP = "TOGGLE_MAP";
    private static final String ESCAPE_MAP = "ESCAPE_MAP";

    private final GameLogicCoreInterface gameLogicCore;
    private InputManager inputManager;

    private Node worldMapNode;
    private Geometry mapBackground;
    private Geometry mapBorder;
    private float mapWidth;
    private float mapHeight;
    private float screenWidth;
    private float screenHeight;
    private boolean isMapVisible = false;

    private float mapMargin;

    public WorldMapState(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    protected void initialize(Application app) {
        this.inputManager = app.getInputManager();

        // Setup input mappings
        inputManager.addMapping(TOGGLE_MAP, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addListener(this, TOGGLE_MAP);
        inputManager.addMapping(ESCAPE_MAP, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(this, ESCAPE_MAP);

        initMapDimensions();

        // Container covers the full screen to hold the full-screen border
        this.worldMapNode = new Node("WorldMapNode");
        this.worldMapNode.setLocalTranslation(0, 0, 5);

        // Create map elements following the requested Z-order
        createBorder();     // Z=0, Full Screen
        createBackground(); // Z=1, centered with uniform margins
        addPlaceholderGrid(); // Z=2, On top of background

        // Initially hidden
        worldMapNode.setCullHint(Node.CullHint.Always);

        LOGGER.info("WorldMapState initialized");
    }

    private void initMapDimensions() {
        Camera cam = getApplication().getCamera();
        screenWidth = cam.getWidth();
        screenHeight = cam.getHeight();

        // Use a fixed uniform margin based on screen height to ensure border size
        mapMargin = screenHeight * 0.011f;

        mapWidth = screenWidth - (mapMargin * 2);
        mapHeight = screenHeight - (mapMargin * 2);
    }

    private void createBorder() {
        try {
            // Border is 100% of screen size
            Quad borderQuad = new Quad(screenWidth, screenHeight);
            mapBorder = new Geometry("MapBorder", borderQuad);
            Material borderMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            Texture borderTex = getApplication().getAssetManager().loadTexture("Textures/Compass/compass_brass_wheel3.png");
            borderMat.setTexture("ColorMap", borderTex);
            mapBorder.setMaterial(borderMat);

            // Z=0: Lowest overlay element
            mapBorder.setLocalTranslation(0, 0, 0);

            worldMapNode.attachChild(mapBorder);
        } catch (Exception e) {
            LOGGER.warning("Could not load border texture: " + e.getMessage());
        }
    }

    private void createBackground() {
        // Create background quad
        Quad backgroundQuad = new Quad(mapWidth, mapHeight);
        mapBackground = new Geometry("MapBackground", backgroundQuad);

        // Create material with gray color (0.2, 0.2, 0.2, 0.8)
        Material backgroundMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        backgroundMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.8f));
        mapBackground.setMaterial(backgroundMat);

        // Z=1: Middle tier, on top of border
        // Use uniform margins to center exactly
        mapBackground.setLocalTranslation(mapMargin, mapMargin, 1);

        worldMapNode.attachChild(mapBackground);
    }

    private void addPlaceholderGrid() {
        // Add a simple grid to show where tiles will go
        // Grid lines at Z=2 to be on top of background

        Material lineMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.3f, 0.5f));
        lineMat.getAdditionalRenderState().setLineWidth(1f);

        // Start drawing from the margin offset
        float startX = mapMargin;
        float startY = mapMargin;

        // Example: Draw a 10x10 grid within the map area
        int gridSize = 10;
        float cellWidth = mapWidth / gridSize;
        float cellHeight = mapHeight / gridSize;

        Node placeholderGrid = new Node("PlaceholderGrid");
        // Z=2 to ensure it's on top of background and border
        placeholderGrid.setLocalTranslation(0, 0, 2);
        worldMapNode.attachChild(placeholderGrid);

        for (int i = 0; i <= gridSize; i++) {
            // Vertical lines
            Geometry vertLine = createLine(
                    startX + i * cellWidth, startY,
                    startX + i * cellWidth, startY + mapHeight,
                    lineMat
            );
            placeholderGrid.attachChild(vertLine);

            // Horizontal lines
            Geometry horizLine = createLine(
                    startX, startY + i * cellHeight,
                    startX + mapWidth, startY + i * cellHeight,
                    lineMat
            );
            placeholderGrid.attachChild(horizLine);
        }
    }

    private Geometry createLine(float x1, float y1, float x2, float y2, Material mat) {
        // Simple line implementation for grid using Line mesh
        Vector3f start = new Vector3f(x1, y1, 2);
        Vector3f end = new Vector3f(x2, y2, 2);

        Line lineMesh = new Line(start, end);
        Geometry line = new Geometry("GridLine", lineMesh);
        line.setMaterial(mat);

        return line;
    }

    public void toggleMap() {
        isMapVisible = !isMapVisible;

        if (isMapVisible) {
            showMap();
        } else {
            hideMap();
        }
    }

    public void showMap() {
        if (worldMapNode.getParent() == null) {
            // Attach to GUI node for 2D overlay
            ((SimpleApplication) getApplication()).getGuiNode().attachChild(worldMapNode);
        }
        worldMapNode.setCullHint(Node.CullHint.Never);
        isMapVisible = true;
        LOGGER.info("World map shown");
    }

    public void hideMap() {
        worldMapNode.setCullHint(Node.CullHint.Always);
        isMapVisible = false;
        LOGGER.info("World map hidden");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            if (name.equals(TOGGLE_MAP)) {
                toggleMap();
            } else if (name.equals(ESCAPE_MAP) && isMapVisible) {
                hideMap();
            }
        }
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    protected void onEnable() {
        // Nothing additional needed
    }

    @Override
    protected void onDisable() {
        // Hide map when state is disabled
        hideMap();
    }

    @Override
    protected void cleanup(Application app) {
        if (worldMapNode != null && worldMapNode.getParent() != null) {
            worldMapNode.removeFromParent();
        }
    }
}
