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

import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.paging.interfaces.PageLoader;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * Default PageLoader implementation.
 * 
 * @author Andreas
 */
public abstract class GeometryPageLoader implements PageLoader {

    protected String name;
    protected PagingManager pagingManager;
    protected List<BlockListener> blockListeners;
    
    protected int ID;
    
    protected int PV = Integer.MIN_VALUE;

    /**
     * Creates a new GeometryPageLoader
     * @param pageSize The size of the pages.
     * @param resolution The resolution of each page.
     * @param viewingRange The maximum viewing range.
     * @param pagingNode The node used as paging node.
     * @param camera The scene camera.
     * @param apm The active paging manager. If this value is null, the pageloader
     * will create its own active paging manager.
     */
    public GeometryPageLoader(int pageSize,
            int resolution,
            float viewingRange,
            Node pagingNode,
            Camera camera,
            ActivePagingManager apm) 
    {
        if (apm == null) {
            this.pagingManager = new ActivePagingManager(pageSize, resolution, viewingRange, pagingNode, camera);
        } else {
            this.pagingManager = new ReactivePagingManager(apm,resolution,pagingNode);
        }
        blockListeners = new ArrayList<BlockListener>();
    }
    
    public GeometryPageLoader(  int pageSize, 
                                int resolution, 
                                float viewingRange, 
                                Node rootNode, 
                                Camera camera)
    {
        this.pagingManager = new ActivePagingManager(pageSize, resolution, viewingRange, rootNode, camera);
        blockListeners = new ArrayList<BlockListener>();
    }

    @Override
    public abstract Callable<Boolean> loadPage(Page page);

    @Override
    public void update(float tpf) {
        pagingManager.update(tpf);
    }
    
    /**
     * Add a block listener to the paging manager used by this pageloader.
     * 
     * @param listener 
     */
    public void addBlockListener(BlockListener listener){
        blockListeners.add(listener);
    }
    
    /**
     * Remove a blocklistener.
     * 
     * @param listener 
     */
    public void removeBlockListener(BlockListener listener){
        blockListeners.remove(listener);
    }

    @Override
    public PagingManager getPagingManager() {
        return this.pagingManager;
    }

    /**
     * Turns the visibility of the geometry on and off (attaches/detaches it from
     * the scenegraph).
     * 
     * @param visible 
     */
    public void setVisible(boolean visible) {
        pagingManager.setVisible(visible);
    }

    public int getPageSize() {
        return pagingManager.getPageSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Node getPagingNode() {
        return pagingManager.getPagingNode();
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public void setID(int ID) {
        this.ID = ID;
    }

    @Override
    public int getPageVersion() {
        return PV;
    }

    @Override
    public void incrementPageVersion() {
        if(PV == Integer.MAX_VALUE){
            PV = Integer.MIN_VALUE;
        }
        PV++;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeometryPageLoader other = (GeometryPageLoader) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = this.name != null ? this.name.hashCode() : 0;
        return hash;
    }
}// GeometryPageLoader
