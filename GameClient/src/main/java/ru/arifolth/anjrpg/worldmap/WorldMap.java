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
import ru.arifolth.anjrpg.interfaces.worldmap.WorldMapInterface;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class WorldMap implements WorldMapInterface {

    private static final Logger LOGGER = Logger.getLogger(WorldMap.class.getName());

    private final SimpleApplication app;
    private Node mapNode;
    private Geometry mapFrame;
    private Geometry mapBackground;
    private Geometry fogOverlay;
    private Geometry playerMarker;

    private Map<String, Geometry> poiMarkers = new HashMap<>();
    private Material mapMaterial;
    private Material fogMaterial;
    private Material playerMaterial;

    private float screenWidth;
    private float screenHeight;
    private Texture2D worldRenderTexture;

    // Add cached tile markers and material
    private final Map<String, Geometry> cachedTileMarkers = new HashMap<>();
    private Material cachedTileMaterial;

    public WorldMap(SimpleApplication app, Texture2D renderedTexture) {
        this.app = app;
        this.screenWidth = app.getCamera().getWidth();
        this.screenHeight = app.getCamera().getHeight();
        this.worldRenderTexture = renderedTexture;

        initializeMapComponents();
    }

    private void initializeMapComponents() {
        mapNode = new Node("WorldMapUI");

        // Create map frame (border)
        createMapFrame();

        // Create map background
        createMapBackground();

        // Create fog of war overlay
        createFogOverlay();

        // Create cached tile material
        createCachedTileMaterial();

        // Create player marker
        createPlayerMarker();

        // Position the entire map UI
        positionMapUI();
    }

    private void createCachedTileMaterial() {
        cachedTileMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        cachedTileMaterial.setColor("Color", new ColorRGBA(0.9f, 0.85f, 0.7f, 0.3f)); // Subtle discovered area tint
        cachedTileMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
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
        mapMaterial.setTexture("ColorMap", worldRenderTexture);
        mapMaterial.setColor("Color", new ColorRGBA(1f, 0.95f, 0.85f, 1f)); // Warm Renaissance tint

        mapBackground.setMaterial(mapMaterial);
        mapNode.attachChild(mapBackground);
    }

    private void createFogOverlay() {
        Quad fogQuad = new Quad(screenWidth, screenHeight);
        fogOverlay = new Geometry("FogOverlay", fogQuad);

        fogMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fogMaterial.setColor("Color", new ColorRGBA(0.2f, 0.15f, 0.1f, 0.6f));
        fogMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        fogOverlay.setMaterial(fogMaterial);
        fogOverlay.setLocalTranslation(0, 0, 0.2f);

        mapNode.attachChild(fogOverlay);
    }

    private void createPlayerMarker() {
        float markerSize = Constants.UI_PADDING;

        Quad quad = new Quad(markerSize, markerSize);
        playerMarker = new Geometry("PlayerMarker", quad);

        playerMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        playerMaterial.setColor("Color", new ColorRGBA(0.9f, 0.2f, 0.1f, 1f));
        playerMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        playerMarker.setMaterial(playerMaterial);

        // Initial position middle of map, corrected as player moves
        playerMarker.setLocalTranslation(screenWidth/2 - markerSize/2, screenHeight/2 - markerSize/2, 0.4f);

        mapNode.attachChild(playerMarker);
    }

    private void positionMapUI() {
        // Position map at bottom-left of GUI coordinates (0,0) to cover entire screen
        mapNode.setLocalTranslation(0, 0, 50f);
    }

    @Override
    public void updateFogOfWar(Set<String> discoveredTiles) {
        if (discoveredTiles == null) return;

        // Clear existing cached tile markers
        for (Geometry marker : cachedTileMarkers.values()) {
            marker.removeFromParent();
        }
        cachedTileMarkers.clear();

        // Create visual markers for all discovered (cached) tiles
        for (String tileId : discoveredTiles) {
            createCachedTileMarker(tileId);
        }

        // Update main fog opacity based on discovery ratio
        if (discoveredTiles.size() > 0) {
            float discoveryRatio = Math.min(1.0f, discoveredTiles.size() / 100.0f);
            ColorRGBA fogColor = new ColorRGBA(0.2f, 0.15f, 0.1f, 0.85f * (1.0f - discoveryRatio * 0.5f));

            if (fogMaterial != null) {
                try {
                    fogMaterial.setColor("FogColor", fogColor);
                } catch (Exception e) {
                    // Fallback for basic material
                    fogMaterial.setColor("Color", fogColor);
                }
            }
        }
    }

    private void createCachedTileMarker(String tileId) {
        if (cachedTileMarkers.containsKey(tileId)) {
            return; // Already exists
        }

        try {
            // Parse tile coordinates from tileId (format: "x,z")
            String[] coords = tileId.split(",");
            if (coords.length != 2) return;

            int tileX = Integer.parseInt(coords[0]);
            int tileZ = Integer.parseInt(coords[1]);

            // Calculate tile size and position on map
            float tileSize = 50f; // Visual size on map
            float tileWorldSize = 100f; // World size of actual tile

            // Convert world tile position to map coordinates
            Vector3f tileWorldPos = new Vector3f(tileX * tileWorldSize, 0, tileZ * tileWorldSize);
            Vector3f mapPos = worldToMapCoordinates(tileWorldPos, new Vector3f(0, 0, 0), 1.0f); // Center reference

            // Create quad for cached tile area
            Quad tileQuad = new Quad(tileSize, tileSize);
            Geometry tileMarker = new Geometry("CachedTile_" + tileId, tileQuad);
            tileMarker.setMaterial(cachedTileMaterial);

            // Position on map
            tileMarker.setLocalTranslation(mapPos.x - tileSize/2, mapPos.z - tileSize/2, 0.15f);

            mapNode.attachChild(tileMarker);
            cachedTileMarkers.put(tileId, tileMarker);

        } catch (Exception e) {
            LOGGER.warning("Failed to create cached tile marker for " + tileId + ": " + e.getMessage());
        }
    }

    @Override
    public void updatePOIMarkers(Collection<POIInterface> pois, CharacterInterface playerCharacter,
                                 Vector3f mapCenter, float mapScale) {
        if (playerCharacter == null) return;

        Vector3f playerPos = playerCharacter.getNode().getWorldTranslation();

        // Clear existing POI markers
        for (Geometry marker : poiMarkers.values()) {
            marker.removeFromParent();
        }
        poiMarkers.clear();

        // Create new POI markers
        for (POIInterface poi : pois) {
            createPOIMarker(poi, mapCenter != null ? mapCenter : playerPos, mapScale);
        }

        // Update player marker position
        updatePlayerMarkerPosition(playerPos, mapCenter != null ? mapCenter : playerPos, mapScale);
    }

    private void createPOIMarker(POIInterface poi, Vector3f mapCenter, float mapScale) {
        float markerSize = Constants.UI_PADDING * 0.8f;

        Quad quad = new Quad(markerSize, markerSize);
        Geometry poiGeo = new Geometry("POI_" + poi.getId(), quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        ColorRGBA col;
        switch(poi.getType()) {
            case NPC: col = new ColorRGBA(1f, 0.8f, 0.2f, 1f); break;
            case LANDMARK: col = new ColorRGBA(0.2f, 0.8f, 1f, 1f); break;
            default: col = new ColorRGBA(0.8f, 0.8f, 0.8f, 1f); break;
        }

        mat.setColor("Color", col);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        poiGeo.setMaterial(mat);

        Vector3f pos = worldToMapCoordinates(poi.getPosition(), mapCenter, mapScale);

        if(pos.x >= 0 && pos.x <= screenWidth && pos.z >= 0 && pos.z <= screenHeight) {
            poiGeo.setLocalTranslation(pos.x - markerSize/2f, pos.z - markerSize/2f, 0.3f);
            mapNode.attachChild(poiGeo);
            poiMarkers.put(poi.getId(), poiGeo);
        }
    }

    // Player marker position update
    private void updatePlayerMarkerPosition(Vector3f playerPos, Vector3f mapCenter, float mapScale) {
        float markerSize = Constants.UI_PADDING;
        Vector3f pos = worldToMapCoordinates(playerPos, mapCenter, mapScale);
        playerMarker.setLocalTranslation(pos.x - markerSize/2f, pos.z - markerSize/2f, 0.4f);
    }

    // Converts world coords to map coordinates for fullscreen map quad
    private Vector3f worldToMapCoordinates(Vector3f worldPos, Vector3f mapCenter, float mapScale) {
        float worldViewRange = 2000f * mapScale; // match camera frustum

        Vector3f relative = worldPos.subtract(mapCenter);

        float mapX = (screenWidth / 2f) + (relative.x / worldViewRange) * (screenWidth / 2f);
        float mapZ = (screenHeight / 2f) - (relative.z / worldViewRange) * (screenHeight / 2f); // Inverted Z for correct orientation

        return new Vector3f(mapX, mapZ, 0);
    }

    // Add method to get cached tile information
    public int getCachedTileCount() {
        return cachedTileMarkers.size();
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
        if(visible && !isVisible()) {
            mapNode.setCullHint(Spatial.CullHint.Inherit);
        } else if(!visible && isVisible()) {
            mapNode.setCullHint(Spatial.CullHint.Always);
        }
    }
}