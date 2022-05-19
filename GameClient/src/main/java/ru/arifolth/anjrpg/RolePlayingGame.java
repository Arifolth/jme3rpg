/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

package ru.arifolth.anjrpg;

import com.idflood.sky.DynamicSky;
import com.jayfella.minimap.MiniMapState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.ui.Picture;
import com.jme3.water.WaterFilter;
import com.simsilica.lemur.OptionPanelState;
import com.simsilica.lemur.event.PopupState;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.tools.SizeValue;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import ru.arifolth.anjrpg.menu.MainMenuState;
import ru.arifolth.game.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class RolePlayingGame extends SimpleApplication implements RolePlayingGameInterface {
    public static final SSAOFilter SSAO_FILTER_BASIC = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.9f);
    public static final SSAOFilter SSAO_FILTER_STRONG = new SSAOFilter(2.9299974f, 25f, 5.8100376f, 0.091000035f);
    public static final String POM_XML = "pom.xml";
    public static final int WATER_LEVEL_HEIGHT = -70;
    protected String version;
    protected Element progressBarElement;
    protected TextRenderer textRenderer;
    private LightScatteringFilter lsf;
    private WaterFilter waterFilter;
    private volatile float progress;
    final private static Logger LOGGER = Logger.getLogger(RolePlayingGame.class.getName());
    private DynamicSky sky;
    private TerrainManagerInterface terrainManager;
    private SoundManagerInterface soundManager;
    protected BulletAppState bulletAppState;
    private PssmShadowRenderer pssmRenderer;
    private GameLogicCoreInterface gameLogicCore;

    public RolePlayingGame() throws IOException, XmlPullParserException {
        super(new FlyCamAppState(),
                new AudioListenerState(),
                new PopupState(),
                new OptionPanelState(),
                new MainMenuState()
        );

        version = new MavenXpp3Reader().read(new FileReader(POM_XML)).getVersion();
    }

    @Override
    public void simpleInitApp() {
        /* Game stuff */
        setupAssetManager();
    }

    protected void loadResources() {
        setupShadowRenderer();
        setupScreenCapture();
        addFog();
        setupSky();
        addFilters();

        attachDamageIndicator();
        attachPlayer();
        attachTerrain();
        attachSky();

        enablePhysics();
        attachNPC();
    }

    protected void attachDamageIndicator() {
        rootNode.attachChild(gameLogicCore.getDamageIndicator());
    }

    protected void createMinimap() {
        // create the minimap
        MiniMapState miniMapState = new MiniMapState(getRootNode());
        stateManager.attach(miniMapState);
    }

    void setupGameLogic() {
        gameLogicCore = new GameLogicCore(this, cam, flyCam, inputManager, bulletAppState, assetManager, soundManager, terrainManager, getRootNode());
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
        lsf.setLightPosition(sky.getSunDirection().normalize());
        pssmRenderer.setDirection(sky.getSunDirection().normalize());
        waterFilter.setLightDirection(sky.getSunDirection().normalize());
    }

    void addFog() {
        /** Add fog to a scene */
        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        FogFilter fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(1000);
        fog.setFogDensity(0.255f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    void setupShadowRenderer() {
        pssmRenderer = new PssmShadowRenderer(assetManager, 2048, 16);
        pssmRenderer.setShadowIntensity(0.55f);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        viewPort.addProcessor(pssmRenderer);

        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void addFilters() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        FXAAFilter fxaa = new FXAAFilter();
        fxaa.setSubPixelShift(5.0f);
        fxaa.setReduceMul(5.0f);
        fxaa.setVxOffset(5.0f);
        fxaa.setEnabled(true);
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
        dof.setFocusDistance(0);
        dof.setFocusRange(50);
        dof.setBlurScale(1.4f);
        fpp.addFilter(dof);

        SSAOFilter ssaoFilter = SSAO_FILTER_BASIC;
        fpp.addFilter(ssaoFilter);

        fpp.addFilter(new TranslucentBucketFilter());

        // add an ocean.
        waterFilter = new WaterFilter(getRootNode(), sky.getSunDirection().normalize());
        waterFilter.setWaterHeight(WATER_LEVEL_HEIGHT);
        fpp.addFilter(waterFilter);
        viewPort.addProcessor(fpp);

        CartoonEdgeFilter toon=new CartoonEdgeFilter();
        toon.setEdgeWidth(0.5f);
        toon.setEdgeIntensity(0.09f);
        toon.setNormalThreshold(0.8f);
        fpp.addFilter(toon);

        viewPort.addProcessor(fpp);

        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupSky() {
        // load sky
        sky = new DynamicSky(assetManager, viewPort, getRootNode());
        getRootNode().setShadowMode(ShadowMode.Off);
        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    protected void attachTerrain() {
        getRootNode().attachChild(terrainManager.getTerrain());
        //terrainManager.generateGrass();
    }

    protected void attachSky() {
        getRootNode().attachChild(sky);
    }

    protected void enablePhysics() {
        gameLogicCore.enablePhysics();
        setProgress(new Object() {}.getClass().getEnclosingMethod().getName());
    }

    protected void attachPlayer() {
        gameLogicCore.attachPlayer();
    }

    protected void attachNPC() {
        gameLogicCore.attachNPC();
    }

    @Override
    public GameLogicCoreInterface getGameLogicCore() {
        return gameLogicCore;
    }

    @Override
    public synchronized Node getRootNode() {
        return this.rootNode;
    }

    void setupPhysix() {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.setEnabled(true);
        //collision capsule shape is visible in debug mode
        bulletAppState.setDebugEnabled(true);
//        setProgress(new Object() {}.getClass().getEnclosingMethod().getName());
    }

    void setupTerrain() {
        terrainManager = new TerrainManager(assetManager, bulletAppState, this);
//        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupSound() {
        soundManager = new SoundManager(assetManager);
//        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupScreenCapture() {
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        stateManager.attach(screenShotState);
        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    public void setProgress(final String loadingText) {
        if(loadingText.equals("Loading complete"))
            progress = 1f;
        else
            progress += 0.1;

        //Since this method is called from another thread, we enqueue the
        //changes to the progressbar to the update loop thread.
        enqueue(() -> {
            final int MIN_WIDTH = 32;
            int pixelWidth = (int) (MIN_WIDTH + (progressBarElement.getParent().getWidth() - MIN_WIDTH) * progress) * 2;
            if(pixelWidth > progressBarElement.getParent().getWidth())
                pixelWidth = progressBarElement.getParent().getWidth();
            progressBarElement.setConstraintWidth(new SizeValue(pixelWidth + "px"));
            progressBarElement.getParent().layoutElements();

            textRenderer.setText(loadingText);

            return null;
        });
    }

    @Override
    public SoundManagerInterface getSoundManager() {
        return soundManager;
    }

    @Override
    public TerrainManagerInterface getTerrainManager() {
        return terrainManager;
    }

    @Override
    public void setTerrainManager(TerrainManagerInterface terrainManager) {
        this.terrainManager = terrainManager;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
