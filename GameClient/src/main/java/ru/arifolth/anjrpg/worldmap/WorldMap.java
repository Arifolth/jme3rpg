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

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;
import ru.arifolth.anjrpg.interfaces.worldmap.WorldMapInterface;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class WorldMap implements WorldMapInterface {

    private static final Logger LOGGER = Logger.getLogger(WorldMap.class.getName());

    private final SimpleApplication app;
    private final WorldMapState worldMapState;
    private final float tileMapSize;

    private Node mapNode;
    private Geometry mapFrame;
    private Geometry mapBackground;
    private Geometry fogOverlay;
    private Geometry playerMarker;

    private Map<String, Geometry> poiMarkers = new HashMap<>();
    private Map<String, Geometry> cachedTileGeometries = new HashMap<>();

    private Material mapMaterial;
    private Material fogMaterial;
    private Material playerMaterial;
    private Material cachedTileMaterial;
    private Material fallbackTileMaterial;

    private float screenWidth;
    private float screenHeight;

    public WorldMap(SimpleApplication app, WorldMapState worldMapState, float tileMapSize) {
        this.app = app;
        this.worldMapState = worldMapState;
        this.tileMapSize = tileMapSize;
        this.screenWidth = app.getCamera().getWidth();
        this.screenHeight = app.getCamera().getHeight();

        initializeMapComponents();
        LOGGER.info("WorldMap initialized with fallback textures");
    }

    private void initializeMapComponents() {
        mapNode = new Node("WorldMapUI");

        createMapFrame();
        createMapBackground();
        createFogOverlay();
        createPlayerMarker();
        createTileMaterials();
        positionMapUI();
    }

    private void createTileMaterials() {
        // Material for cached textures
        cachedTileMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        cachedTileMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Fallback material for tiles without cached textures
        fallbackTileMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fallbackTileMaterial.setColor("Color", new ColorRGBA(0.7f, 0.6f, 0.4f, 0.8f)); // Earth tone
        fallbackTileMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    }

    private void createMapFrame() {
        float frameBorder = Constants.FRAME_BORDER_WIDTH;
        Quad frameQuad = new Quad(screenWidth + 2*frameBorder, screenHeight + 2*frameBorder);
        mapFrame = new Geometry("MapFrame", frameQuad);

        Material frameMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        frameMat.setTexture("ColorMap", app.getAssetManager().loadTexture("Textures/Compass/A_21_9_rectangular_frame_.png"));
        mapFrame.setMaterial(frameMat);
        mapFrame.setLocalTranslation(-frameBorder, -frameBorder, -0.1f);
        mapNode.attachChild(mapFrame);
    }

    private void createMapBackground() {
        Quad mapQuad = new Quad(screenWidth, screenHeight);
        mapBackground = new Geometry("MapBackground", mapQuad);

        mapMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mapMaterial.setColor("Color", new ColorRGBA(0.92f, 0.87f, 0.78f, 1f));
        mapBackground.setMaterial(mapMaterial);
        mapNode.attachChild(mapBackground);
    }

    private void createFogOverlay() {
        Quad fogQuad = new Quad(screenWidth, screenHeight);
        fogOverlay = new Geometry("FogOverlay", fogQuad);

        fogMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fogMaterial.setColor("Color", new ColorRGBA(0.15f, 0.12f, 0.08f, 0.85f));
        fogMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        fogOverlay.setMaterial(fogMaterial);
        fogOverlay.setLocalTranslation(0, 0, 0.3f);
        mapNode.attachChild(fogOverlay);
    }

    private void createPlayerMarker() {
        float markerSize = Constants.UI_PADDING * 2f;
        Quad quad = new Quad(markerSize, markerSize);
        playerMarker = new Geometry("PlayerMarker", quad);

        playerMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        playerMaterial.setColor("Color", new ColorRGBA(0.9f, 0.2f, 0.1f, 1f));
        playerMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        playerMarker.setMaterial(playerMaterial);

        centerPlayerMarker();
        mapNode.attachChild(playerMarker);
    }

    private void centerPlayerMarker() {
        float markerSize = Constants.UI_PADDING * 2f;
        playerMarker.setLocalTranslation(
                screenWidth/2 - markerSize/2,
                screenHeight/2 - markerSize/2,
                0.5f
        );
    }

    private void positionMapUI() {
        mapNode.setLocalTranslation(0, 0, 50f);
    }

    @Override
    public void updateFogOfWar(Set<String> discoveredTiles) {
        // Fog is handled in updateTileGrid
    }

    public void updateTileGrid(Set<String> discoveredTiles, int gridCenterX, int gridCenterZ) {
        if (discoveredTiles == null) return;

        // Update fog transparency
        if (!discoveredTiles.isEmpty()) {
            float discoveryRatio = Math.min(1.0f, discoveredTiles.size() / 50.0f);
            float fogAlpha = 0.85f * (1.0f - discoveryRatio * 0.7f);
            fogMaterial.setColor("Color", new ColorRGBA(0.15f, 0.12f, 0.08f, fogAlpha));
        }

        clearOldTiles(gridCenterX, gridCenterZ);
        createTileGrid(discoveredTiles, gridCenterX, gridCenterZ);
    }

    private void clearOldTiles(int gridCenterX, int gridCenterZ) {
        cachedTileGeometries.entrySet().removeIf(entry -> {
            String tileId = entry.getKey();
            String[] coords = tileId.split(",");
            if (coords.length != 2) return true;

            try {
                int tileX = Integer.parseInt(coords[0]);
                int tileZ = Integer.parseInt(coords[1]);
                return Math.abs(tileX - gridCenterX) > 2 || Math.abs(tileZ - gridCenterZ) > 2;
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }

    private void createTileGrid(Set<String> discoveredTiles, int gridCenterX, int gridCenterZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int tileX = gridCenterX + dx;
                int tileZ = gridCenterZ + dz;
                String tileId = tileX + "," + tileZ;

                if (discoveredTiles.contains(tileId)) {
                    if (!cachedTileGeometries.containsKey(tileId)) {
                        createCachedTileGeometry(tileId, tileX, tileZ);
                    } else {
                        updateTilePosition(tileId, tileX, tileZ);
                    }
                }
            }
        }
    }

    private void createCachedTileGeometry(String tileId, int tileX, int tileZ) {
        try {
            Quad tileQuad = new Quad(tileMapSize, tileMapSize);
            Geometry tileGeometry = new Geometry("CachedTile_" + tileId, tileQuad);

            // Try to use cached texture, fallback to colored quad
            Texture2D cachedTexture = worldMapState.getTileTextureCache().get(tileId);
            Material tileMat;

            if (cachedTexture != null) {
                tileMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                tileMat.setTexture("ColorMap", cachedTexture);
                LOGGER.fine("Using cached texture for tile: " + tileId);
            } else {
                tileMat = fallbackTileMaterial;
                LOGGER.fine("Using fallback material for tile: " + tileId);
            }

            tileMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            tileGeometry.setMaterial(tileMat);

            positionTileInGrid(tileGeometry, tileX, tileZ);

            mapNode.attachChild(tileGeometry);
            cachedTileGeometries.put(tileId, tileGeometry);

            LOGGER.fine("Created tile geometry: " + tileId);

        } catch (Exception e) {
            LOGGER.warning("Failed to create tile geometry for " + tileId + ": " + e.getMessage());
        }
    }

    private void updateTilePosition(String tileId, int tileX, int tileZ) {
        Geometry tileGeometry = cachedTileGeometries.get(tileId);
        if (tileGeometry != null) {
            positionTileInGrid(tileGeometry, tileX, tileZ);

            // Update texture if it became available
            Texture2D cachedTexture = worldMapState.getTileTextureCache().get(tileId);
            if (cachedTexture != null) {
                Material currentMat = tileGeometry.getMaterial();
                if (currentMat != null && currentMat == fallbackTileMaterial) {
                    // Upgrade to texture material
                    Material textureMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                    textureMat.setTexture("ColorMap", cachedTexture);
                    textureMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    tileGeometry.setMaterial(textureMat);
                    LOGGER.fine("Upgraded tile to texture: " + tileId);
                }
            }
        }
    }

    private void positionTileInGrid(Geometry tileGeometry, int tileX, int tileZ) {
        int gridCenterX = worldMapState.getCurrentGridCenterX();
        int gridCenterZ = worldMapState.getCurrentGridCenterZ();

        int relX = tileX - gridCenterX;
        int relZ = tileZ - gridCenterZ;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float screenX = centerX + (relX * tileMapSize);
        float screenY = centerY + (relZ * tileMapSize);

        tileGeometry.setLocalTranslation(
                screenX - tileMapSize/2,
                screenY - tileMapSize/2,
                0.1f
        );
    }

    public void updatePOIMarkers(Collection<POIInterface> pois, CharacterInterface playerCharacter,
                                 int gridCenterX, int gridCenterZ) {
        if (playerCharacter == null) return;

        for (Geometry marker : poiMarkers.values()) {
            marker.removeFromParent();
        }
        poiMarkers.clear();

        for (POIInterface poi : pois) {
            createPOIMarker(poi, gridCenterX, gridCenterZ);
        }

        centerPlayerMarker();
    }

    private void createPOIMarker(POIInterface poi, int gridCenterX, int gridCenterZ) {
        float markerSize = Constants.UI_PADDING * 1.5f;
        Quad quad = new Quad(markerSize, markerSize);
        Geometry poiGeo = new Geometry("POI_" + poi.getId(), quad);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA color = getPOIColor(poi.getType());
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        poiGeo.setMaterial(mat);

        Vector3f poiWorldPos = poi.getPosition();
        int poiTileX = worldToTileX(poiWorldPos.x);
        int poiTileZ = worldToTileZ(poiWorldPos.z);

        int relX = poiTileX - gridCenterX;
        int relZ = poiTileZ - gridCenterZ;

        if (Math.abs(relX) <= 1 && Math.abs(relZ) <= 1) {
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;

            float screenX = centerX + (relX * tileMapSize);
            float screenY = centerY + (relZ * tileMapSize);

            poiGeo.setLocalTranslation(
                    screenX - markerSize/2f,
                    screenY - markerSize/2f,
                    0.4f
            );
            mapNode.attachChild(poiGeo);
            poiMarkers.put(poi.getId(), poiGeo);
        }
    }

    private int worldToTileX(float worldX) {
        return (int) Math.floor(worldX / worldMapState.getTileWorldSize());
    }

    private int worldToTileZ(float worldZ) {
        return (int) Math.floor(worldZ / worldMapState.getTileWorldSize());
    }

    private ColorRGBA getPOIColor(POIType type) {
        switch(type) {
            case NPC: return new ColorRGBA(1f, 0.8f, 0.2f, 1f);
            case LANDMARK: return new ColorRGBA(0.2f, 0.8f, 1f, 1f);
            default: return new ColorRGBA(0.8f, 0.8f, 0.8f, 1f);
        }
    }

    @Override
    public void updatePOIMarkers(Collection<POIInterface> pois, CharacterInterface playerCharacter,
                                 Vector3f mapCenter, float mapScale) {
        updatePOIMarkers(pois, playerCharacter,
                worldMapState.getCurrentGridCenterX(),
                worldMapState.getCurrentGridCenterZ());
    }

    public int getCachedTileCount() {
        return cachedTileGeometries.size();
    }

    @Override
    public Node getMapNode() {
        return mapNode;
    }

    @Override
    public boolean isVisible() {
        return mapNode.getParent() != null;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !isVisible()) {
            mapNode.setCullHint(Spatial.CullHint.Inherit);
        } else if (!visible && isVisible()) {
            mapNode.setCullHint(Spatial.CullHint.Always);
        }
    }
}