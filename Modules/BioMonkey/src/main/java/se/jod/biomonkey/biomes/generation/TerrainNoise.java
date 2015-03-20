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
package se.jod.biomonkey.biomes.generation;

import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.terrain.noise.fractal.FractalSum;
import java.nio.FloatBuffer;

/**
 * A noise generator for terrain.
 * 
 * @author Andreas
 */
public class TerrainNoise {
    
    protected HMFT base;
    protected ImprovedNoise roughness;
    protected FractalSum distortion;
    
    public TerrainNoise(){
        this.base = new HMFT();
        this.roughness = new ImprovedNoise();
        this.distortion = new FractalSum();
        this.distortion.setScale(8f);
    }
    
    public FloatBuffer getRoughnessNoise(float x, float z, int tileSize){
        return roughness.getBuffer(x*(tileSize - 1), z*(tileSize - 1), 0, tileSize);
    }
    
    public float getHeightValue(float x, float z, int tileX, int tileZ, int size){
        return base.value((tileX + x / size), (tileZ + z / size), 0);
    }
    
    public float getRoughnessValue(float x, float z, int tileX, int tileZ, int size){
        return roughness.value((tileX + x / size), (tileZ + z / size), 0);
    }
    
    public float getDistortionValue(float x, float z, int tileX, int tileZ, int size){
        return distortion.value((tileX + x / size), (tileZ + z / size), 0);
    }
    
    public HMFT getHeightMapGenerator(){
        return base;
    }
    
    public void setSeed(int seed){
        base.setSeed(seed);
    }
    
    public void setScale(float scale){
        base.setScale(scale);
    }
    
    public void setHeightOffset(float heightOffset){
        base.setOffset(heightOffset);
    }
    
}
