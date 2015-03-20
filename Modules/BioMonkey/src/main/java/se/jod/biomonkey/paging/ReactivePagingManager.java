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

import com.jme3.scene.Node;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;

/**
 * A reactive paging manager handles pages, but delegates the loading/unloading
 * to an active paging manager.
 * @author Andreas
 */
public class ReactivePagingManager extends AbstractPagingManager implements PagingListener {

    ActivePagingManager apm;

    public ReactivePagingManager(ActivePagingManager apm, int resolution, Node pagingNode) {
        super(apm.getPageSize(), resolution, pagingNode);
        this.apm = apm;
        apm.addListener(this);

    }

    @Override
    protected void initGrid() {
        gridSize = apm.gridSize;
        // Clean out the old grid if it's not null and non-empty.
        if(grid != null && !grid.isEmpty()){
            for(Page page : grid.values()){
                page.unload();
            }
        }
        grid = new Grid2D<Page>(gridSize, gridSize);
        log.log(Level.INFO, "Grid created (number of pages: {0}).", gridSize * gridSize);
        setVisible(true);
    }

    @Override
    public void update(float tpf) {

        for (Page page : grid.values()) {
            
            if (page == null) {
                continue;
            }

            if (!page.isLoaded() && !page.isIdle() && !page.isPending()) {
                Callable<Boolean> task = pageLoader.loadPage(page);
                Future<Boolean> future = EcoManager.getInstance().getExecutor().submit(task);
                page.setFuture(future);
                page.setPending(true);
                continue;

            } else if (page.isPending()) {
                if (page.getFuture().isDone()) {
                    try {
                        boolean result = page.getFuture().get();
                        if (result == true) {
                            page.setLoaded(true);
                        } else {
                            page.setIdle(true);
                        }
                        page.setPending(false);
                        page.setFuture(null);
                    } catch (InterruptedException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    } catch (ExecutionException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    }

                }
            } else if (page.isLoaded()) {
                //If the page is loaded, update and process it every frame.
                if (updatePages) {
                    page.update(tpf);
                }
                if (visible) {
                    page.process(apm.getCamPos());
                }
            }
        }

        //If the cache is being used.
        if (useCache) {
            for (Page page : cache.values()) {
                if (page.getCacheTimer() >= cacheTime) {
                    cache.remove(page.hashCode());
                    page.unload();
                    log.log(Level.INFO, "Page removed from cache: {0}", page.toString());
                } else {
                    page.increaseCacheTimer(tpf);
                }
            }
        }
    }

    @Override
    public void notifyLoaded(Page inPage, int x, int z) {

        Page page;

        if (useCache) {
            //Browse the cache to see if the page is there before
            //creating a new one
            page = cache.removeCell(x, z);

            if (page == null) {
                Page newTile = pageLoader.createPage(x, z);
                page = grid.setCell(x, z, newTile);
                if (page != null) {
                    cache.put(page.hashCode(), page);
                }
            } else {
                Page oldTile = grid.setCell(x, z, page);
                log.log(Level.INFO, "Tile recycled from cache at: {0}", page.toString());
                if(oldTile != null){
                cache.put(oldTile.hashCode(),oldTile);
                oldTile.resetCacheTimer();
                }
            }
        } else {
            //Just create a new page and loose the old one, if any.
            Page newTile = pageLoader.createPage(x, z);
            Page oldTile = grid.setCell(x, z, newTile);
            if (oldTile != null) {
                oldTile.unload();
            }
        }
    }

    @Override
    public void notifyUnloaded(Page page, int x, int z) {
        Page p = grid.removeCell(x, z);

        if (p != null) {
            if (useCache) {
                cache.put(page.hashCode(), page);
            } else {
                p.unload();
            }
        }
    }

    @Override
    public void notifyGridModified() {
        initGrid();
    }
}
