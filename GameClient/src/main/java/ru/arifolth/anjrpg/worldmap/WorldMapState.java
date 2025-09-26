/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2025 Alexander Nilov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.compass.CompassStateInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.worldmap.WorldMapStateInterface;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WorldMapState extends BaseAppState implements ActionListener, WorldMapStateInterface {

    private static final Logger LOGGER = Logger.getLogger(WorldMapState.class.getName());

    private static final String TOGGLE_MAP = "TOGGLE_MAP";
    private static final String ESCAPE_MAP = "ESCAPE_MAP";

    private SimpleApplication app;
    private InputManager inputManager;
    private Node guiNode;
    private CharacterInterface playerCharacter;
    private CompassStateInterface compassState;

    private WorldMap worldMap;
    private boolean mapVisible = false;

    // Tile configuration
    private static final int TILE_TEXTURE_SIZE = 1024;
    private static final float TILE_WORLD_SIZE = 400f;
    private static final float TILE_MAP_SIZE = 600f;

    // Map center and panning
    private Vector3f mapCenter = new Vector3f();
    private Vector3f panOffset = new Vector3f(0, 0, 0);
    private Set<String> panKeysPressed = new HashSet<>();
    private static final float PAN_SPEED = 800f;

    // Tile discovery and caching
    private Set<String> discoveredTiles = ConcurrentHashMap.newKeySet();
    private final Map<String, Texture2D> tileTextureCache = new ConcurrentHashMap<>();
    private final Map<String, Vector3f> tileWorldPositions = new ConcurrentHashMap<>();
    private static final int MAX_CACHED_TILES = 200;

    // Current visible grid
    private int currentGridCenterX = 0;
    private int currentGridCenterZ = 0;

    // Texture capture management
    private final Set<String> tilesPendingCapture = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> tileCaptureAttempts = new ConcurrentHashMap<>();
    private static final int MAX_CAPTURE_ATTEMPTS = 60; // ~1 second at 60fps

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.inputManager = this.app.getInputManager();
        this.guiNode = this.app.getGuiNode();
        this.playerCharacter = ((ANJRpgInterface) this.app).getGameLogicCore().getPlayerCharacter();
        this.compassState = this.app.getStateManager().getState(ru.arifolth.anjrpg.compass.CompassState.class);

        setupWorldMap();
        setupInputMappings();

        LOGGER.info("WorldMapState initialized with improved texture capture.");
    }

    private void setupWorldMap() {
        worldMap = new WorldMap(app, this, TILE_MAP_SIZE);
        worldMap.setVisible(false);
    }

    private void setupInputMappings() {
        inputManager.addMapping(TOGGLE_MAP, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addListener(this, TOGGLE_MAP);

        inputManager.addMapping(ESCAPE_MAP, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(this, ESCAPE_MAP);

        inputManager.addMapping("PAN_UP", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("PAN_DOWN", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("PAN_LEFT", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("PAN_RIGHT", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(this, "PAN_UP", "PAN_DOWN", "PAN_LEFT", "PAN_RIGHT");
    }

    @Override
    public void update(float tpf) {
        if (!mapVisible) return;

        updatePanning(tpf);
        updateMapCenter();
        updateGridCenter();
        updateDiscoveredTiles();
        updateTextureCapture();
        updatePOIMarkers();
    }

    private void updatePanning(float tpf) {
        Vector3f panDelta = new Vector3f();
        if (panKeysPressed.contains("PAN_UP")) panDelta.z += PAN_SPEED * tpf;
        if (panKeysPressed.contains("PAN_DOWN")) panDelta.z -= PAN_SPEED * tpf;
        if (panKeysPressed.contains("PAN_LEFT")) panDelta.x -= PAN_SPEED * tpf;
        if (panKeysPressed.contains("PAN_RIGHT")) panDelta.x += PAN_SPEED * tpf;

        if (panDelta.lengthSquared() > 0) {
            panOffset.addLocal(panDelta);
        }
    }

    private void updateMapCenter() {
        if (playerCharacter != null) {
            Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
            mapCenter.set(playerPos.x + panOffset.x, 0, playerPos.z + panOffset.z);
        }
    }

    private void updateGridCenter() {
        if (playerCharacter != null) {
            Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
            currentGridCenterX = worldToTileX(playerPos.x);
            currentGridCenterZ = worldToTileZ(playerPos.z);
        }
    }

    private void updateDiscoveredTiles() {
        if (playerCharacter == null) return;

        Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
        int currentTileX = worldToTileX(playerPos.x);
        int currentTileZ = worldToTileZ(playerPos.z);

        // Discover 3x3 grid around player
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int tileX = currentTileX + dx;
                int tileZ = currentTileZ + dz;
                String tileId = tileX + "," + tileZ;

                if (!discoveredTiles.contains(tileId)) {
                    discoverTile(tileId, tileX, tileZ);
                }
            }
        }

        worldMap.updateTileGrid(discoveredTiles, currentGridCenterX, currentGridCenterZ);
    }

    private int worldToTileX(float worldX) {
        return (int) Math.floor(worldX / TILE_WORLD_SIZE);
    }

    private int worldToTileZ(float worldZ) {
        return (int) Math.floor(worldZ / TILE_WORLD_SIZE);
    }

    private void discoverTile(String tileId, int tileX, int tileZ) {
        discoveredTiles.add(tileId);

        Vector3f tileWorldPos = new Vector3f(
                (tileX * TILE_WORLD_SIZE) + (TILE_WORLD_SIZE / 2),
                0,
                (tileZ * TILE_WORLD_SIZE) + (TILE_WORLD_SIZE / 2)
        );
        tileWorldPositions.put(tileId, tileWorldPos);

        // Schedule texture capture instead of immediate capture
        scheduleTextureCapture(tileId, tileWorldPos);

        LOGGER.info("Discovered tile: " + tileId + " at " + tileWorldPos);
    }

    private void scheduleTextureCapture(String tileId, Vector3f tileWorldPos) {
        if (!tileTextureCache.containsKey(tileId) && !tilesPendingCapture.contains(tileId)) {
            tilesPendingCapture.add(tileId);
            tileCaptureAttempts.put(tileId, 0);
            LOGGER.fine("Scheduled texture capture for tile: " + tileId);
        }
    }

    private void updateTextureCapture() {
        if (tilesPendingCapture.isEmpty()) return;

        // Process one tile per frame to avoid performance spikes
        String tileId = tilesPendingCapture.iterator().next();
        tilesPendingCapture.remove(tileId);

        Vector3f tileWorldPos = tileWorldPositions.get(tileId);
        if (tileWorldPos != null) {
            captureTileTexture(tileId, tileWorldPos);
        }
    }

    private void captureTileTexture(String tileId, Vector3f tileWorldPos) {
        if (tileTextureCache.containsKey(tileId)) return;

        // Manage cache size
        if (tileTextureCache.size() >= MAX_CACHED_TILES) {
            removeOldestCachedTile();
        }

        try {
            // Create camera for tile capture
            Camera tileCam = createTileCamera(tileWorldPos);

            // Create framebuffer and texture
            FrameBuffer tileBuffer = new FrameBuffer(TILE_TEXTURE_SIZE, TILE_TEXTURE_SIZE, 1);
            Texture2D tileTexture = new Texture2D(TILE_TEXTURE_SIZE, TILE_TEXTURE_SIZE, Image.Format.RGBA8);
            tileTexture.setMinFilter(Texture2D.MinFilter.Trilinear);
            tileTexture.setMagFilter(Texture2D.MagFilter.Bilinear);
            tileBuffer.setColorTexture(tileTexture);
            tileBuffer.setDepthBuffer(Image.Format.Depth);

            // Create viewport for tile rendering
            ViewPort tileViewPort = app.getRenderManager().createMainView("TileView_" + tileId, tileCam);
            tileViewPort.setClearFlags(true, true, true);
            tileViewPort.setBackgroundColor(new com.jme3.math.ColorRGBA(0.1f, 0.1f, 0.1f, 1f)); // Dark background for contrast
            tileViewPort.setOutputFrameBuffer(tileBuffer);

            // Attach only the terrain and static objects, not the entire scene
            attachRelevantSceneObjects(tileViewPort);

            // Store texture
            tileTextureCache.put(tileId, tileTexture);

            LOGGER.info("Captured texture for tile: " + tileId + " - Texture will be available next frame");

            // Schedule cleanup for next frame
            app.enqueue(() -> {
                app.getRenderManager().removeMainView(tileViewPort);
                tileBuffer.dispose();
            });

        } catch (Exception e) {
            LOGGER.warning("Failed to capture texture for tile " + tileId + ": " + e.getMessage());

            // Retry if we haven't exceeded max attempts
            int attempts = tileCaptureAttempts.getOrDefault(tileId, 0);
            if (attempts < MAX_CAPTURE_ATTEMPTS) {
                tileCaptureAttempts.put(tileId, attempts + 1);
                tilesPendingCapture.add(tileId); // Retry
                LOGGER.fine("Scheduling retry " + (attempts + 1) + " for tile: " + tileId);
            } else {
                LOGGER.warning("Giving up on texture capture for tile: " + tileId);
                tileCaptureAttempts.remove(tileId);
            }
        }
    }

    private Camera createTileCamera(Vector3f tileWorldPos) {
        Camera tileCam = new Camera(TILE_TEXTURE_SIZE, TILE_TEXTURE_SIZE);
        tileCam.setParallelProjection(true);

        // Set frustum to cover the entire tile area
        float halfTileSize = TILE_WORLD_SIZE * 0.5f;
        tileCam.setFrustumLeft(-halfTileSize);
        tileCam.setFrustumRight(halfTileSize);
        tileCam.setFrustumBottom(-halfTileSize);
        tileCam.setFrustumTop(halfTileSize);
        tileCam.setFrustumNear(0.1f);
        tileCam.setFrustumFar(2000f);

        // Position camera high above looking straight down
        tileCam.setLocation(new Vector3f(tileWorldPos.x, 1000f, tileWorldPos.z));
        tileCam.lookAtDirection(new Vector3f(0, -1, 0), Vector3f.UNIT_Z); // Look straight down

        return tileCam;
    }

    private void attachRelevantSceneObjects(ViewPort tileViewPort) {
        // Attach the root node which contains the terrain
        // In JME3, the terrain should be attached to the rootNode
        if (app.getRootNode() != null) {
            tileViewPort.attachScene(app.getRootNode());
        }

        // You might want to exclude dynamic objects like characters, effects, etc.
        // For a clean map view, we only want terrain and static geometry
    }

    private void removeOldestCachedTile() {
        if (tileTextureCache.isEmpty()) return;

        String oldestTileId = tileTextureCache.keySet().iterator().next();
        tileTextureCache.remove(oldestTileId);
        tileWorldPositions.remove(oldestTileId);
        LOGGER.fine("Removed oldest cached tile: " + oldestTileId);
    }

    private void updatePOIMarkers() {
        if (compassState == null || compassState.getCompass() == null || playerCharacter == null) return;

        Collection<POIInterface> pois = compassState.getCompass().getPointsOfInterest();
        worldMap.updatePOIMarkers(pois, playerCharacter, currentGridCenterX, currentGridCenterZ);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals(TOGGLE_MAP) && isPressed) {
            toggleMap();
            return;
        }

        if (name.equals(ESCAPE_MAP) && isPressed && mapVisible) {
            hideMap();
            return;
        }

        handlePanning(name, isPressed);
    }

    private void toggleMap() {
        if (mapVisible) {
            hideMap();
        } else {
            showMap();
        }
    }

    private void handlePanning(String name, boolean isPressed) {
        switch (name) {
            case "PAN_UP":
            case "PAN_DOWN":
            case "PAN_LEFT":
            case "PAN_RIGHT":
                if (isPressed) panKeysPressed.add(name);
                else panKeysPressed.remove(name);
                break;
        }
    }

    private void showMap() {
        if (!mapVisible) {
            mapVisible = true;
            guiNode.attachChild(worldMap.getMapNode());
            inputManager.setCursorVisible(true);

            panOffset.set(0, 0, 0);
            if (playerCharacter != null) {
                Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
                mapCenter.set(playerPos.x, 0, playerPos.z);
                updateGridCenter();
                updateDiscoveredTiles();
            }

            LOGGER.info("World map opened");
        }
    }

    private void hideMap() {
        if (mapVisible) {
            mapVisible = false;
            worldMap.getMapNode().removeFromParent();
            inputManager.setCursorVisible(false);
            panKeysPressed.clear();
            panOffset.set(0, 0, 0);
            LOGGER.info("World map closed.");
        }
    }

    // Getters for WorldMap
    public Map<String, Texture2D> getTileTextureCache() {
        return new HashMap<>(tileTextureCache);
    }

    public Map<String, Vector3f> getTileWorldPositions() {
        return new HashMap<>(tileWorldPositions);
    }

    public float getTileWorldSize() {
        return TILE_WORLD_SIZE;
    }

    public float getTileMapSize() {
        return TILE_MAP_SIZE;
    }

    public Vector3f getMapCenter() {
        return mapCenter.clone();
    }

    public CharacterInterface getPlayerCharacter() {
        return playerCharacter;
    }

    public int getCurrentGridCenterX() {
        return currentGridCenterX;
    }

    public int getCurrentGridCenterZ() {
        return currentGridCenterZ;
    }

    @Override
    public boolean isMapVisible() {
        return mapVisible;
    }

    @Override
    public Set<String> getDiscoveredTiles() {
        return new HashSet<>(discoveredTiles);
    }

    @Override
    public void addDiscoveredTile(String tileId) {
        if (!discoveredTiles.contains(tileId)) {
            try {
                String[] coords = tileId.split(",");
                if (coords.length == 2) {
                    int tileX = Integer.parseInt(coords[0]);
                    int tileZ = Integer.parseInt(coords[1]);
                    discoverTile(tileId, tileX, tileZ);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to parse tile coordinates for " + tileId);
            }
        }
    }

    @Override
    public void clearDiscoveredTiles() {
        discoveredTiles.clear();
        tileTextureCache.clear();
        tileWorldPositions.clear();
        tilesPendingCapture.clear();
        tileCaptureAttempts.clear();
        LOGGER.info("Cleared all discovered tiles and texture cache");
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.removeListener(this);
        inputManager.deleteMapping(TOGGLE_MAP);
        inputManager.deleteMapping(ESCAPE_MAP);
        inputManager.deleteMapping("PAN_UP");
        inputManager.deleteMapping("PAN_DOWN");
        inputManager.deleteMapping("PAN_LEFT");
        inputManager.deleteMapping("PAN_RIGHT");

        if (mapVisible) {
            hideMap();
        }

        clearDiscoveredTiles();
    }

    @Override
    protected void onEnable() {
        // Map visibility controlled by key presses
    }

    @Override
    protected void onDisable() {
        if (mapVisible) {
            hideMap();
        }
    }
}