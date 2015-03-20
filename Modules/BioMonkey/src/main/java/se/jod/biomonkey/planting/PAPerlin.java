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
package se.jod.biomonkey.planting;

import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.terrain.noise.modulator.Modulator;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.RectBounds;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.random.FastRandom;

/**
 * Planting algorithm based on a perlin noise rather then pseudo-random values.
 * 
 * @author Andreas
 */
public class PAPerlin extends PAUniform {

    protected ImprovedNoise noise;
    protected boolean invertNoise;

    public PAPerlin() {
        super();
        noise = new ImprovedNoise();
        noise.addModulator(new Modulator() {
            @Override
            public float value(float... in) {
                return in[0] * 0.5f + 0.5f;
            }
        });
        noise.setScale(2);
    }

    public PAPerlin(float threshold) {
        super(threshold);
    }

    /**
     * Set the scale of the noise.
     * @param scale 
     */
    public void setNoiseScale(float scale) {
        noise.setScale(scale);
    }

    public float getNoiseScale() {
        return noise.getScale();
    }

    protected float getNoiseValue(float x, float z, int tileX, int tileZ, int size) {
        return noise.value((tileX + x / size), (tileZ + z / size), 0);
    }

    @Override
    public float[] generateData(GeometryPage page,
            GeometryBlock block,
            GeometryLayer layer,
            BiotopeMap biotopeMap) {

        RectBounds bounds = block.getBounds();
        long seed = EcoManager.getInstance().getRandomTable().lookup(page, block, layer.getID());
        //Populating the array of locations (and also getting the total amount
        //of quads).
        FastRandom rand = new FastRandom(seed);

        //Calculate the area of the page
        float width = bounds.getWidth();
        float area = width * width;

        //This is the grasscount variable. The initial value is the maximum
        //possible count. It may be reduced by densitymaps, height restrictions
        //and other stuff.
        int objectCount = (int) ((double) (area * layer.getDensityMultiplier()));

        byte[] indices = layer.getTextureIndices();

        float offsetX = bounds.getxMin() - page.getCenterPoint().x + page.getPageSize() * 0.5f;
        float offsetZ = bounds.getzMin() - page.getCenterPoint().z + page.getPageSize() * 0.5f;

        //Iterator
        int iIt = 0;
        float[] data = new float[objectCount * 5];
        for (int i = 0; i < objectCount; i++) {

            float d = 0;

            float x = rand.unitRandom() * width;
            float z = rand.unitRandom() * width;
            float xx = x + offsetX;
            float zz = z + offsetZ;

            for (int idx = 0; idx < indices.length; idx++) {
                float dTemp = biotopeMap.getDensityUnfiltered(xx, zz, indices[idx]);
                if (dTemp > d) {
                    d = dTemp;
                }
            }
            if (scaling == Scaling.Quadratic) {
                d *= d;
            } else if (scaling == Scaling.Linear_Inverted) {
                d = 1 - d;
            } else if (scaling == Scaling.Quadratic_Inverted) {
                d = 1 - d * d;
            }

            if (binary) {
                d = (d < threshold) ? 0 : 1;
            }
            
            float noiseVal = this.getNoiseValue(xx, zz, page.getX(), page.getZ(), page.getPageSize());
            
            if(invertNoise){
                noiseVal = 1.0f - noiseVal;
            }

            if (rand.unitRandom() + threshold < d * noiseVal) {
                // Normalize to block center.
                data[iIt++] = x + bounds.getxMin();
                data[iIt++] = 0;
                data[iIt++] = z + bounds.getzMin();
                data[iIt++] = rand.unitRandom();
                // (-pi/2, pi/2]
                data[iIt++] = (-0.5f + rand.unitRandom()) * 3.141593f;
            }

        }
        float[] finished = new float[iIt];
        System.arraycopy(data, 0, finished, 0, iIt);
        return finished;
    }

    public boolean isInvertNoise() {
        return invertNoise;
    }

    public void setInvertNoise(boolean invertNoise) {
        this.invertNoise = invertNoise;
    }

    public ImprovedNoise getNoise() {
        return noise;
    }

    public void setNoise(ImprovedNoise noise) {
        this.noise = noise;
    }
}
