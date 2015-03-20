/*
 * Copyright (c) 2012, Andreas Olofsson
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
package se.jod.biomonkey.atmosphere.sky;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Dome;
import se.jod.biomonkey.EcoManager;

/**
 * A skydome using atmospheric scattering shaders.
 * 
 * @author Andreas
 */
public class Sky  {
    
    protected Vector3f lightDir;
    
    protected boolean HDR = false;
    
    protected ScatteringParameters sParams;
    
    protected Material material;
    
    protected Geometry skyDome;
    
    protected float domeRadius;
    protected int horizontalPlanes = 32;
    protected int verticalPlanes = 32;
    
    public Sky(){
        this(900,0);
    }
    
    public Sky(float domeRadius, float height){
        
        sParams = new ScatteringParameters();
        lightDir = new Vector3f(-1,-1,-1).normalizeLocal();
        
        this.domeRadius = domeRadius;
        // ------ Sky geometry and material ------
        Dome dome = new Dome(new Vector3f(), horizontalPlanes, verticalPlanes, domeRadius, true);
        skyDome = new Geometry("SkyDome",dome);
        material = new Material(EcoManager.getInstance().getApp().getAssetManager(), "se/jod/biomonkey/assets/matdefs/SkyBase.j3md");
        skyDome.setMaterial(material);
        skyDome.setQueueBucket(Bucket.Transparent);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
//        skyDome.setCullHint(Spatial.CullHint.Never);
        
        initMaterial(false);
    }
    
    // Inits the sky material with the default params.
    protected void initMaterial(boolean reset){
        
        // Values used to calculate atmospheric depth.
        float Scale = 1f / (sParams.OuterRadius - sParams.InnerRadius);
        float ScaleDepth = (sParams.OuterRadius - sParams.InnerRadius) / 2f;
        float ScaleOverScaleDepth = Scale / ScaleDepth;
        
        // Rayleigh scattering constant.
        float Kr4PI  = sParams.RayleighMultiplier * 4f * FastMath.PI,
              KrESun = sParams.RayleighMultiplier * sParams.SunIntensity;
        // Mie scattering constant.
        float Km4PI  = sParams.MieMultiplier * 4f * FastMath.PI,
              KmESun = sParams.MieMultiplier * sParams.SunIntensity;
        
        // Wavelengths
        Vector3f invWaveLength = new Vector3f(FastMath.pow(sParams.WaveLength.x, -4f),
                                                  FastMath.pow(sParams.WaveLength.y, -4f),
                                                  FastMath.pow(sParams.WaveLength.z, -4f));
        
        
        material.setFloat("Scale", Scale);
        material.setFloat("ScaleDepth", ScaleDepth);
        material.setFloat("ScaleOverScaleDepth", ScaleOverScaleDepth);
        material.setFloat("InnerRadius", sParams.InnerRadius);
        material.setVector3("CameraPos",new Vector3f(0, sParams.InnerRadius + (sParams.OuterRadius-sParams.InnerRadius)*sParams.HeightPosition, 0));
        material.setFloat("Kr4PI", Kr4PI);
        material.setFloat("KrESun", KrESun);
        material.setFloat("Km4PI", Km4PI);
        material.setFloat("KmESun", KmESun);
        material.setInt("NumberOfSamples", sParams.NumberOfSamples);
        material.setFloat("Samples", (float)sParams.NumberOfSamples);
        material.setVector3("InvWaveLength", invWaveLength);
        material.setFloat("G", sParams.G);
        material.setFloat("G2", sParams.G*sParams.G);
        material.setFloat("Exposure", sParams.Exposure);
        
        if(reset == false){
            material.setBoolean("HDR", HDR);
            material.setVector3("LightDir", lightDir);
        }
    }
    
    public float getExposure() {
        return sParams.Exposure;
    }

    /**
     * Set exposure (for non HDR rendering).
     * 
     * @param Exposure 
     */
    public void setExposure(float Exposure) {
        sParams.Exposure = Exposure;
        material.setFloat("Exposure", sParams.Exposure);
    }

    public float getG() {
        return sParams.G;
    }

