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

package ru.arifolth.anjrpg;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.water.WaterFilter;
import ru.arifolth.anjrpg.filters.FSRCASFilter;
import ru.arifolth.anjrpg.filters.FilmGrainFilter;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.menu.SettingsUtils;
import ru.arifolth.terrain.grass.GpuGrassMaterial;

public class FilterManager implements FilterManagerInterface {

    private Node rootNode;
    private AssetManager assetManager;
    private LightScatteringFilter lsf;
    private PssmShadowRenderer pssmRenderer;
    private SkyInterface sky;
    private  ViewPort viewPort;
    private WaterFilter waterFilter;
    private FilterPostProcessor fpp;
    private FogFilter fog;
    private GammaCorrectionFilter gammaCorrectionFilter;
    private Renderer renderer;
    private TileTextureCapturer tileTextureCapturer;
    private SimpleApplication application;
    private FilmGrainFilter filmGrainFilter;
    private FSRCASFilter fsrCASFilter;

    public FilterManager(SimpleApplication application, AssetManager assetManager, Node rootNode, ViewPort viewPort, SkyInterface sky, Renderer renderer, TerrainManagerInterface terrainManager) {
        this.application = application;
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.viewPort = viewPort;
        this.sky = sky;
        this.renderer = renderer;

        this.renderer.setDefaultAnisotropicFilter(1);

        setupShadowRenderer();
        setupFilterPostProcessor(assetManager);

        setupSSAOFilter();
        setupDepthOfFieldFilter();

        setupFog();
        setupTranslucentBucketFilter();

        setupCartoonEdgeFilter();

        setupBloomFilter();
        setupToneMapFilter();

        setupFXAAFilter();

        setupFSRCASFilter();
        setupGammaCorrectionFilter();
        // NOTE: the film grain filter is deliberately NOT added here.
        // FilterPostProcessor executes filters in the order they were
        // added and the last enabled filter's output goes to the screen,
        // so anything appended later composites OVER the grained image.
        // The WaterFilter is appended later (initialize()), so grain
        // added here ran before the water and the water pass kept
        // repainting clean, ungrained pixels over it - the "water is not
        // affected by film grain" bug. See initialize().

        viewPort.addProcessor(fpp);

        tileTextureCapturer = terrainManager.getTerrainBuilder().getTileCapturer();
    }

    private void setupFXAAFilter() {
        FXAAFilter fxaa = new FXAAFilter();
        fxaa.setSubPixelShift(5.0f);
        fxaa.setReduceMul(5.0f);
        fxaa.setVxOffset(5.0f);
        fxaa.setEnabled(true);
        fpp.addFilter(fxaa);
    }

