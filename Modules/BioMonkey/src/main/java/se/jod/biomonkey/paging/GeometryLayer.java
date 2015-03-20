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
package se.jod.biomonkey.paging;

import se.jod.biomonkey.planting.PAUniform;
import se.jod.biomonkey.planting.PlantingAlgorithm;

/**
 * GeometryLayers represents a type of geometry. It could be a specific type 
 * of grass, or a type of tree, or anything. Should be extended.
 * 
 * @author Kallsta
 */
public class GeometryLayer {
    
    protected int ID;
    protected GeometryPageLoader pageLoader;
    protected PlantingAlgorithm pa;
    protected float densityMultiplier = 0.1f;
    // Density data
    protected byte[] textureIndices;

    public GeometryLayer() {
        pa = new PAUniform();
    }
    
    public PlantingAlgorithm getPlantingAlgorithm() {
        return pa;
    }

    /**
     * Set the planting algorithm used for this layer. The planting algorithm
     * is used to distribute individual geometries, such as grass patches or
     * trees.
     * 
     * @param plantingAlgorithm 
     */
    public void setPlantingAlgorithm(PlantingAlgorithm plantingAlgorithm) {
        this.pa = plantingAlgorithm;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    /**
     * Set the indices of the biotope map to be used for this grass.
     * @param indices 
     */
    public void setTextureIndices(byte[] indices) {
        this.textureIndices = indices;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public int getID() {
        return ID;
    }

    /**
     * Handled internally.
     * @param ID 
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    public byte[] getTextureIndices() {
        return textureIndices;
    }

    public GeometryPageLoader getPageLoader() {
        return pageLoader;
    }
    
    /**
     * Associates this layer with a geometry page loader. This method should not
     * be called manually.
     * @param pageLoader 
     */
    public void setPageLoader(GeometryPageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    public float getDensityMultiplier() {
        return densityMultiplier;
    }

    /**
     * The density multiplier is used to regulate the maximum object count during
     * planting. If density is 1, the total number of objects defaults to 1 per
     * world unit squared. The maximum object count is calculated for each block
     * by taking "blockWidth*blockHeight*densityMultiplier". The final object count
     * is normally reduced by using density maps, but the value should normally 
     * be much less then 1. Default is 0.1. Trees and other large objects should
     * normally be between 0.01 and 0.001.
     * 
     * @param densityMultiplier The multiplier value.
     */
    public void setDensityMultiplier(float densityMultiplier) {
        this.densityMultiplier = densityMultiplier;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeometryLayer other = (GeometryLayer) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }
    
    
}
