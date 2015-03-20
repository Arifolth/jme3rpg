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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.terrain.noise.Basis;
import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.terrain.noise.basis.Noise;

/**
 * "Hetrogenous Multi Fractal Terrain". This class generates a standard 
 * multi-fractal based terrain heightfield.
 * 
 * @author Andreas
 */
public class HMFT extends Noise {

//    protected ImprovedNoise offsetNoise;
    
    protected Basis basis;
    protected float lacunarity;
    protected float H;
    protected float octaves;
    protected float minOffset, maxOffset;
    protected float offset = 0.6f;
    
    protected float seed = 66; // 66
    
    protected float[] exponent_array;
    protected Vector3f p = new Vector3f();

    public HMFT() {
        this.basis = new ImprovedNoise();
        this.lacunarity = 2.124367f;
        this.H = 0.25f;
        this.octaves = 8;
        this.maxOffset = 1f;
        this.minOffset = 0.2f;
        this.scale = 0.5f;
        
        calcExponentArray();
        
//        this.offsetNoise = new ImprovedNoise();
        
    }

    @Override
    public float value(float x, float y, float z) {
//        return this.basis.value(x, y, z)*0.5f + 0.5f;
        p.set(x + seed, y + seed, z + seed);
//        float offset = offsetNoise.value(x, y, 0);
//        offset = (1 + offset)*0.5f*(maxOffset - minOffset) + minOffset;
//        float offset = 0.8f;
        float signal = 0, weight = 0;
        // Get first octave. Will be used as basis for the rest.
        float result = this.basis.value(p.x*scale, p.y*scale, p.z*scale) + offset;
        weight = result;
        p.multLocal(lacunarity);
        

        for (int i = 1; i < octaves; i++) {
            if(weight > 1){weight = 1;}
            signal = (this.basis.value(p.x*scale, p.y*scale, p.z*scale) + offset)*exponent_array[i];
            result += weight*signal;
            weight *= signal;
            p.multLocal(lacunarity);
        }
        
        float remainder = octaves - (int)octaves;
        if(octaves > 0){
            result += remainder*(this.basis.value(p.x*scale, p.y*scale, p.z*scale) + offset)*exponent_array[(int)octaves - 1];
        }
        return result/scale;
    }

    public float getH() {
        return H;
    }
    /**
     * Set the H value. This value changes the "roughness" of the heightfield.
     * Default is 0.25.
     * @param H 
     */
    public void setH(float H) {
        this.H = H;
        calcExponentArray();
    }

    public float getLacunarity() {
        return lacunarity;
    }

    /**
     * Lacunarity changes the "hetrogenity" of the fractal, and should
     * generally not be changed. Every time the fractal is repeated
     * (once for each octave), the sample point is multiplied by the
     * lacunarity value. Default value is 2.12...
     * 
     * @param lacunarity 
     */
    public void setLacunarity(float lacunarity) {
        this.lacunarity = lacunarity;
        calcExponentArray();
    }

    public float getOctaves() {
        return octaves;
    }

    /**
     * Octaves is the number of repetitions of the fractal. The more repetitions
     * the more detailed the heightfield becomes - up to a point. Default value is 8.
     * @param octaves 
     */
    public void setOctaves(float octaves) {
        this.octaves = octaves;
        calcExponentArray();
    }

//    public float getMaxOffset() {
//        return maxOffset;
//    }
//
//    public void setMaxOffset(float maxOffset) {
//        this.maxOffset = maxOffset;
//    }
//
//    public float getMinOffset() {
//        return minOffset;
//    }
//
//    public void setMinOffset(float minOffset) {
//        this.minOffset = minOffset;
//    }

    public float getOffset() {
        return offset;
    }
    /**
     * The offset affects noise values directly. A higher value increases the 
     * noise input value, which in turn increase the value of the fractal, including
     * amplitude and frequency. Mountains have a high offset value (around 1),
     * whereas sea level terrain has a lower one (near 0). Default is 0.6, and
     * the default noise input value ranges between -1 and 1.
     * @param offset 
     */
    public void setOffset(float offset) {
        this.offset = offset;
    }

    public float getSeed() {
        return seed;
    }

    public void setSeed(float seed) {
        this.seed = seed;
    }
    
    protected void calcExponentArray(){
        float frequency = 1;
        exponent_array = new float[(int)octaves + 1];
        for(int i = 0; i < octaves; i++){
            exponent_array[i] = FastMath.pow(frequency, -H);
            frequency *= lacunarity;
        }
    }

    @Override
    public void init() {
    }
}