    private void setupBloomFilter() {
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.SceneAndObjects);
        bloom.setDownSamplingFactor(2.0f);
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        fpp.addFilter(bloom);
    }

    private void setupFilterPostProcessor(AssetManager assetManager) {
        fpp = new FilterPostProcessor(assetManager);
    }

    private void setupFSRCASFilter() {
        fsrCASFilter = new FSRCASFilter();
        fsrCASFilter.setSharpness(0.0f); // 0.0 = soft (no sharpening), 1.0 = sharp
        // The Advanced Video panel may have switched this off in the main menu,
        // before the world (and this manager) was loaded - honor it here.
        fsrCASFilter.setEnabled(advancedVideoEnabled(Constants.ADVANCED_VIDEO_FSR_CAS));
        fpp.addFilter(fsrCASFilter);
    }

    @Override
    public void initialize() {
        setupLightScatteringFilter();

        setupWaterFilter();

        // Film grain goes LAST, after the water: the add order above is
        // the render order, so the grain is now applied to the FINAL
        // image - scene, water and god rays - instead of being painted
        // over by the water pass. As the final filter it also receives
        // display-ready (already gamma-encoded) colors, which is the
        // space film_grain.frag v5 weights its grain in. Fog, appended at
        // runtime by the weather, blends toward fog color only where the
        // fog is dense and passes the grained pixels through untouched.
        setupFilmGrainFilter();
    }

    private void setupFilmGrainFilter() {
        filmGrainFilter = new FilmGrainFilter(application);
        filmGrainFilter.setGrainRate(30f);
        // Honor a toggle made from the main menu before the world was loaded
        filmGrainFilter.setEnabled(advancedVideoEnabled(Constants.ADVANCED_VIDEO_FILM_GRAIN));
        fpp.addFilter(filmGrainFilter);
    }

    /**
     * Reads an Advanced Video toggle from the settings; an absent key means
     * the default (on). See Constants.ADVANCED_VIDEO_*.
     */
    private boolean advancedVideoEnabled(String settingsKey) {
        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        return SettingsUtils.readToggle(settings, settingsKey);
    }

    private void setupGammaCorrectionFilter() {
        gammaCorrectionFilter = new GammaCorrectionFilter();
        fpp.addFilter(gammaCorrectionFilter);
    }

    @Override
    public GammaCorrectionFilter getGammaCorrectionFilter() {
        return gammaCorrectionFilter;
    }

    /**
     * Film grain composites over the final image because it was added last
     * (after Water, see initialize()). Callers toggle it with setEnabled();
     * the chain position is preserved.
     */
    @Override
    public FilmGrainFilter getFilmGrainFilter() {
        return filmGrainFilter;
    }

    /**
     * FSR CAS runs before GammaCorrection. Callers toggle it with
     * setEnabled(); the chain position is preserved.
     */
    @Override
    public FSRCASFilter getFsrCASFilter() {
        return fsrCASFilter;
    }

    private void setupToneMapFilter() {
        ToneMapFilter toneMapFilter = new ToneMapFilter();
        toneMapFilter.setWhitePoint(new Vector3f(8f, 8f, 8f));
        fpp.addFilter(toneMapFilter);
    }

    private void setupFog() {
        fog = new FogFilter();

        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(5000);
        fog.setFogDensity(2.255f);
    }

    private void setupCartoonEdgeFilter() {
        CartoonEdgeFilter toon=new CartoonEdgeFilter();
        toon.setEdgeWidth(0.5f);
        toon.setEdgeIntensity(0.09f);
        toon.setNormalThreshold(0.8f);
        fpp.addFilter(toon);
    }

    private void setupWaterFilter() {
        // add an ocean.
        waterFilter = new WaterFilter(rootNode, sky.getSunDirection().normalize());
        waterFilter.setWaterHeight(Constants.WATER_LEVEL_HEIGHT);
        fpp.addFilter(waterFilter);

        // Pass reference to texture capturer
        tileTextureCapturer.setMainWaterFilter(waterFilter);
    }


    @Override
    public void addFog() {
        /** Add fog to a scene */
        fpp.addFilter(fog);
    }

    @Override
    public void removeFog() {
        fpp.removeFilter(fog);
    }

    private void setupShadowRenderer() {
        pssmRenderer = new PssmShadowRenderer(assetManager, 2048, 16);
        pssmRenderer.setShadowIntensity(0.55f);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        viewPort.addProcessor(pssmRenderer);
    }

    private void setupTranslucentBucketFilter() {
        fpp.addFilter(new TranslucentBucketFilter());
    }

    private void setupSSAOFilter() {
        SSAOFilter ssaoFilter = Constants.SSAO_FILTER_BASIC;
        fpp.addFilter(ssaoFilter);
    }

    private void setupDepthOfFieldFilter() {
        DepthOfFieldFilter dof=new DepthOfFieldFilter();
        dof.setFocusDistance(0);
        dof.setFocusRange(20);
        dof.setBlurScale(0.325f);
        fpp.addFilter(dof);
    }

    private void setupLightScatteringFilter() {
        lsf = new LightScatteringFilter(sky.getSunDirection().negate().normalizeLocal().mult(500));
        lsf.setLightDensity(1.0f);
        fpp.addFilter(lsf);
    }

    @Override
    public void update(float tpf) {
        lsf.setLightPosition(sky.getSunDirection().normalize().normalize().mult(500));
        pssmRenderer.setDirection(sky.getSunDirection().negate().normalize().mult(500));
        waterFilter.setLightDirection(sky.getSunDirection().normalize().mult(500));
        //Backlight translucency: same live sun direction source as the other
        //sun-driven effects; GpuGrassMaterial computes the glow axis and the
        //day-night fade once per frame for all material clones.
        GpuGrassMaterial.setSharedSunDirection(sky.getSunDirection());
    }
}
