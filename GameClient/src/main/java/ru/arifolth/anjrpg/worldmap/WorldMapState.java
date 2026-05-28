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
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.graphics.ViewDistanceSettingsInterface;
import ru.arifolth.anjrpg.menu.SettingsUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WorldMapState extends BaseAppState implements ActionListener, AnalogListener {
    private static final Logger LOGGER = Logger.getLogger(WorldMapState.class.getName());

    // ========== CALIBRATION – adjust so triangle sits exactly on player ==========
    // These values shift the entire world-to-screen mapping (in subquad units)
    private static final float OFFSET_X_SUBQUADS = 2.0f;   // positive = right
    private static final float OFFSET_Y_SUBQUADS = 2.0f;   // positive = up
    // ============================================================================

    private static final String TOGGLE_MAP = "TOGGLE_MAP";
    private static final String ESCAPE_MAP = "ESCAPE_MAP";
    private static final String PAN_UP = "PAN_UP";
    private static final String PAN_DOWN = "PAN_DOWN";
    private static final String PAN_LEFT = "PAN_LEFT";
    private static final String PAN_RIGHT = "PAN_RIGHT";

    private final GameLogicCoreInterface gameLogicCore;
    private InputManager inputManager;

    private Node worldMapNode;
    private Node mapTilesNode;
    private Geometry mapBorder;

    private float mapWidth;
    private float mapHeight;
    private float screenWidth;
    private float screenHeight;
    private float mapMargin;
    private boolean isMapVisible = false;

    private float panX = 0f;
    private float panY = 0f;
    private final float PAN_SPEED = 4.0f;
    private float subquadWidth;
    private float subquadHeight;

    private ViewDistanceSettingsInterface viewDistanceSettings;
    private float quadSizeWorld;
    private float subquadSizeWorld;

    private Node playerMarkerNode;
    private Geometry playerMarker;
    private float markerBaseWidth = 24f;
    private float markerHeight = 48f;

    private Node poiMarkersNode;
    private Material poiMaterial;
    private final Map<String, Geometry> poiGeometries = new HashMap<>();

    public WorldMapState(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
        this.viewDistanceSettings = SettingsUtils.getViewDistanceSettings(gameLogicCore.getApp().getContext().getSettings());

        if (viewDistanceSettings != null) {
            quadSizeWorld = viewDistanceSettings.getTerrainSize() - 1;
            subquadSizeWorld = quadSizeWorld / 2f;
            LOGGER.info("WorldMapState: quadSizeWorld=" + quadSizeWorld + ", subquadSizeWorld=" + subquadSizeWorld);
        } else {
            quadSizeWorld = 512f;
            subquadSizeWorld = 256f;
            LOGGER.warning("ViewDistanceSettings null, using defaults");
        }
    }

    @Override
    protected void initialize(Application app) {
        this.inputManager = app.getInputManager();

        inputManager.addMapping(TOGGLE_MAP, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping(ESCAPE_MAP, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping(PAN_UP, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(PAN_DOWN, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(PAN_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(PAN_RIGHT, new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(this, TOGGLE_MAP, ESCAPE_MAP);
        inputManager.addListener(this, PAN_UP, PAN_DOWN, PAN_LEFT, PAN_RIGHT);

        initMapDimensions();

        worldMapNode = new Node("WorldMapNode");
        worldMapNode.setLocalTranslation(0, 0, 5);

        createBorder();

        mapTilesNode = new Node("MapTilesNode");
        mapTilesNode.setLocalTranslation(mapMargin, mapMargin, 1);
        worldMapNode.attachChild(mapTilesNode);

        playerMarkerNode = new Node("PlayerMarkerNode");
        poiMarkersNode = new Node("POIMarkersNode");
        worldMapNode.attachChild(playerMarkerNode);
        worldMapNode.attachChild(poiMarkersNode);

        createPlayerMarker();
        createPoiMaterial();

        worldMapNode.setCullHint(Node.CullHint.Always);
    }

    private void initMapDimensions() {
        screenWidth = getApplication().getCamera().getWidth();
        screenHeight = getApplication().getCamera().getHeight();

        mapMargin = screenHeight * 0.011f;
        mapWidth = screenWidth - (mapMargin * 2);
        mapHeight = screenHeight - (mapMargin * 2);

        subquadWidth = mapWidth / 4f;
        subquadHeight = mapHeight / 4f;
    }

    private void createBorder() {
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

    private void createPlayerMarker() {
        Mesh triangleMesh = new Mesh();

        float halfBase = markerBaseWidth / 2f;
        float halfHeight = markerHeight / 2f;
        float tipY = halfHeight;
        float baseY = -halfHeight;

        Vector3f[] vertices = {
                new Vector3f(-halfBase, baseY, 0),
                new Vector3f( halfBase, baseY, 0),
                new Vector3f(0, tipY, 0)
        };
        int[] indices = {0, 1, 2};
        Vector3f[] normals = {
                new Vector3f(0, 0, 1),
                new Vector3f(0, 0, 1),
                new Vector3f(0, 0, 1)
        };
        Vector2f[] texCoords = {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(0.5f, 1)
        };

        triangleMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        triangleMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        triangleMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        triangleMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
        triangleMesh.updateBound();
        triangleMesh.setStatic();

        playerMarker = new Geometry("PlayerMarker", triangleMesh);
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.85f, 0.65f, 0.2f, 1f)); // golden
        playerMarker.setMaterial(mat);

        playerMarkerNode.attachChild(playerMarker);
        playerMarkerNode.setLocalTranslation(0, 0, 2);
    }

    private void createPoiMaterial() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 100, 255, 200));
        g.fillOval(0, 0, size, size);
        g.dispose();

        ByteBuffer buffer = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = img.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buffer.flip();
        Image jmeImage = new Image(Image.Format.RGBA8, size, size, buffer);
        Texture2D tex = new Texture2D(jmeImage);
        tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        tex.setMagFilter(Texture.MagFilter.Bilinear);

        poiMaterial = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        poiMaterial.setTexture("ColorMap", tex);
        poiMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    }

    public void addPoiMarker(String id, Vector3f worldPos) {
        if (poiGeometries.containsKey(id)) return;
        Geometry marker = new Geometry("POI_" + id, new Quad(24, 24));
        marker.setMaterial(poiMaterial);
        marker.setUserData("worldPos", worldPos.clone());
        poiMarkersNode.attachChild(marker);
        poiGeometries.put(id, marker);
    }

    public void removePoiMarker(String id) {
        Geometry marker = poiGeometries.remove(id);
        if (marker != null) marker.removeFromParent();
    }

    public void clearPoiMarkers() {
        for (Geometry geom : poiGeometries.values()) geom.removeFromParent();
        poiGeometries.clear();
    }

    // -------------------------------------------------------------------------
    // World → screen with calibration offsets
    // -------------------------------------------------------------------------
    private Vector2f worldToScreen(Vector3f worldPos) {
        float worldX = worldPos.x;
        float worldZ = worldPos.z;

        int quadX = (int) Math.floor(worldX / quadSizeWorld);
        int quadZ = (int) Math.floor(worldZ / quadSizeWorld);
        float localX = worldX - quadX * quadSizeWorld;
        float localZ = worldZ - quadZ * quadSizeWorld;
        float subXf = localX / subquadSizeWorld;
        float subZf = localZ / subquadSizeWorld;

        float absoluteSubX = quadX * 2 + subXf + OFFSET_X_SUBQUADS;
        float absoluteSubZ = quadZ * 2 + subZf + OFFSET_Y_SUBQUADS;

        float screenX = (absoluteSubX - panX) * subquadWidth;
        float screenY = (absoluteSubZ - panY) * subquadHeight;

        return new Vector2f(screenX, screenY);
    }

    private float getAngleFromNorth(Vector3f forward) {
        return FastMath.atan2(forward.x, -forward.z);
    }

    private void centerMapOnPlayer() {
        Vector3f playerPos = gameLogicCore.getPlayerCharacter().getNode().getWorldTranslation();
        Vector2f playerScreen = worldToScreen(playerPos);
        // Desired: player at center of mapTilesNode
        float targetX = mapWidth / 2f;
        float targetY = mapHeight / 2f;
        float deltaX = targetX - playerScreen.x;
        float deltaY = targetY - playerScreen.y;
        panX += deltaX / subquadWidth;
        panY += deltaY / subquadHeight;

        LOGGER.info(String.format("Centered map: player screen (%.1f, %.1f), pan (%.2f, %.2f)",
                playerScreen.x, playerScreen.y, panX, panY));
    }

    private void updateMarkers() {
        if (!isMapVisible) return;

        Vector3f playerPos = gameLogicCore.getPlayerCharacter().getNode().getWorldTranslation();
        Vector2f screenPos = worldToScreen(playerPos);

        // Debug output every few seconds
        if (System.currentTimeMillis() % 3000 < 50) {
            LOGGER.info(String.format("Player world (%.1f, %.1f) -> screen (%.1f, %.1f) | pan (%.2f, %.2f)",
                    playerPos.x, playerPos.z, screenPos.x, screenPos.y, panX, panY));
        }

        float worldMapX = mapMargin + screenPos.x;
        float worldMapY = mapMargin + screenPos.y;
        playerMarkerNode.setLocalTranslation(worldMapX, worldMapY, 2);

        Vector3f forward = gameLogicCore.getPlayerCharacter().getCharacterControl().getViewDirection();
        float angle = getAngleFromNorth(forward);
        playerMarkerNode.setLocalRotation(com.jme3.math.Quaternion.IDENTITY);
        playerMarkerNode.rotate(0, 0, angle);

        for (Geometry geom : poiGeometries.values()) {
            Vector3f worldPos = geom.getUserData("worldPos");
            if (worldPos != null) {
                Vector2f poiScreen = worldToScreen(worldPos);
                float poiX = mapMargin + poiScreen.x - 12;
                float poiY = mapMargin + poiScreen.y - 12;
                geom.setLocalTranslation(poiX, poiY, 1);
            }
        }
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

        for (int row = -1; row <= 4; row++) {
            for (int col = -1; col <= 4; col++) {
                int worldSubX = startSubX + col;
                int worldSubY = startSubY + row;

                int quadX = worldSubX >> 1;
                int quadY = worldSubY >> 1;
                int subX = worldSubX & 1;
                int subY = worldSubY & 1;

                String tileId = String.format("tile_%d_%d_sub_%d_%d", quadX, quadY, subX, subY);

                Quad quadMesh = new Quad(subquadWidth, subquadHeight);
                Geometry geom = new Geometry(tileId, quadMesh);
                Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

                if (cache.containsTexture(tileId)) {
                    Texture texture = cache.getTexture(tileId).get();
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
    public void update(float tpf) {
        if (isMapVisible) {
            renderVisibleTiles();
            updateMarkers();
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isMapVisible) return;
        if (name.equals(PAN_UP)) panY += PAN_SPEED * tpf;
        else if (name.equals(PAN_DOWN)) panY -= PAN_SPEED * tpf;
        else if (name.equals(PAN_RIGHT)) panX += PAN_SPEED * tpf;
        else if (name.equals(PAN_LEFT)) panX -= PAN_SPEED * tpf;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            if (name.equals(TOGGLE_MAP)) toggleMap();
            else if (name.equals(ESCAPE_MAP) && isMapVisible) hideMap();
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
        centerMapOnPlayer();
        worldMapNode.setCullHint(Node.CullHint.Never);
        isMapVisible = true;
        renderVisibleTiles();
    }

    public void hideMap() {
        worldMapNode.setCullHint(Node.CullHint.Always);
        isMapVisible = false;
    }

    @Override
    protected void cleanup(Application app) {
        if (worldMapNode != null && worldMapNode.getParent() != null) {
            worldMapNode.removeFromParent();
        }
        inputManager.removeListener(this);
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() { hideMap(); }
}