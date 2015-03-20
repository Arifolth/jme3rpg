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
package se.jod.biomonkey.biomes;

import com.jme3.scene.Node;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.biomes.generation.TerrainData;
import se.jod.biomonkey.biomes.generation.TerrainGenerator;
import se.jod.biomonkey.biomes.plants.Grass;
import se.jod.biomonkey.biomes.plants.Tree;
import se.jod.biomonkey.grass.GrassLayer;
import se.jod.biomonkey.grass.GrassLoader;
import se.jod.biomonkey.terrain.TerrainLoader;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid;
import se.jod.biomonkey.trees.TreeLayer;
import se.jod.biomonkey.trees.TreeLoader;

/**
 * A BioMonkey world object.
 * 
 * @author Andreas
 */
public class BMWorld {
    
    public enum MetricUnit {Meters("meters", "m", "km"), Feet("feet","ft","miles");
        private String name;
        private String shortName;
        private String aggregateName;
        
        private MetricUnit(String name, String shortName, String aggregateName){
            this.name = name;
            this.shortName = shortName;
            this.aggregateName = aggregateName;
        }

        public String getName() {
            return name;
        }

        public String getShortName() {
            return shortName;
        }
        
        public String getAggregateName() {
            return aggregateName;
        }
    
    } // MetricUnit
    
    protected EcoManager ecoManager;
    
    protected int biomeID = 0;
    
    protected TerrainData terrainData;
    
    protected Set<Biome> biomes;
    
    protected HashMap<Grass,List<Byte>> grassMap;
    protected HashMap<Tree,List<Byte>> treesMap;
    
    protected TerrainLoader terrainLoader;
    
    public BMWorld(EcoManager ecoManager){
        this.ecoManager = ecoManager;
        terrainData = new TerrainData();
        biomes = new HashSet<Biome>();
    }
    
    /**
     * Create a new biome.
     * @param name The name of the biome.
     * @return The biome object.
     */
    public Biome createBiome(String name){
        Biome b = new Biome(name);
        b.setID(biomeID*1024);
        biomes.add(b);
        return b;
    }
    
    /**
     * Generate the world.
     * 
     * @param createNewData If everything should be generated from scratch, set this to true.
     */
    public void generate(boolean createNewData){
        TerrainMapGrid grid;
        if(createNewData){
        TerrainGenerator tg = new TerrainGenerator(this);
        
        // Massive work.
        grid = tg.generateTerrain();
        
        } else {
            grid = new TerrainMapGrid();
        }
        float ff = ecoManager.getCamera().getFrustumFar();
        terrainLoader = new TerrainLoader(terrainData.getRealTileSize(), ff, new Node("TerrainNode"), ecoManager.getCamera());
        terrainLoader.setMapProvider(grid);
        terrainLoader.setUnitsPerVert(terrainData.getUnitsPerVert());
        
        linkGrassAndTrees();
        
    }
    
    protected void linkGrassAndTrees(){
        if(grassMap != null && !grassMap.isEmpty()){
            
            GrassLoader grassLoader = ecoManager.getGrassLoader();
            Iterator<Entry<Grass, List<Byte>>> iterator = grassMap.entrySet().iterator();
            while(iterator.hasNext()){
                Entry<Grass, List<Byte>> next = iterator.next();
                Grass g = next.getKey();
                GrassLayer grassLayer = g.getGrassLayer();
                grassLoader.addLayer(grassLayer);
                List<Byte> biotopes = next.getValue();
                
                if(biotopes.isEmpty()){
                   continue; 
                }
                
                byte[] indices = new byte[biotopes.size()];
                for(int i = 0; i < indices.length; i++){
                    indices[i] = biotopes.get(i);
                }
                grassLayer.setTextureIndices(indices);
            }
        }
        if(treesMap != null && !treesMap.isEmpty()){
            TreeLoader treeLoader = ecoManager.getTreeLoader();
            Iterator<Entry<Tree, List<Byte>>> iterator = treesMap.entrySet().iterator();
            while(iterator.hasNext()){
                Entry<Tree, List<Byte>> next = iterator.next();
                Tree t = next.getKey();
                TreeLayer treeLayer = t.getTreeLayer();
                treeLoader.addLayer(treeLayer);
                List<Byte> biotopes = next.getValue();
                
                if(biotopes.isEmpty()){
                   continue; 
                }
                
                byte[] indices = new byte[biotopes.size()];
                for(int i = 0; i < indices.length; i++){
                    indices[i] = biotopes.get(i);
                }
                treeLayer.setTextureIndices(indices);
            }
        }
    }
    
    /**
     * Update the world. Run each frame.
     * @param tpf 
     */
    public void update(float tpf){
        terrainLoader.update(tpf);
    }

    public Set<Biome> getBiomes() {
        return biomes;
    }

    /**
     * Get the terrain loader object.
     * @return 
     */
    public TerrainLoader getTerrainLoader() {
        return terrainLoader;
    }
    
    /**
     * Get the terrain data.
     * @return 
     */
    public TerrainData getTerrainData() {
        return terrainData;
    }

    /**
     * Used internally.
     * @return 
     */
    public void setTerrainData(TerrainData terrainData) {
        this.terrainData = terrainData;
    }

    /**
     * Used internally.
     * @return 
     */
    public HashMap<Grass, List<Byte>> getGrassMap() {
        if(grassMap == null){
            grassMap = new HashMap<Grass,List<Byte>>();
        }
        return grassMap;
    }

    /**
     * Used internally.
     * @return 
     */
    public void setGrassMap(HashMap<Grass, List<Byte>> grassMap) {
        this.grassMap = grassMap;
    }

    /**
     * Used internally.
     * @return 
     */
    public HashMap<Tree, List<Byte>> getTreesMap() {
        if(treesMap == null){
            treesMap = new HashMap<Tree,List<Byte>>();
        }
        return treesMap;
    }

    /**
     * Used internally.
     * @return 
     */
    public void setTreesMap(HashMap<Tree, List<Byte>> treesMap) {
        this.treesMap = treesMap;
    }
    
}