    /**
     * Set the G param used in the Mie scattering phase function.
     * 
     * @param G 
     */
    public void setG(float G) {
        sParams.G = G;
        material.setFloat("G", sParams.G);
        material.setFloat("G2", sParams.G*sParams.G);
    }

    public float getHeightPosition() {
        return sParams.HeightPosition;
    }

    /**
     * Relative height (0 to 1). Determines how far up the atmosphere the
     * camera is. Very questionable approach, gonna replace.
     * 
     * @param HeightPosition 
     */
    public void setHeightPosition(float HeightPosition) {
        HeightPosition = FastMath.clamp(HeightPosition, 0, 1);
        sParams.HeightPosition = HeightPosition;
        material.setVector3("CameraPos",new Vector3f(0, sParams.InnerRadius + (sParams.OuterRadius-sParams.InnerRadius)*sParams.HeightPosition, 0));
    }

    public float getInnerRadius() {
        return sParams.InnerRadius;
    }

    /**
     * Set the inner radius of the atmosphere.
     * 
     * @param InnerRadius 
     */
    public void setInnerRadius(float InnerRadius) {
        sParams.InnerRadius = InnerRadius;
        
        // Values used to calculate atmospheric depth.
        float Scale = 1f / (sParams.OuterRadius - sParams.InnerRadius);
        float ScaleDepth = (sParams.OuterRadius - sParams.InnerRadius) / 2f;
        float ScaleOverScaleDepth = Scale / ScaleDepth;
        
        material.setFloat("Scale", Scale);
        material.setFloat("ScaleDepth", ScaleDepth);
        material.setFloat("ScaleOverScaleDepth", ScaleOverScaleDepth);
        material.setFloat("InnerRadius", sParams.InnerRadius);
        material.setVector3("CameraPos",new Vector3f(0, sParams.InnerRadius + (sParams.OuterRadius-sParams.InnerRadius)*sParams.HeightPosition, 0));
    }

    public float getMieMultiplier() {
        return sParams.MieMultiplier;
    }

    /**
     * Set the mie scattering constant.
     * 
     * @param MieMultiplier 
     */
    public void setMieMultiplier(float MieMultiplier) {
        sParams.MieMultiplier = MieMultiplier;
        
        // Mie scattering constant.
        float Km4PI  = sParams.MieMultiplier * 4f * FastMath.PI,
              KmESun = sParams.MieMultiplier * sParams.SunIntensity;
        
        material.setFloat("Km4PI", Km4PI);
        material.setFloat("KmESun", KmESun);
    }

    public int getNumberOfSamples() {
        return sParams.NumberOfSamples;
    }

    /**
     * Set the number of samples used in the shader scattering loop.
     * @param NumberOfSamples 
     */
    public void setNumberOfSamples(int NumberOfSamples) {
        sParams.NumberOfSamples = NumberOfSamples;
        material.setInt("NumberOfSamples", sParams.NumberOfSamples);
        material.setFloat("Samples", (float)sParams.NumberOfSamples);
    }

    public float getOuterRadius() {
        return sParams.OuterRadius;
    }

    /**
     * Set the outer radius of the atmosphere.
     * 
     * @param OuterRadius 
     */
    public void setOuterRadius(float OuterRadius) {
        sParams.OuterRadius = OuterRadius;
        
        // Values used to calculate atmospheric depth.
        float Scale = 1f / (sParams.OuterRadius - sParams.InnerRadius);
        float ScaleDepth = (sParams.OuterRadius - sParams.InnerRadius) / 2f;
        float ScaleOverScaleDepth = Scale / ScaleDepth;
        
        material.setFloat("Scale", Scale);
        material.setFloat("ScaleDepth", ScaleDepth);
        material.setFloat("ScaleOverScaleDepth", ScaleOverScaleDepth);
        material.setVector3("CameraPos",new Vector3f(0, sParams.InnerRadius + (sParams.OuterRadius-sParams.InnerRadius)*sParams.HeightPosition, 0));
    }

    public float getRayleighMultiplier() {
        return sParams.RayleighMultiplier;
    }

