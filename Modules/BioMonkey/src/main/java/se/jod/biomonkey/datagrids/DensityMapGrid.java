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
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;

/**
 * This class supplies the pageloader with planting data generated from biotope-maps.
 * The biotope maps are added to a grid that is kept and maintained internally, 
 * unlike the terrain-based version that delegates the handling of biotope maps 
 * to a terrain data provider.
 *
 * @author Andreas
 */
public class DensityMapGrid implements DataProvider {

    @Override
    public boolean hasContents(Page page) {
         BiotopeMap bm = grid.get(page.hashCode()).getBiotopeMap();
         if(bm == null){
             return false;
         }
         return true;
    }

    protected Grid2D<MapCell> grid;

    public DensityMapGrid() {
        grid = new Grid2D<MapCell>();
    }

    @Override
    public float[] getData(GeometryPage page, GeometryBlock block, GeometryLayer layer) {

        BiotopeMap bm = grid.get(page.hashCode()).getBiotopeMap();
        float[] data = layer.getPlantingAlgorithm().generateData(page, block, layer, bm);
        return data;
    }

    /**
     * Add a biotopemap.
     *
     * @param bm The biotope map.
     * @param x The page x-index.
     * @param z The page z-index.
     */
    public void addBiotopeMap(BiotopeMap bm, int x, int z) {
        loadMapCell(x, z).addBiotopeMap(bm);
    }

    protected MapCell loadMapCell(int x, int z) {
        MapCell mapCell = grid.getCell(x, z);
        if (mapCell == null) {
            mapCell = new MapCell(x, z);
            grid.put(mapCell.hashCode(), mapCell);
        }
        return mapCell;
    }

    /**
     * This class is used to store densitymaps.
     */
    protected class MapCell extends GenericCell2D {

        BiotopeMap map;

        protected MapCell(int x, int z) {
            super(x, z);
        }

        public void addBiotopeMap(BiotopeMap bm) {
            this.map = bm;
        }
        
        public BiotopeMap getBiotopeMap(){
            return map;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GenericCell2D other = (GenericCell2D) obj;
            if (this.hash != other.hashCode()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "MapCell: (" + Short.toString(x) + ',' + Short.toString(z) + ')';
        }
    }
}
