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
import com.jme3.math.Quaternion;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Container;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.worldmap.ITextureCache;

import java.util.Optional;
import java.util.logging.Logger;

public class WorldMapState extends BaseAppState implements ActionListener {
    private static final Logger LOGGER = Logger.getLogger(WorldMapState.class.getName());

    private static final String TOGGLE_MAP = "TOGGLE_MAP";
    private static final String ESCAPE_MAP = "ESCAPE_MAP";

    // World configuration (adjust these based on your terrain settings)
    private static final float TILE_WORLD_SIZE = 512.0f;  // Each base tile is 512x512 world units
    private static final float SUBTILE_WORLD_SIZE = TILE_WORLD_SIZE / 2.0f;  // 256x256 per sub-tile

    private final GameLogicCoreInterface gameLogicCore;
    private Container worldMapContainer;
    private InputManager inputManager;
    private Geometry playerMarker;
    private boolean mapVisible = false;

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

        // Create world map container
        worldMapContainer = new Container();
        worldMapContainer.setLocalTranslation(0, 0, 1000); // High Z to be on top
        worldMapContainer.setBackground(null);

        createPlayerMarker();
        LOGGER.info("WorldMapState initialized");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals(TOGGLE_MAP)) {
            toggleMap();
        } else if (name.equals(ESCAPE_MAP) && mapVisible) {
            hideMap();
        }
    }

    private void toggleMap() {
        if (mapVisible) {
            hideMap();
        } else {
            showMap();
        }
    }

    private void showMap() {
        if (!mapVisible) {
            mapVisible = true;
            ((SimpleApplication) getApplication()).getGuiNode().attachChild(worldMapContainer);
            if (playerMarker != null) {
                playerMarker.setCullHint(Spatial.CullHint.Never);
            }
            LOGGER.info("World map shown");
        }
    }

    private void hideMap() {
        if (mapVisible) {
            mapVisible = false;
            worldMapContainer.removeFromParent();
            if (playerMarker != null) {
                playerMarker.setCullHint(Spatial.CullHint.Always);
            }
            LOGGER.info("World map hidden");
        }
    }

    /**
     * Converts world position to sub-tile ID (tile_X_Z_subX_subZ)
     */
    public String getSubTileIdForWorldPosition(float worldX, float worldZ) {
        // Calculate base tile coordinates
        int baseTileX = (int) Math.floor(worldX / TILE_WORLD_SIZE);
        int baseTileZ = (int) Math.floor(worldZ / TILE_WORLD_SIZE);

        // Calculate local position within the base tile
        float localX = worldX - (baseTileX * TILE_WORLD_SIZE);
        float localZ = worldZ - (baseTileZ * TILE_WORLD_SIZE);

        // Determine which sub-tile (0 or 1) within the 2x2 grid
        int subX = (localX < SUBTILE_WORLD_SIZE) ? 0 : 1;
        int subZ = (localZ < SUBTILE_WORLD_SIZE) ? 0 : 1;

        return String.format("tile_%d_%d_%d_%d", baseTileX, baseTileZ, subX, subZ);
    }

    /**
     * Renders the world map with cached textures
     */
    public void renderWorldMap() {
        if (!mapVisible) return;

        Camera cam = getApplication().getCamera();
        Vector3f playerPos = cam.getLocation();

        // Get the sub-tile ID for the player's current position
        String currentSubTileId = getSubTileIdForWorldPosition(playerPos.x, playerPos.z);
        LOGGER.info("Player in sub-tile: " + currentSubTileId);

        // Try to get the texture for the current sub-tile
        ITextureCache textureCache = gameLogicCore.getTextureCache();
        Optional<Texture> textureOpt = textureCache.getTexture(currentSubTileId);

        if (textureOpt.isPresent()) {
            LOGGER.info("Found cached texture for: " + currentSubTileId);
            // TODO: Render the texture on the world map
            // You would create a Geometry with this texture and add it to worldMapContainer
        } else {
            LOGGER.info("No cached texture for: " + currentSubTileId);
        }

        // Update player marker position
        updatePlayerMarker(playerPos);
    }

    private void createPlayerMarker() {
        Mesh triangleMesh = new Mesh();
        float[] vertices = new float[] {
                0, 10, 0,
                -8, -10, 0,
                8, -10, 0
        };
        short[] indices = new short[] {0, 1, 2};

        triangleMesh.setBuffer(VertexBuffer.Type.Position, 3, vertices);
        triangleMesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        triangleMesh.updateBound();

        playerMarker = new Geometry("PlayerMarker", triangleMesh);
        Material markerMaterial = new Material(getApplication().getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        markerMaterial.setColor("Color", ColorRGBA.Red);
        playerMarker.setMaterial(markerMaterial);

        worldMapContainer.attachChild(playerMarker);
    }

    private void updatePlayerMarker(Vector3f playerPos) {
        if (playerMarker != null && mapVisible) {
            // Convert world position to map coordinates
            // Adjust scale as needed for your map display
            float mapScale = 0.1f;
            Vector3f mapPos = new Vector3f(
                    playerPos.x * mapScale,
                    playerPos.z * mapScale,
                    0
            );
            playerMarker.setLocalTranslation(mapPos);

            // Rotate marker to face player direction
            Camera cam = getApplication().getCamera();
            Vector3f playerDir = cam.getDirection();
            float angle = (float) Math.atan2(playerDir.x, playerDir.z);
            playerMarker.setLocalRotation(new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Z));
        }
    }

    @Override
    public void update(float tpf) {
        if (mapVisible) {
            renderWorldMap();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (worldMapContainer != null) {
            worldMapContainer.removeFromParent();
        }
        inputManager.removeListener(this);
        inputManager.deleteMapping(TOGGLE_MAP);
        inputManager.deleteMapping(ESCAPE_MAP);
    }

    @Override
    protected void onEnable() {
        // Nothing additional needed
    }

    @Override
    protected void onDisable() {
        hideMap();
    }
}