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

import com.jme3.math.FastMath;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.RectBounds;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.random.FastRandom;

/**
 * The default planting algorithm. Uses pseudo-random numbers to determine
 * if objects are planted or not.
 *
 * @author Andreas
 */
public class PAUniform implements PlantingAlgorithm {

    public enum Scaling {

        Linear, Quadratic, Linear_Inverted, Quadratic_Inverted
    }
    protected Scaling scaling = Scaling.Linear;
    protected float threshold = 0;
    protected boolean binary = false;

    public PAUniform() {
    }

    public PAUniform(float threshold) {
        this.threshold = threshold;
    }

    @Override
    public float[] generateData(GeometryPage page,
            GeometryBlock block,
            GeometryLayer layer,
            BiotopeMap biotopeMap
            ) {

        RectBounds bounds = block.getBounds();
        long seed = EcoManager.getInstance().getRandomTable().lookup(page, block, layer.getID());
        //Populating the array of locations (and also getting the total amount
        //of quads).
        FastRandom rand = new FastRandom(seed);

        //Calculate the area of the page
        float width = bounds.getWidth();
        float area = width*width;
        
        //This is the grasscount variable. The initial value is the maximum
        //possible count. It may be reduced by densitymaps, height restrictions
        //and other stuff.
        int objectCount = (int) ((double)(area * layer.getDensityMultiplier()));

        byte[] indices = layer.getTextureIndices();

        float offsetX = bounds.getxMin() - page.getCenterPoint().x + page.getPageSize() * 0.5f;
        float offsetZ = bounds.getzMin() - page.getCenterPoint().z + page.getPageSize() * 0.5f;

        //Iterator
        int iIt = 0;
        float[] data = new float[objectCount*5];
        for (int i = 0; i < objectCount; i++) {

            float d = 0;

            float x = rand.unitRandom() * width;
            float z = rand.unitRandom() * width;
            float xx = x + offsetX;
            float zz = z + offsetZ;

            // Base the random test on highest density value at this point
            // (some grasses wants to grow in several biotopes so they have
            // several values to check).
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

            if (rand.unitRandom() + threshold < d) {
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

    public Scaling getScaling() {
        return scaling;
    }

    /**
     * Method for manipulating density-map values. You can change this to modify
     * the density-values in various ways.
     *
     * @param scaling
     */
    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }

    public float getThreshold() {
        return threshold;
    }

    /**
     * Allows you to restrict planting at areas with density lower then the
     * given threshold value (between 0 and 1). For example, if you set
     * threshold to 0.5, no grass will be planted at points where density < 0.5.
     * When using terrain alphamaps for density, this is a good way to reduce
     * grass-growth in areas where a certain texture is present but has a very
     * low blending value (barely visible).
     */
    public void setThreshold(float threshold) {
        this.threshold = FastMath.clamp(threshold, 0, 1f);
    }

    /**
     * Use binary density values (0 or 1). This should be used in conjunction
     * with the setThreshold-method. If the threshold is set to 0.5 and binary
     * is used, all density-values below 0.5 are set to 0, and all values equal
     * to or larger then 0.5 are set to 1.
     *
     * @param binary True if binary values should be used.
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }
}//GPAUniform
