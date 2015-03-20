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

import com.jme3.math.Vector3f;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.trees.TreeLayer;


/**
 * A tile-loader implementation for adding & loading trees. It uses a list of
 * tree-data to generate the planting data list.
 * 
 * @author Andreas
 */
public class TreeBlockDataProvider implements DataProvider {
    
    protected Grid2D<TreeCell> grid;
    protected int pageSize;
    protected int resolution;
    protected float blockSize;

    // TODO: Look this class over.
    public TreeBlockDataProvider(){
    }
    
    public TreeBlockDataProvider(int pageSize, int resolution) 
    {
        this.pageSize = pageSize;
        this.resolution = resolution;
        this.blockSize = pageSize/(float)resolution;
        grid = new Grid2D<TreeCell>();
    }

    /**
     * This method is very useful when generating blocks of tree 
     * data manually, instead of generating tree data over entire pages, or
     * worlds.
     * 
     * When you generate tree data over an arbitrary area, the treegrid has
     * to parse each data post and place it in its proper block based
     * on its coordinates. If you do this manually, there's no need for
     * that sorting.
     * 
     * @param layer The tree layer.
     * @param list The properly formated list.
     * @param x The x-coordinate of the page.
     * @param z The z-coordinate of the page.
     * 
     */
    public void addTrees(TreeLayer layer, Grid2D<TreeDataList> list, int x, int z){
        grid.getCell(x, z).dataBlock.put(layer, list);
    }
    
    /**
     * Bulk method for adding tree data.
     * 
     * @param layer
     * @param list 
     */
    public void addTrees(TreeLayer layer, TreeDataList list){
        for(TreeData data : list){
            addTree(layer,data);
        }
    }
    
    public void addTree(TreeLayer layer, TreeData data) {
        getTreeCell(data).addData(layer, data);
    }
    
    public void removeTree(TreeLayer layer, TreeData data){
        getTreeCell(data).removeData(layer,data);
    }
    
    public TreeCell getTreeCell(TreeData data){
        //Normalize coordinates to the tree grid.
        int t = (data.x >= 0) ? 1 : -1;
        float xx = data.x / (float) pageSize + t * 0.5f;
        t = (data.z >= 0) ? 1 : -1;
        float zz = data.z / (float) pageSize + t * 0.5f;
        TreeCell cell = grid.getCell((int)xx, (int)zz);
        
        if (cell == null) {
            cell = new TreeCell((int) xx, (int) zz);
            grid.put(cell.hashCode(),cell);
        }
        
        return cell;
    }

    @Override
    public float[] getData(GeometryPage page, GeometryBlock block, GeometryLayer layer) {
        TreeCell cell = grid.getCell(page.hashCode());
        if(cell == null){
            return null;
        }
        int size = cell.dataBlock.size();
        float[] data = new float[5*size];
        int it = 0;
        for(TreeData d : cell.dataBlock.get(layer).getCell(block)){
            data[it++] = d.x;
            data[it++] = d.y;
            data[it++] = d.z;
            data[it++] = d.scale;
            data[it++] = d.rot;
        }
        return data;
    }

    @Override
    public boolean hasContents(Page page) {
        return (this.grid.get(page.hashCode()) != null);
    }

    //Used for creating a grid of tree-pages.
    protected class TreeCell extends GenericCell2D {

        protected TreeDataBlock dataBlock;
        protected Vector3f centerPoint;

        public TreeCell(int x, int z) {
            super(x, z);
            this.centerPoint = new Vector3f(x*pageSize,0,z*pageSize);
            dataBlock = new TreeDataBlock();
        }
        
        protected boolean addData(TreeLayer layer, TreeData data) {
            Grid2D<TreeDataList> dataGrid;
            //Get the correct dataList (the list of tree-data corresponding
            //to the given spatial).
            if (!dataBlock.containsKey(layer)) {
                dataGrid = new Grid2D<TreeDataList>(resolution,resolution);
                
                for(int j = 0; j < resolution; j++){
                    for(int i = 0; i < resolution; i++){
                        dataGrid.put(Grid2D.hash(i, j),new TreeDataList(i,j));
                    }
                }
                dataBlock.put(layer, dataGrid);
                
            }
            
            return getDataList(layer,data).add(data);
        }
        
        protected boolean removeData(TreeLayer layer, TreeData data){
            
            return dataBlock.get(layer).get(0).remove(data);
        }
        
        /*
         * Get data. Note it alters the tree data. Should not be used
         * for anything other then adding/removing tree-data from the
         * lists.
         */
        protected TreeDataList getDataList(TreeLayer layer,TreeData data){
            //Align with page. xz-positions is now in the range 0->pageSize.
            data.x -= centerPoint.x - pageSize*0.5f;
            data.z -= centerPoint.z - pageSize*0.5f;
            //Find the proper block indices based on the coordinates.
            int xx = (int) (data.x/blockSize);
            int zz = (int) (data.z/blockSize);
            
            //Align the coordinates with block.
            data.x -= (xx + 0.5f)*blockSize;
            data.z -= (zz + 0.5f)*blockSize;
            //Tree coordinates are now relative to the center of 
            //their corresponding block.
            int packed = (int)xx + resolution*(int)zz;
            //Get the appropriate list.
            return dataBlock.get(layer).get(packed);
        }

        public Vector3f getCenterPoint() {
            return centerPoint;
        }

        public TreeDataBlock getDataBlock() {
            return dataBlock;
        }
        
    }//TreeList
    
}//TreeGrid
