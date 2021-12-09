package ru.arifolth.game;

import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.scene.Node;
import com.stomrage.grassarea.GrassArea;
import com.stomrage.grassarea.GrassAreaControl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TerrainManager implements TerrainManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(TerrainManager.class.getName());

    private GrassArea grassArea;
    private TerrainQuad terrain;
    private Material matTerrain;
    private RigidBodyControl landscape;

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private RolePlayingGameInterface app;

    public TerrainManager(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;

        initialize();
    }

    private void initialize() {
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Grass/alpha1.png"));
        matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Grass/alpha2.png"));
        TextureKey hmKey = new TextureKey("Textures/Grass/mountains512.png", false);
        Texture heightMapImage = assetManager.loadTexture(hmKey);
        Texture dirt = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", dirt);
        matTerrain.setFloat("DiffuseMap_0_scale", 64);
        Texture darkRock = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        darkRock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", darkRock);
        matTerrain.setFloat("DiffuseMap_1_scale", 64);
        Texture pinkRock = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        pinkRock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_2", pinkRock);
        matTerrain.setFloat("DiffuseMap_2_scale", 64);
        Texture riverRock = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        riverRock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_3", riverRock);
        matTerrain.setFloat("DiffuseMap_3_scale", 64);
        Texture grass = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_4", grass);
        matTerrain.setFloat("DiffuseMap_4_scale", 64);
        Texture brick = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        brick.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_5", brick);
        matTerrain.setFloat("DiffuseMap_5_scale", 64);
        Texture road = assetManager.loadTexture("Textures/Grass/Grass_1.jpg");
        road.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_6", road);
        matTerrain.setFloat("DiffuseMap_6_scale", 64);


        /** 2. Create the height map */

        AbstractHeightMap heightmap = null;
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();
        heightmap.smooth(0.9f, 1);

        /** 3. We have prepared material and heightmap.
         * Now we createCharacter the actual terrain:
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */

        int patchSize = 65;
        terrain = new TerrainQuad(
                "my terrain",
                patchSize,
                513,
                heightmap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(matTerrain);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, app.getCamera());
        control.setLodCalculator(new DistanceLodCalculator(65, 2.7f)); // patch size, and a multiplier

        terrain.addControl(control);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) terrain);
        landscape = new RigidBodyControl(sceneShape, 0);
        terrain.addControl(landscape);

//        TODO:move terrain UNDER the player start location
        //terrain.setLocalTranslation(this.app.getGameLogicCore().getPlayerCharacter().getCharacterModel().getLocalTranslation());
//        terrain.setLocalTranslation(Constants.PLAYER_START_LOCATION.getX(), -50, Constants.PLAYER_START_LOCATION.getZ());
//        terrain.setLocalScale(1f, 1f, 1f);
        //terrain postion
        terrain.setLocalTranslation(0, -200, 0);
        terrain.setLocalScale(1f, 1f, 1f);

        // We attach the scene and the playerControl to the rootNode and the physics space,
        // to make them appear in the game world.
        bulletAppState.getPhysicsSpace().add(landscape);
    }

    public void generateGrass() {
        try {
            grassArea = new GrassArea(terrain, 8, assetManager, 75);
            grassArea.setColorTexture(assetManager.loadTexture("Textures/Grass/tile_1.png"));
            grassArea.setDissolveTexture(assetManager.loadTexture("Textures/Grass/noise.png"));
            grassArea.addDensityMap(assetManager.loadTexture("Textures/Grass/noise.png"));
            grassArea.addDensityMap(assetManager.loadTexture("Textures/Grass/noise_2.png"));
            grassArea.addLayer(0f, 0.5f, 0.75f, GrassArea.ColorChannel.RED_CHANNEL, GrassArea.DensityMap.DENSITY_MAP_1, 2f, 3f);
            grassArea.addLayer(0.5f, 0.5f, 0.75f, GrassArea.ColorChannel.BLUE_CHANNEL, GrassArea.DensityMap.DENSITY_MAP_2, 2f, 3f);
            grassArea.generate();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        GrassAreaControl grassAreaControl = new GrassAreaControl(this.app.getCamera());
        grassArea.addControl(grassAreaControl);
        grassArea.setAutoUpdate(true);
        this.app.getRootNode().attachChild(grassArea);
    }

    public TerrainQuad getTerrain() {
        return terrain;
    }

    public void update(float tpf) {

    }
}
