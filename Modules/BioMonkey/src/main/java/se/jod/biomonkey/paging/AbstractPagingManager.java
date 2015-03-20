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
package se.jod.biomonkey.paging;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.grid.Cell2D;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.paging.interfaces.PageLoader;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * This is the basis for paging managers.
 * 
 * @author Andreas
 */
public abstract class AbstractPagingManager implements PagingManager {
    
    protected static final Logger log = Logger.getLogger(AbstractPagingManager.class.getCanonicalName());
    
    protected Node pagingNode;
    //Used to turn visibility off globally.
    protected boolean visible = true;
    
    protected ArrayList<DetailLevel> detailLevels;
    protected int numDetailLevels;
    protected boolean fadeEnabled = false;
    
    protected PageLoader pageLoader;
    
    
    //The size of a page.
    protected short gridSize;
    protected short pageSize;
    protected short resolution;
    protected float blockSize;
    
    //Grid data
    protected Grid2D<Page> grid;
    protected Grid2D<Page> cache;
    
    protected boolean useCache = false;
    protected float cacheTime = 6f;
    
    protected boolean updatePages = false;
    protected boolean debug = false;
    
    /**
     * Constructor.
     * 
     * @param pageSize The size of pages.
     * @param resolution Each page contains resolution^2 pages.
     * @param pagingNode A node for attaching paged geometry to.
     */
    public AbstractPagingManager(int pageSize, int resolution, Node pagingNode){
        
        this.pageSize = (short) pageSize;
        this.resolution = (short) resolution;
        this.blockSize = (pageSize/(float)resolution);
        this.pagingNode = pagingNode;
        EcoManager.getInstance().getRootNode().attachChild(pagingNode);
        this.visible = true;
        detailLevels = new ArrayList<DetailLevel>();
        
        if(useCache){
            //Start at 5*size, expands if needed.
            cache = new Grid2D<Page>(5,gridSize);
        }
    }
    
    protected abstract void initGrid();
    
    @Override
    public void setPageLoader(PageLoader pageLoader)
    {
        this.pageLoader = pageLoader;
        initGrid();
    }
    
    @Override
    public void reloadPages(Vector3f center, float radius) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void reloadPage(Vector3f loc){
        Cell2D cell = createGridCell(loc);
        reloadPage(cell.getX(),cell.getZ());
    }
    
    @Override
    public void reloadPage(int x, int z){
        Page page = grid.getCell(x, z);
        if(page != null){
            page.unload();
            grid.removeCell(page);
            Page newTile = pageLoader.createPage(x, z);
            grid.put(newTile.hashCode(),newTile);
        }
    }
    
    @Override
    public void reloadPages(){
        for(Page page: grid.values()){
            page.unload();
        }
        grid.clear();
        if(useCache){
            cache.clear();
        }
        initGrid();
    }
    
    @Override
    public void reloadPages(float l, float r, float t, float b){
        Vector3f tl = new Vector3f(l,0,t);
        Vector3f br = new Vector3f(r,0,b);
        Cell2D tlc = createGridCell(tl);
        Cell2D brc = createGridCell(br);
        for(int j = brc.getZ(); j <= tlc.getZ();j++){
            for(int i = tlc.getX(); i <= brc.getX();i++){
                reloadPage(i,j);
            }
        }
    }
    
    /**
     * Create a gridcell with indices based on the provided location.
     * 
     * @param loc The location.
     * @return A generic cell.
     */
    public Cell2D createGridCell(Vector3f loc){
        float x = loc.x;
        float z = loc.z;
        int t = (x >= 0) ? 1 : -1;
        x = x/(float)pageSize + t*0.5f;
        t = (z >= 0) ? 1 : -1;
        z = z/(float)pageSize + t*0.5f;
        return new GenericCell2D((int)x,(int)z);
    }
    
    /**
     * A convenience method for getting cells based on world x and z coordinates.
     * 
     * @param x The world x-coordinate.
     * @param z The world z-coordinate.
     * @return The cell matching the given location.
     */
    public Cell2D createGridCell(float x, float z){
        int t = (x >= 0) ? 1 : -1;
        x = x/(float)pageSize + t*0.5f;
        t = (z >= 0) ? 1 : -1;
        z = z/(float)pageSize + t*0.5f;
        return grid.getCell((int)x, (int)z);
    }
    
