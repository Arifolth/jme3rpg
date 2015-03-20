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

import com.jme3.math.Vector2f;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.RectBounds;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.random.FastRandom;
import se.jod.biomonkey.random.PoissonDisk2D;
import se.jod.biomonkey.random.PoissonDisk2D.Vec2List;

/**
 * Planting algorithm based on poisson disk sampling. Objects becomes
 * more evenly spaced then with random placement, but the method is a lot
 * slower.
 * 
 * @author Andreas
 */
public class PAPoisson extends PAUniform {

    protected PoissonDisk2D pd2d;
    protected int rejectionLimit = 30;
    protected float minDist;

    public PAPoisson() {
        pd2d = new PoissonDisk2D();
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

        float mD;
        if(minDist != 0){
            mD = minDist;
        } else {
            mD = 5;
        }

        pd2d.generate(0, 0, width, width, mD, rejectionLimit);
        Vec2List points = pd2d.getPoints();
        //Iterator
        int iIt = 0;
        float[] data = new float[objectCount * 5];
        for (int i = 0; i < Math.min(objectCount, points.size()); i++) {

            float d = 0;
            Vector2f p = points.get(i);
            float x = p.x;
            float z = p.y;
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

    public int getRejectionLimit() {
        return rejectionLimit;
    }

    /**
     * The rejection limit is the maximum number of times the algorithm will
     * run, default is 30.
     * @param rejectionLimit 
     */
    public void setRejectionLimit(int rejectionLimit) {
        this.rejectionLimit = rejectionLimit;
    }

    public float getMinDist() {
        return minDist;
    }

    /**
     * Minimum distance between objects. 
     * @param minDist 
     */
    public void setMinDist(float minDist) {
        this.minDist = minDist;
    }
}