    /**
     * Set the rayleigh scattering constant value.
     * 
     * @param RayleighMultiplier 
     */
    public void setRayleighMultiplier(float RayleighMultiplier) {
        sParams.RayleighMultiplier = RayleighMultiplier;
        
        // Rayleigh scattering constant.
        float Kr4PI  = sParams.RayleighMultiplier * 4f * FastMath.PI,
              KrESun = sParams.RayleighMultiplier * sParams.SunIntensity;
        
        material.setFloat("Kr4PI", Kr4PI);
        material.setFloat("KrESun", KrESun);
    }

    public float getSunIntensity() {
        return sParams.SunIntensity;
    }

    /**
     * Set the sun intensity value.
     * 
     * @param SunIntensity 
     */
    public void setSunIntensity(float SunIntensity) {
        sParams.SunIntensity = SunIntensity;
        
        // Rayleigh scattering constant.
        float KrESun = sParams.RayleighMultiplier * sParams.SunIntensity;
        // Mie scattering constant.
        float KmESun = sParams.MieMultiplier * sParams.SunIntensity;
        
        material.setFloat("KrESun", KrESun);
        material.setFloat("KmESun", KmESun);
    }

    public Vector3f getWaveLength() {
        return sParams.WaveLength;
    }

    /**
     * Set the wavelength vector:<br/>
     * x = Red<br/>
     * y = Green<br/>
     * z = Blue<br/>
     * Uses micrometers (to set blue 450 nm: WaveLength.z = 0.45 etc)
     * 
     * @param WaveLength 
     */
    public void setWaveLength(Vector3f WaveLength) {
        sParams.WaveLength = WaveLength;
        
        // Wavelengths
        Vector3f invWaveLength = new Vector3f(FastMath.pow(sParams.WaveLength.x, -4f),
                                                  FastMath.pow(sParams.WaveLength.y, -4f),
                                                  FastMath.pow(sParams.WaveLength.z, -4f));
        
        material.setVector3("InvWaveLength", invWaveLength);
    }

    public Vector3f getLightDir() {
        return lightDir;
    }

    /**
     * Set the direction towards the light.
     * 
     * @param lightDir 
     */
    public void setLightDir(Vector3f lightDir) {
        this.lightDir = lightDir;
        material.setVector3("LightDir", lightDir);
    }
    
    public boolean isHDR() {
        return HDR;
    }

    /**
     * Enable HDR rendering. If using a HDR filter, set this to true.
     * 
     * @param HDR 
     */
    public void setHDR(boolean HDR) {
        this.HDR = HDR;
        material.setBoolean("HDR", HDR);
    }

    public ScatteringParameters getsParams() {
        return sParams;
    }

    /**
     * Add a new scattering parameters object.
     * @param sParams 
     */
    public void setsParams(ScatteringParameters sParams) {
        this.sParams = sParams;
        initMaterial(true);
    }
    
    
    public float getDomeRadius() {
        return domeRadius;
    }

    /**
     * Set the radius of the skydome.
     * 
     * @param domeRadius 
     */
    public void setDomeRadius(float domeRadius) {
        this.domeRadius = domeRadius;
        Dome dome = new Dome(new Vector3f(), horizontalPlanes, verticalPlanes, domeRadius, true);
        skyDome.setMesh(dome);
    }

    public int getHorizontalPlanes() {
        return horizontalPlanes;
    }
    
    /**
     * Set the number of horizontal planes in the skydome.
     * 
     * @param horizontalPlanes 
     */
    public void setHorizontalPlanes(int horizontalPlanes){
        this.horizontalPlanes = horizontalPlanes;
        Dome dome = new Dome(new Vector3f(), horizontalPlanes, verticalPlanes, domeRadius, true);
        skyDome.setMesh(dome);
    }
    
    public int getVerticalPlanes(){
        return verticalPlanes;
    }
    
    /**
     * Set the number of vertical planes in the skydome (the amount of
     * subdivisions of each circle).
     * 
     * @param verticalPlanes 
     */
    public void setVerticalPlanes(int verticalPlanes){
        this.verticalPlanes = verticalPlanes;
        Dome dome = new Dome(new Vector3f(), horizontalPlanes, verticalPlanes, domeRadius, true);
        skyDome.setMesh(dome);
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Geometry getSkyDome() {
        return skyDome;
    }

    public void setSkyDome(Geometry skyDome) {
        this.skyDome = skyDome;
    }

}