    @Override
    public Page getGridCell(Vector3f loc){
        float x = loc.x;
        float z = loc.z;
        int t = (x >= 0) ? 1 : -1;
        x = x/(float)pageSize + t*0.5f;
        t = (z >= 0) ? 1 : -1;
        z = z/(float)pageSize + t*0.5f;
        return grid.getCell((int)x, (int)z);
    }
    
    @Override
    public Page getGridCell(float x, float z){
        int t = (x >= 0) ? 1 : -1;
        int xx = (int) (x/(float)pageSize + t*0.5f);
        t = (z >= 0) ? 1 : -1;
        int zz = (int) (z/(float)pageSize + t*0.5f);
        return grid.getCell(xx,zz);
    }
    
    @Override
    public PageLoader getPageLoader() {
        return pageLoader;
    }

    @Override
    public short getResolution() {
        return resolution;
    }

    @Override
    public short getPageSize() {
        return pageSize;
    }
    
    @Override
    public float getBlockSize() {
        return blockSize;
    }
    
    @Override
    public int getCurrentGridSize(){
        return grid.size();
    }
    
    @Override
    public short getGridSize() {
        return gridSize;
    }

    public float getCacheTime() {
        return cacheTime;
    }

    public boolean isUseCache() {
        return useCache;
    }
    
    @Override
    public void setCacheTime(float cacheTime) {
        if(cacheTime > 60){
            log.log(Level.WARNING,"The cache-time is extremely high, make sure it's correctly typed and measured in seconds.");
        }
        this.cacheTime = cacheTime;
    }

    @Override
    public void setUseCache(boolean useCache) {
        if(useCache == true && this.cache == null){
            cache = new Grid2D<Page>(5,gridSize);
        }
        if(useCache == false && this.cache != null){
            cache = null;
        }
        this.useCache = useCache;
    }    
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        
        if(visible == true && this.visible == false){
            EcoManager.getInstance().getRootNode().attachChild(pagingNode);
            this.visible = visible;
        }
        
        else if(visible == false && this.visible == true){
            pagingNode.removeFromParent();
            this.visible = visible;
        }
    }
    
    @Override
    public Node getPagingNode(){
        return pagingNode;
    }
    
    @Override
    public void setPagingNode(Node pagingNode){
        this.pagingNode = pagingNode;
    }
    
    public void addDetailLevel(float farDist)
    {
        addDetailLevel(farDist,0);
    }
    
    
    @Override
    public void addDetailLevel(float farDist , float fadingRange)
    {
        float nearDist = 0;
        DetailLevel level;
        
        //If a detail level has previously been added, use its far distance as 
        //near distance for the new one.
        if(numDetailLevels != 0){
            level = detailLevels.get(numDetailLevels - 1);
            nearDist = level.farDist;
            if(nearDist >= farDist) {
                throw new RuntimeException("The near viewing distance must be closer then the far viewing distance");
            }
        }
        if(fadingRange > 0){
            this.fadeEnabled = true;
        }
        DetailLevel newLevel = new DetailLevel();
        newLevel.setFarDist(farDist);
        newLevel.setNearDist(nearDist);
        newLevel.setTransition(fadingRange);
        detailLevels.add(newLevel);
        numDetailLevels += 1;
    }
   
    public void removeDetailLevels() 
    {
        detailLevels.clear();
        setVisible(false);
    }
    
    public int getNumDetailLevels() {
        return numDetailLevels;
    }

    @Override
    public ArrayList<DetailLevel> getDetailLevels() {
        return detailLevels;
    }

    @Override
    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    @Override
    public Grid2D<Page> getGrid() {
        return grid;
    }

    @Override
    public void setResolution(int resolution) {
        this.resolution = (short) resolution;
        reloadPages();
    }

    @Override
    public void setPageSize(int pageSize) {
        if(pageSize < 64){
            pageSize = 64;
        }
        this.pageSize = (short) pageSize;
        reloadPages();
    }

    public boolean isUpdatePages() {
        return updatePages;
    }

    public void setUpdatePages(boolean updatePages) {
        this.updatePages = updatePages;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }
    
    @Override
    public void setDebug(boolean debug){
        this.debug = debug;
    }
    
}
