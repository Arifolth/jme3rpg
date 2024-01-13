/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.water.WaterFilter;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.FilterManagerInterface;
import ru.arifolth.anjrpg.interfaces.SkyInterface;

public class FilterManager implements FilterManagerInterface {

    private Node rootNode;
    private AssetManager assetManager;
    private LightScatteringFilter lsf;
    private PssmShadowRenderer pssmRenderer;
    private SkyInterface sky;
    private  ViewPort viewPort;
    private WaterFilter waterFilter;
    private FilterPostProcessor fpp;


    public FilterManager(AssetManager assetManager, Node rootNode, ViewPort viewPort, SkyInterface sky) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.viewPort = viewPort;
        this.sky = sky;
    }

    @Override
    public void initialize() {
        fpp = new FilterPostProcessor(assetManager);

        //addFog();

        setupFilterPostProcessor();
        setupLightScatteringFilter();
//        setupDepthOfFieldFilter();
        setupSSAOFilter();
        //setupTranslucentBucketFilter();
        setupShadowRenderer();
        setupWaterFilter();
        setupCartoonEdgeFilter();

        viewPort.addProcessor(fpp);
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
    }


    private void addFog() {
        /** Add fog to a scene */
        FogFilter fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(5000);
        fog.setFogDensity(2.255f);
        fpp.addFilter(fog);
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
        dof.setFocusRange(40);
        dof.setBlurScale(1.125f);
        fpp.addFilter(dof);
    }

    private void setupLightScatteringFilter() {
        lsf = new LightScatteringFilter(sky.getSunDirection().normalize().mult(500));
        lsf.setLightDensity(1.0f);
        fpp.addFilter(lsf);
    }

    private void setupFilterPostProcessor() {
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
    }

    @Override
    public void update(float tpf) {
        lsf.setLightPosition(sky.getSunDirection().normalize().normalize().mult(500));
        pssmRenderer.setDirection(sky.getSunDirection().negate().normalize().mult(500));
        waterFilter.setLightDirection(sky.getSunDirection().normalize().mult(500));
    }
}
