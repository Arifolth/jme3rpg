package ru.arifolth.anjrpg; /**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 16.12.12
 * Time: 1:36
 * To change this template use File | Settings | File Templates.
 */

import com.idflood.sky.DynamicSky;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.awt.*;


public class RolePlayingGame extends SimpleApplication {
    LightScatteringFilter lsf;

    private void initializeApplicationSettings() {
        showSettings = false;

        AppSettings settings = new AppSettings(true);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        settings.setBitsPerPixel(24); //24
        settings.setSamples(1); //16
        settings.setVSync(false);
        settings.setResolution(3840,2160);
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        settings.setFrameRate(30);

        //setDisplayFps(true);
        //setDisplayStatView(false);

        this.setSettings(settings);
        this.setShowSettings(showSettings);
    }

    private static Application app;
    private TerrainQuad terrain;
    private Material matTerrain;
    private RigidBodyControl landscape;
    private DynamicSky sky;
    private BulletAppState bulletAppState;
    private PssmShadowRenderer pssmRenderer;
    private GameLogicCore gameLogicCore;
    private Vector3f lightDir;

    public RolePlayingGame() {
        initializeApplicationSettings();
    }

    public static void main(String[] args) {
        app = new RolePlayingGame();
        app.start();
    }

    public void simpleInitApp() {
        //chain of responsibility
        setupAssetManager();
        setupPhysix();
        setupGameLogic();

        setupTerrain();

        setupShadowRenderer();
        setupScreenCapture();
        //addFog();
        setupSky();
        addFilters();
    }

    private void setupGameLogic() {
        gameLogicCore = new GameLogicCore(cam, flyCam, inputManager, bulletAppState, assetManager, rootNode);
        gameLogicCore.initialize();
    }

    private void setupAssetManager() {
        assetManager.registerLocator("assets", ClasspathLocator.class);
    }

    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the playerControl is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled playerControl walk.
     * We also make sure here that the camera moves with playerControl.
     */
    @Override
    public void simpleUpdate(float tpf) {
        gameLogicCore.update(tpf);

        //skydome
        sky.updateTime();

        //update filters on observer pattern base
        lsf.setLightPosition(sky.getSunDirection().normalize().mult(500));

        pssmRenderer.setDirection(sky.getSunDirection().normalize().mult(-500));
    }

    private void addFog() {
        /** Add fog to a scene */
        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        FogFilter fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(800);
        fog.setFogDensity(1.0f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    private void setupShadowRenderer() {
        pssmRenderer = new PssmShadowRenderer(assetManager, 4096, 32);
        //pssmRenderer.setDirection(lightDir);
        pssmRenderer.setShadowIntensity(0.55f);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        terrain.setShadowMode(ShadowMode.CastAndReceive);
        viewPort.addProcessor(pssmRenderer);
    }

    private void addFilters() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        /*FXAAFilter fxaa = new FXAAFilter();
        fpp.addFilter(fxaa);*/

        /*BloomFilter bloom = new BloomFilter(*//*BloomFilter.GlowMode.Objects*//*);
        bloom.setDownSamplingFactor(2.0f);
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        fpp.addFilter(bloom);*/

        lsf = new LightScatteringFilter(sky.getSunDirection().normalize());
        lsf.setLightDensity(1.0f);
        //LightScatteringUI ui = new LightScatteringUI(inputManager, lsf);
        fpp.addFilter(lsf);

        /*DepthOfFieldFilter dof=new DepthOfFieldFilter();
        dof.setFocusDistance(10000);
        dof.setFocusRange(15000);
        dof.setBlurScale(0.65f);
        fpp.addFilter(dof);*/

        //SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.9f);
        /*SSAOFilter ssaoFilter = new SSAOFilter(10.0f, 25.0f, 0.35f, 1.0f);
        fpp.addFilter(ssaoFilter);

        fpp.addFilter(new TranslucentBucketFilter());*/

        /*
        FadeFilter fade = new FadeFilter(3);
        fpp.addFilter(fade);
        fade.fadeIn();
        */

        /*
        CartoonEdgeFilter toon=new CartoonEdgeFilter();
        toon.setEdgeColor(ColorRGBA.Yellow);
        fpp.addFilter(toon);
        */

        viewPort.addProcessor(fpp);
    }

    private void setupSky() {
        // load sky
        sky = new DynamicSky(assetManager, viewPort, rootNode);
        rootNode.attachChild(sky);
        rootNode.setShadowMode(ShadowMode.Off);
    }

    private void setupPhysix() {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.setEnabled(true);
        //collision capsule shape is visible in debug mode
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
    }

    private void setupTerrain() {
        /** 1. Create terrain material and load four textures into it. */
        /*matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);*/
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        matTerrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        //matTerrain.setTexture("GrassAlphaMap", assetManager.loadTexture(
                //"Textures/Terrain/grass-map512.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex1", grass);
        matTerrain.setFloat("Tex1Scale", 256f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex2", dirt);
        matTerrain.setFloat("Tex2Scale", 128f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex3", rock);
        matTerrain.setFloat("Tex3Scale", 512f);


        /** 2. Create the height map */

        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();
        /*
        HillHeightMap heightmap = null;
        HillHeightMap.NORMALIZE_RANGE = 100; // optional
        try {                                       //50/75
            heightmap = new HillHeightMap(513, 1000, 50, 350, (byte) 0); // byte 3 is a random seed
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */

        /** 3. We have prepared material and heightmap.
         * Now we createCharacter the actual terrain:
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */

        /*
         int patchSize = 97;
            terrain = new TerrainQuad(
                "my terrain",
                patchSize,
                1025,
                heightmap.getHeightMap());
         */
        int patchSize = 65;
        terrain = new TerrainQuad(
                "my terrain",
                patchSize,
                513,
                heightmap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(matTerrain);
        //terrain.setLocalTranslation(0, -1000, 0);
        //terrain.setLocalScale(2f, 1f, 2f);
        terrain.setLocalTranslation(0, -1150, 0);
        terrain.setLocalScale(20f, 10f, 20f);
        rootNode.attachChild(terrain);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);
        // We load the scene from the zip file and adjust its size.
        //assetManager.registerLocator("town.zip", ZipLocator.class);
        //sceneModel = assetManager.loadModel("main.scene");
        //sceneModel.setLocalScale(2f);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) terrain);
        landscape = new RigidBodyControl(sceneShape, 0);
        terrain.addControl(landscape);

        //debug terrain
        //Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        //terrain.generateDebugTangents(debugMat);
        //terrain.addControl(new SimpleGrassControl(assetManager,"Textures/Terrain/grass/weedy_grass_clover_9091077.JPG"));

        // We attach the scene and the playerControl to the rootNode and the physics space,
        // to make them appear in the game world.
        bulletAppState.getPhysicsSpace().add(landscape);
    }

    private void setupScreenCapture() {
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        this.stateManager.attach(screenShotState);
    }

}
