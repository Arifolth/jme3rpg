/*
 * Copyright (c) 2011, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.jod.biomonkey.grass;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.MaterialSP;
import se.jod.biomonkey.paging.DetailLevel;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPageLoader;

/**
 * The GrassLayer class contains data specific to a type of grass.
 *
 * @author Andreas
 */
public class GrassLayer extends GeometryLayer {

    public enum MeshType {

        QUADS, //One static quad per patch of grass.
        CROSSQUADS, //Two crossed static quads per patch of grass.
    }
    protected MaterialSP material;
    protected MeshType type = MeshType.CROSSQUADS;
    //The individual grass-patches height and width range.
    protected float maxHeight = 1.2f, minHeight = 0.8f;
    protected float maxWidth = 1.2f, minWidth = 0.8f;
    //This value is stored as the tangent of the slope, to save calculations.
    protected float maxTerrainSlope = (float) Math.tan(Math.toRadians(30.0));
    //Material parameters.
    protected boolean swaying = false;
    protected Vector3f swayData;
    protected Vector2f wind;
    protected boolean lighting = true;
    protected ColorRGBA specularColor;
    protected boolean highQualityFading = true;
    protected Texture alphaNoiseMap;
    protected Texture grassTexture;
    protected Texture normalTexture;
    protected Texture specularTexture;
    protected float alphaCutOff = 0.3f;
    boolean materialInitialized = false;

    public GrassLayer() {
        super();
    }

    public GrassLayer(Texture tex) {
        super();
        this.grassTexture = tex;
        initMaterial();
    }

    public GrassLayer(Texture tex, Texture normal) {
        super();
        this.grassTexture = tex;
        this.normalTexture = normal;
        initMaterial();
    }

    /**
     *
     * @param tex The grass texture.
     * @param pageLoader The grassloader.
     */
    public GrassLayer(Texture tex, GrassLoader pageLoader) {
        this(tex);
        this.pageLoader = pageLoader;
    }

    /**
     *
     * @param tex The grass texture.
     * @param normal The normal texture.
     * @param pageLoader The grassloader.
     */
    public GrassLayer(Texture tex, Texture normal, GrassLoader pageLoader) {
        this(tex, normal);
        this.pageLoader = pageLoader;
    }

    /**
     * Internal method.
     */
    protected final void initMaterial() {
        AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
        this.material = new MaterialSP(assetManager, "se/jod/biomonkey/assets/matdefs/GrassBase.j3md");

        // TODO add an alternative.
        if (grassTexture != null) {
            material.setTexture("DiffuseMap", grassTexture);
        } else {
            throw new RuntimeException("Grass texture cannot be null.");
        }

        if (normalTexture != null) {
            material.setTexture("NormalMap", normalTexture);
        }

        material.setBoolean("HighQualityFading", highQualityFading);
        
        if (highQualityFading) {
            if (alphaNoiseMap == null) {
                alphaNoiseMap = assetManager.loadTexture("se/jod/biomonkey/assets/textures/noise.png");
            }
            material.setTexture("AlphaNoiseMap", alphaNoiseMap);
        }

        material.setFloat("AlphaDiscardThreshold", alphaCutOff);

        swayData = new Vector3f(1.0f, 0.5f, 1f);
        wind = new Vector2f(1, 1);

        material.setBoolean("Lighting", lighting);
        material.setBoolean("Swaying", swaying);
        material.setVector3("SwayData", swayData);
        material.setVector2("Wind", wind);

        material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
//        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        EcoManager.getInstance().getAtmosphereManager().getFogManager().addMaterial(material);
        materialInitialized = true;
    }

    public void update() {
    }

