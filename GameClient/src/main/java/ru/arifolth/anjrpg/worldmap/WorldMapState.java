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

package ru.arifolth.anjrpg.worldmap;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;

import java.util.Optional;
import java.util.logging.Logger;

public class WorldMapState extends BaseAppState implements ActionListener, AnalogListener {
    private static final Logger LOGGER = Logger.getLogger(WorldMapState.class.getName());

    private static final String TOGGLE_MAP = "TOGGLE_MAP";
    private static final String ESCAPE_MAP = "ESCAPE_MAP";
    private static final String PAN_UP = "PAN_UP";
    private static final String PAN_DOWN = "PAN_DOWN";
    private static final String PAN_LEFT = "PAN_LEFT";
    private static final String PAN_RIGHT = "PAN_RIGHT";

    private final GameLogicCoreInterface gameLogicCore;
    private InputManager inputManager;

    private Node worldMapNode;
    private Node mapTilesNode; // Holds the dynamic subquad meshes
    private Geometry mapBorder;

    private float mapWidth;
    private float mapHeight;
    private float screenWidth;
    private float screenHeight;
    private float mapMargin;
    private boolean isMapVisible = false;

    // Viewport and Panning State
    private float panX = 0f; // in subquad units
    private float panY = 0f; // in subquad units
    private final float PAN_SPEED = 4.0f; // Subquads per second
    private float subquadWidth;
    private float subquadHeight;

    public WorldMapState(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    protected void initialize(Application app) {
        this.inputManager = app.getInputManager();

        // 1. Setup Input Mappings
        inputManager.addMapping(TOGGLE_MAP, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping(ESCAPE_MAP, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping(PAN_UP, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(PAN_DOWN, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(PAN_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(PAN_RIGHT, new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(this, TOGGLE_MAP, ESCAPE_MAP);
        inputManager.addListener(this, PAN_UP, PAN_DOWN, PAN_LEFT, PAN_RIGHT);

        initMapDimensions();

        this.worldMapNode = new Node("WorldMapNode");
        this.worldMapNode.setLocalTranslation(0, 0, 5);

        createBorder();

        // 2. Container for our 16 dynamic subquad meshes
        this.mapTilesNode = new Node("MapTilesNode");
        // Center the tiles area within the border
        this.mapTilesNode.setLocalTranslation(mapMargin, mapMargin, 1);
        this.worldMapNode.attachChild(mapTilesNode);

        worldMapNode.setCullHint(Node.CullHint.Always);
    }

    private void initMapDimensions() {
        screenWidth = getApplication().getCamera().getWidth();
        screenHeight = getApplication().getCamera().getHeight();

        mapMargin = screenHeight * 0.011f;
        mapWidth = screenWidth - (mapMargin * 2);
        mapHeight = screenHeight - (mapMargin * 2);

        // We want exactly 4x4 subquads visible to occupy the whole screen container
        subquadWidth = mapWidth / 4f;
        subquadHeight = mapHeight / 4f;
    }

    private void createBorder() {
        // Keeps your existing border setup untouched
        Quad borderQuad = new Quad(screenWidth, screenHeight);
        mapBorder = new Geometry("MapBorder", borderQuad);
        Material borderMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture borderTex = getApplication().getAssetManager().loadTexture("Textures/Compass/compass_brass_wheel3.png");
            borderMat.setTexture("ColorMap", borderTex);
        } catch (Exception e) {
            LOGGER.warning("Could not load border texture: " + e.getMessage());
        }
        mapBorder.setMaterial(borderMat);
        mapBorder.setLocalTranslation(0, 0, 0);
        worldMapNode.attachChild(mapBorder);
    }

    private void renderVisibleTiles() {
        mapTilesNode.detachAllChildren();

        int startSubX = (int) Math.floor(panX);
        int startSubY = (int) Math.floor(panY);

        float fractionalPanX = panX - startSubX;
        float fractionalPanY = panY - startSubY;

        float offsetX = -fractionalPanX * subquadWidth;
        float offsetY = -fractionalPanY * subquadHeight;

        TextureCache cache = (TextureCache) TextureCache.getInstance();

        // 5x5 grid
        for (int row = -1; row <= 4; row++) {
            for (int col = -1; col <= 4; col++) {

                int worldSubX = startSubX + col;
                int worldSubY = startSubY + row;

                // BITWISE SHIFT is the only safe way to map negatives in Java
                int quadX = worldSubX >> 1;
                int quadY = worldSubY >> 1;

                // AND operation is the only safe way to get positive remainders for negatives
                int subX = worldSubX & 1;
                int subY = worldSubY & 1;

                String tileId = String.format("tile_%d_%d_sub_%d_%d", quadX, quadY, subX, subY);

                Quad quadMesh = new Quad(subquadWidth, subquadHeight);
                Geometry geom = new Geometry(tileId, quadMesh);
                Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

                if (cache.containsTexture(tileId)) {
                    Texture texture = cache.getTexture(tileId).get();
//                    texture.setWrap(Texture.WrapMode.Repeat);

                    mat.setTexture("ColorMap", texture);
                    mat.setColor("Color", ColorRGBA.White);
                } else {
                    boolean isEven = ((worldSubX + worldSubY) & 1) == 0;
                    mat.setColor("Color", isEven ? new ColorRGBA(0.4f, 0.4f, 0.4f, 1f) : new ColorRGBA(0.25f, 0.25f, 0.25f, 1f));
                }

                geom.setMaterial(mat);

                float drawX = offsetX + (col * subquadWidth);
                float drawY = offsetY + (row * subquadHeight);
                geom.setLocalTranslation(drawX, drawY, 0);

                mapTilesNode.attachChild(geom);
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isMapVisible) return;

        // 3. Smooth panning with W/A/S/D
        if (name.equals(PAN_UP)) {
            panY += PAN_SPEED * tpf;
        } if (name.equals(PAN_DOWN)) {
            panY -= PAN_SPEED * tpf;
        } if (name.equals(PAN_RIGHT)) {
            panX += PAN_SPEED * tpf;
        } if (name.equals(PAN_LEFT)) {
            panX -= PAN_SPEED * tpf;
        }
    }

    public void toggleMap() {
        isMapVisible = !isMapVisible;
        if (isMapVisible) showMap();
        else hideMap();
    }

    public void showMap() {
        if (worldMapNode.getParent() == null) {
            ((SimpleApplication) getApplication()).getGuiNode().attachChild(worldMapNode);
        }
        worldMapNode.setCullHint(Node.CullHint.Never);
        isMapVisible = true;
        renderVisibleTiles(); // Initial render setup upon opening the map
    }

    public void hideMap() {
        worldMapNode.setCullHint(Node.CullHint.Always);
        isMapVisible = false;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            if (name.equals(TOGGLE_MAP)) toggleMap();
            else if (name.equals(ESCAPE_MAP) && isMapVisible) hideMap();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (worldMapNode != null && worldMapNode.getParent() != null) {
            worldMapNode.removeFromParent();
        }
    }

    // Unchanged lifecycle methods...
    @Override public void update(float tpf) {
        renderVisibleTiles(); // Re-render / translate immediately
    }
    @Override protected void onEnable() {}
    @Override protected void onDisable() { hideMap(); }
}