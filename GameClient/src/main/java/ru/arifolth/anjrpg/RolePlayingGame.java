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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.shadow.*;
import com.jme3.system.AppSettings;
import com.jme3.water.WaterFilter;
import ru.arifolth.game.TerrainManager;

import java.awt.*;

public class RolePlayingGame extends SimpleApplication {
    LightScatteringFilter lsf;

    private void initializeApplicationSettings() {
        showSettings = false;

        AppSettings settings = new AppSettings(true);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        settings.setBitsPerPixel(24); //24
        settings.setSamples(16); //16
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

    private DynamicSky sky;
    private TerrainManager terrainManager;
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

        terrainManager.update(tpf);

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
        pssmRenderer = new PssmShadowRenderer(assetManager, 2048, 16);
        pssmRenderer.setShadowIntensity(0.55f);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        viewPort.addProcessor(pssmRenderer);
    }

    private void addFilters() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        FXAAFilter fxaa = new FXAAFilter();
        fpp.addFilter(fxaa);

        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.SceneAndObjects);
        bloom.setDownSamplingFactor(2.0f);
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        fpp.addFilter(bloom);

        lsf = new LightScatteringFilter(sky.getSunDirection().normalize().mult(500));
        lsf.setLightDensity(1.0f);
        fpp.addFilter(lsf);

        DepthOfFieldFilter dof=new DepthOfFieldFilter();
        dof.setFocusDistance(10000);
        dof.setFocusRange(15000);
        dof.setBlurScale(0.65f);
        fpp.addFilter(dof);

        SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.9f);
        //SSAOFilter ssaoFilter = new SSAOFilter(2.9299974f,25f,5.8100376f,0.091000035f);
        fpp.addFilter(ssaoFilter);

        fpp.addFilter(new TranslucentBucketFilter());

        FadeFilter fade = new FadeFilter(3);
        fade.fadeIn();
        fpp.addFilter(fade);

        // add an ocean.
        WaterFilter waterFilter = new WaterFilter(rootNode, sky.getSunDirection().normalize().mult(500));
        waterFilter.setWaterHeight(-70);
        fpp.addFilter(waterFilter);
        viewPort.addProcessor(fpp);

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
        terrainManager = new TerrainManager(assetManager, bulletAppState, this);

        rootNode.attachChild(terrainManager.getTerrain());
    }

    private void setupScreenCapture() {
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        this.stateManager.attach(screenShotState);
    }
}