    /**
     * Set the mesh type to use for the grass.
     *
     * @param type
     */
    public void setMeshType(MeshType type) {
        this.type = type;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public MeshType getMeshType() {
        return type;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    /**
     * Set the maximum height scale of the grass quads. Default value is 1.2
     *
     * @param maxHeight
     */
    public void setMaxHeight(float maxHeight) {
        if (maxHeight < minHeight) {
            throw new RuntimeException("Max height needs to be larger then or equal to min height.");
        }
        this.maxHeight = maxHeight;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    /**
     * Set the maximum width scale of the grass quads. Default value is 1.2
     *
     * @param maxWidth
     */
    public void setMaxWidth(float maxWidth) {
        if (maxWidth < minWidth) {
            throw new RuntimeException("Max width needs to be larger then or equal to min width.");
        }
        this.maxWidth = maxWidth;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMinHeight() {
        return minHeight;
    }

    /**
     * Set the minimum height scale of the grass quads. Default value is 0.8
     *
     * @param minHeight
     */
    public void setMinHeight(float minHeight) {
        if (minHeight > maxHeight) {
            throw new RuntimeException("Min height needs to be smaller then or equal to max height.");
        }
        this.minHeight = minHeight;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMinWidth() {
        return minWidth;
    }

    /**
     * Set the minimum width scale of the grass quads. Default value is 0.8
     *
     * @param minWidth
     */
    public void setMinWidth(float minWidth) {
        if (minWidth > maxWidth) {
            throw new RuntimeException("Min width needs to be smaller then or equal to max width.");
        }
        this.minWidth = minWidth;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    /**
     * Set the maximum slope of the terrain in degrees. If the terrain slope is
     * higher then that number, no grass will be planted.
     *
     * @param angleInDegrees An angle between 0 and 90 (degrees).
     */
    public void setMaxTerrainSlope(float angleInDegrees) {
        float aID = FastMath.clamp(angleInDegrees, 0, 90);
        maxTerrainSlope = (float) Math.tan(Math.toRadians(aID));
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMaxTerrainSlope() {
        return maxTerrainSlope;
    }

    public boolean isSwaying() {
        return swaying;
    }

    public Material getMaterial() {
        return material;
    }

    /**
     * Should the grass sway in the wind?
     *
     * @param swaying
     */
    public void setSwaying(boolean swaying) {
        this.swaying = swaying;
        material.setBoolean("Swaying", swaying);
    }

    public void setWind(Vector2f wind) {
        this.wind = wind;
        material.setVector2("Wind", wind);
    }

    /**
     * Set the parameters used for wind animation.
     *
     * @param swayData
     */
    public void setSwayData(Vector3f swayData) {
        this.swayData = swayData;
        material.setVector3("SwayData", swayData);
    }

    /**
     * Set the frequency of the swaying animation.
     *
     * @param distance
     */
    public void setSwayingFrequency(float distance) {
        swayData.x = distance;
        material.setVector3("SwayData", swayData);
    }

    /**
     * Set the variation of the swaying animation. High values will cause the
     * swaying to be more irregular over large areas.
     *
     * @param distance
     */
    public void setSwayingVariation(float distance) {
        swayData.y = distance;
        material.setVector3("SwayData", swayData);
    }

    /**
     * Set the amplitude (strength) of the animation. Frequency sets the speed
     * of the animation, while amplitude sets the strength.
     *
     * @param amplitude
     */
    public void setSwayingAmplitude(float amplitude) {
        swayData.z = amplitude;
        material.setVector3("SwayData", swayData);
    }

    public Texture getAlphaNoiseMap() {
        return alphaNoiseMap;
    }

    /**
     * Set the alpha noise map to use with high quality fading. Setting this
     * param enables high quality fading.
     *
     * @param alphaNoiseMap
     */
    public void setAlphaNoiseMap(Texture alphaNoiseMap) {
        this.alphaNoiseMap = alphaNoiseMap;
        this.highQualityFading = true;
        material.setTexture("AlphaNoiseMap", alphaNoiseMap);
        material.setBoolean("HighQualityFading", highQualityFading);
    }

    public boolean isHighQualityFading() {
        return highQualityFading;
    }

    public void setHighQualityFading(boolean highQualityFading) {
        this.highQualityFading = highQualityFading;
        material.setBoolean("HighQualityFading", highQualityFading);
        if (alphaNoiseMap == null) {
            alphaNoiseMap = EcoManager.getInstance().getApp().getAssetManager().loadTexture("se/jod/biomonkey/assets/textures/noise.png");
        }
        material.setTexture("AlphaNoiseMap", alphaNoiseMap);
    }

    public Texture getGrassTexture() {
        return grassTexture;
    }

    public void setGrassTexture(Texture grassTexture) {
        this.grassTexture = grassTexture;
        material.setTexture("DiffuseMap", grassTexture);
        if (materialInitialized == false) {
            initMaterial();
        }
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public void setNormalTexture(Texture normalTexture) {
        this.normalTexture = normalTexture;
        material.setTexture("NormalMap", normalTexture);
    }

    public float getAlphaCutOff() {
        return alphaCutOff;
    }

    public void setAlphaCutOff(float alphaCutOff) {
        this.alphaCutOff = alphaCutOff;
        material.setFloat("AlphaDiscardThreshold", alphaCutOff);
    }

    public boolean isLighting() {
        return lighting;
    }

    public void setLighting(boolean lighting) {
        this.lighting = lighting;
        material.setBoolean("Lighting", lighting);
    }

    @Override
    public void setPageLoader(GeometryPageLoader pageLoader) {
        super.setPageLoader(pageLoader);
        DetailLevel dl = this.getPageLoader().getPagingManager().getDetailLevels().get(0);
        float fadeEnd = dl.getFarTransDist();
        float fadeRange = fadeEnd - dl.getFarDist();
        material.setFloat("FadeEnd", fadeEnd);
        material.setFloat("FadeRange", fadeRange);
    }
}//GrassLayer
