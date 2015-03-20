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
package se.jod.biomonkey.datagrids;

import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.terrain.datagrids.TerrainMapProvider;

/**
 * TerrainBasedDensityMapGrid uses a terrain loader for biotope maps, then
 * a standard planting algorithm to generate data.
 * 
 * @author Andreas
 */
public class TerrainBasedDensityMapGrid implements DataProvider {
    
    protected TerrainMapProvider tmp;
    
    public TerrainBasedDensityMapGrid(TerrainMapProvider tmp) {
        this.tmp = tmp;
    }
    
    @Override
    public boolean hasContents(Page page) {
        BiotopeMap bm = tmp.getMaps(page).getBiotopeMap();
        if(bm == null){
             return false;
         }
         return true;
    }
    
    @Override
    public float[] getData(GeometryPage page, GeometryBlock block, GeometryLayer layer) {

        BiotopeMap bm = tmp.getMaps(page).getBiotopeMap();
        float[] data = layer.getPlantingAlgorithm().generateData(page, block, layer, bm);
        return data;
    }
    
}
