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
import com.jme3.math.ColorRGBA;
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

    private Camera mapCam;
    private ViewPort mapViewPort;
    private FrameBuffer offBuffer;
    private Texture2D offTexture;

    private WorldMap worldMap;

    private boolean mapVisible = false; // Start hidden!

    private int mapTextureSize;
    private Vector3f mapCenter;

    private Set<String> panKeysPressed = new HashSet<>();
    private Vector3f panOffset = new Vector3f(0, 0, 0);
    private static final float PAN_SPEED = 800f;

    private Set<String> discoveredTiles = ConcurrentHashMap.newKeySet();

    // Add tile caching functionality
    private final Map<String, Texture2D> tileTextureCache = new ConcurrentHashMap<>();
    private final Map<String, Vector3f> tileWorldPositions = new ConcurrentHashMap<>();
    private static final int MAX_CACHED_TILES = 200; // Memory management

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.inputManager = this.app.getInputManager();
        this.guiNode = this.app.getGuiNode();
        this.playerCharacter = ((ANJRpgInterface) this.app).getGameLogicCore().getPlayerCharacter();
        this.compassState = this.app.getStateManager().getState(ru.arifolth.anjrpg.compass.CompassState.class);

        mapTextureSize = this.app.getCamera().getHeight();
        mapCenter = new Vector3f();

        setupOffscreenRendering();
        setupWorldMap();
        setupInputMappings();

        LOGGER.info("WorldMapState initialized.");
    }

    private void setupOffscreenRendering() {
        mapCam = new Camera(mapTextureSize, mapTextureSize);
        mapCam.setParallelProjection(true);

        float worldViewRange = 2000f;
        mapCam.setFrustumLeft(-worldViewRange);
        mapCam.setFrustumRight(worldViewRange);
        mapCam.setFrustumBottom(-worldViewRange);
        mapCam.setFrustumTop(worldViewRange);
        mapCam.setFrustumNear(0.1f);
        mapCam.setFrustumFar(4000f);

        mapViewPort = app.getRenderManager().createMainView("WorldMapView", mapCam);
        mapViewPort.setClearFlags(true, true, true);
        mapViewPort.setBackgroundColor(new ColorRGBA(0.85f, 0.75f, 0.55f, 1f));

        offBuffer = new FrameBuffer(mapTextureSize, mapTextureSize, 1);
        offTexture = new Texture2D(mapTextureSize, mapTextureSize, Image.Format.RGBA8);
        offTexture.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
        offTexture.setMagFilter(Texture2D.MagFilter.Bilinear);
        offBuffer.setColorTexture(offTexture);
        offBuffer.setDepthBuffer(Image.Format.Depth);
        mapViewPort.setOutputFrameBuffer(offBuffer);

        mapViewPort.attachScene(app.getRootNode());
    }

    private void setupWorldMap() {
        worldMap = new WorldMap(app, offTexture);
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
        if (!mapVisible)
            return;

        Vector3f panDelta = new Vector3f();
        if (panKeysPressed.contains("PAN_UP")) panDelta.z += PAN_SPEED * tpf; // invert z pan for north up
        if (panKeysPressed.contains("PAN_DOWN")) panDelta.z -= PAN_SPEED * tpf;
        if (panKeysPressed.contains("PAN_LEFT")) panDelta.x -= PAN_SPEED * tpf;
        if (panKeysPressed.contains("PAN_RIGHT")) panDelta.x += PAN_SPEED * tpf;

        if (panDelta.lengthSquared() > 0) {
            panOffset.addLocal(panDelta);
        }

        if (playerCharacter != null) {
            Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();
            mapCenter.set(playerPos.x + panOffset.x, 0, playerPos.z + panOffset.z);
        }

        mapCam.setLocation(new Vector3f(mapCenter.x, 3500f, mapCenter.z));
        mapCam.lookAt(new Vector3f(mapCenter.x, 0, mapCenter.z), Vector3f.UNIT_Y);

        updateDiscoveredTiles();
        updatePOIMarkers();
    }

    private void updateDiscoveredTiles() {
        if (playerCharacter != null) {
            Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();

            // Calculate tile coordinates based on player position
            int tileSize = 100; // Size of each discoverable tile
            int tileX = (int) (playerPos.x / tileSize);
            int tileZ = (int) (playerPos.z / tileSize);

            // Mark current and surrounding tiles as discovered and cache their positions
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String tileId = (tileX + dx) + "," + (tileZ + dz);

                    // Add to discovered tiles (NEVER remove from this set)
                    if (!discoveredTiles.contains(tileId)) {
                        discoveredTiles.add(tileId);

                        // Cache the world position of this tile
                        Vector3f tileWorldPos = new Vector3f(
                                (tileX + dx) * tileSize,
                                0,
                                (tileZ + dz) * tileSize
                        );
                        tileWorldPositions.put(tileId, tileWorldPos);

                        // Capture texture snapshot for this tile if not already cached
                        captureTileTexture(tileId, tileWorldPos);

                        LOGGER.fine("Discovered and cached tile: " + tileId);
                    }
                }
            }

            // Update fog of war with persistent discovered tiles
            worldMap.updateFogOfWar(discoveredTiles);
        }
    }

    private void captureTileTexture(String tileId, Vector3f tileWorldPos) {
        if (tileTextureCache.containsKey(tileId)) {
            return; // Already cached
        }

        // Manage cache size to prevent memory issues
        if (tileTextureCache.size() >= MAX_CACHED_TILES) {
            // Remove oldest entries (simple FIFO approach)
            String firstKey = tileTextureCache.keySet().iterator().next();
            tileTextureCache.remove(firstKey);
            tileWorldPositions.remove(firstKey);
            LOGGER.fine("Removed cached tile due to memory limit: " + firstKey);
        }

        // Create a small texture snapshot for this tile area
        // This is a simplified approach - in practice you might render just this tile area
        try {
            int tileTextureSize = 128; // Small texture per tile
            Texture2D tileTexture = new Texture2D(tileTextureSize, tileTextureSize, Image.Format.RGBA8);
            tileTexture.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
            tileTexture.setMagFilter(Texture2D.MagFilter.Bilinear);

            // Store in cache
            tileTextureCache.put(tileId, tileTexture);

            LOGGER.fine("Cached texture for tile: " + tileId);
        } catch (Exception e) {
            LOGGER.warning("Failed to create texture cache for tile " + tileId + ": " + e.getMessage());
        }
    }

    // Add getter methods for the cache
    public Map<String, Texture2D> getTileTextureCache() {
        return new HashMap<>(tileTextureCache);
    }

    public Map<String, Vector3f> getTileWorldPositions() {
        return new HashMap<>(tileWorldPositions);
    }

    @Override
    public void clearDiscoveredTiles() {
        discoveredTiles.clear();
        tileTextureCache.clear();
        tileWorldPositions.clear();
        LOGGER.info("Cleared all discovered tiles and texture cache");
    }

    private void updatePOIMarkers() {
        if (compassState == null || compassState.getCompass() == null || playerCharacter == null) return;

        Collection<POIInterface> pois = compassState.getCompass().getPointsOfInterest();
        worldMap.updatePOIMarkers(pois, playerCharacter, mapCenter, 1f);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals(TOGGLE_MAP) && isPressed) {
            if (mapVisible) {
                hideMap();
            } else {
                showMap();
            }
            return;
        }

        if (name.equals(ESCAPE_MAP) && isPressed && mapVisible) {
            hideMap();
            return;
        }

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
                mapCam.setLocation(new Vector3f(mapCenter.x, 3500f, mapCenter.z));
                mapCam.lookAt(new Vector3f(mapCenter.x, 0, mapCenter.z), Vector3f.UNIT_Y);
            }

            LOGGER.info("World map opened.");
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
            discoveredTiles.add(tileId);

            // Also add to position cache if we can calculate it
            try {
                String[] coords = tileId.split(",");
                if (coords.length == 2) {
                    int tileX = Integer.parseInt(coords[0]);
                    int tileZ = Integer.parseInt(coords[1]);
                    int tileSize = 100;
                    Vector3f tileWorldPos = new Vector3f(tileX * tileSize, 0, tileZ * tileSize);
                    tileWorldPositions.put(tileId, tileWorldPos);
                    captureTileTexture(tileId, tileWorldPos);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to parse tile coordinates for " + tileId + ": " + e.getMessage());
            }
        }
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

        // Clean up rendering resources
        if (mapViewPort != null) {
            app.getRenderManager().removeMainView(mapViewPort);
        }

        if (mapVisible) {
            hideMap();
        }
    }

    @Override
    protected void onEnable() {
        // Map is shown/hidden based on key presses, not state enable/disable
    }

    @Override
    protected void onDisable() {
        if (mapVisible) {
            hideMap();
        }
    }
}