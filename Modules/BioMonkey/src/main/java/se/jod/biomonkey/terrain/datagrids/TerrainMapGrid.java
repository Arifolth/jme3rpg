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
package se.jod.biomonkey.terrain.datagrids;

import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.texture.Texture;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.image.FloatMap;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;

/**
 * A grid of terrain data such as heightmaps and alphamaps.
 *
 * @author Andreas
 */
public class TerrainMapGrid implements TerrainMapProvider {

    protected Grid2D<MapCell> grid;

    public TerrainMapGrid() {
        grid = new Grid2D<MapCell>();
    }

    @Override
    public TerrainMapBlock getMaps(Page page) {
        String textureFolder = EcoManager.getInstance().getTextureFolder();
        String fullName = textureFolder + "Tile_" + page.getX() + "_" + page.getZ() + "/terrainData.j3o";
        
        File data = new File(fullName);
        if (!data.exists()) {
            return null;
        }
        
        TerrainMapBlock tmb = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(data);
            BinaryImporter imp = new BinaryImporter();
            AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
            imp.setAssetManager(assetManager);
            tmb = (TerrainMapBlock) imp.load(new BufferedInputStream(fis), null, null);
        } catch (IOException ex) {
            Logger.getLogger(TerrainMapGrid.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TerrainMapGrid.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return tmb;
    }

    @Override
    public TerrainMapBlock getMaps(int x, int z) {
        MapCell mapCell = grid.getCell(x, z);
        if (mapCell != null) {
            return mapCell.maps;
        }
        return null;
    }

    public Grid2D<MapCell> getGrid() {
        return grid;
    }

    public void setMapBlock(TerrainMapBlock tmb, int x, int z) {
        loadMapCell(x, z).setMapBlock(tmb);
    }

    public void setHeightMap(FloatMap heightMap, int x, int z) {
        loadMapCell(x, z).addHeightMap(heightMap);
    }

    public void setSlopeMap(FloatMap slopeMap, int x, int z) {
        loadMapCell(x, z).addSlopeMap(slopeMap);
    }

    public void setSoilRoughnessMap(FloatMap rougnessMap, int x, int z) {
        loadMapCell(x, z).addSoilRoughnessMap(rougnessMap);
    }

    public void setSoilMoistureMap(FloatMap moistureMap, int x, int z) {
        loadMapCell(x, z).addSoilMoistureMap(moistureMap);
    }

    public void setBiotopeMap(BiotopeMap bm, int x, int z) {
        loadMapCell(x, z).addBiotopeMap(bm);
    }

    public void setAlphaMaps(Texture[] alphaMaps, int x, int z) {
        loadMapCell(x, z).addAlphaMaps(alphaMaps);
    }

    public void setTextureIndices(byte[] textureIndices, int x, int z) {
        loadMapCell(x, z).addTextureIndices(textureIndices);
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
     * This class is used to store density and colormaps.
     */
    public class MapCell extends GenericCell2D {

        TerrainMapBlock maps;

        protected MapCell(int x, int z) {
            super(x, z);
            maps = new TerrainMapBlock();
        }

        protected void setMapBlock(TerrainMapBlock tmb) {
            this.maps = tmb;
        }

        protected void addHeightMap(FloatMap heightMap) {
            maps.setHeightMap(heightMap);
        }

        protected void addSlopeMap(FloatMap slopeMap) {
            maps.setSlopeMap(slopeMap);
        }

        protected void addSoilRoughnessMap(FloatMap roughnessMap) {
            maps.setSoilRoughnessMap(roughnessMap);
        }

        protected void addSoilMoistureMap(FloatMap moistureMap) {
            maps.setSoilMoistureMap(moistureMap);
        }

        protected void addBiotopeMap(BiotopeMap bm) {
            maps.setBiotopeMap(bm);
        }

        protected void addAlphaMaps(Texture[] txs) {
            maps.setAlphaMaps(txs);
        }

        protected void addTextureIndices(byte[] textureIndices) {
            maps.setTextureIndices(textureIndices);
        }

        public TerrainMapBlock getTerrainMapBlock() {
            return maps;
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
    }//MapCell
}
