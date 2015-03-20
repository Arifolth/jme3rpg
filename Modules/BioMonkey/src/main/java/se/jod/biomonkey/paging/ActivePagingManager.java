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

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.grid.Cell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Page;

/**
 * A paging manager with its own paging routine.
 *
 * @author Andreas
 */
public class ActivePagingManager extends AbstractPagingManager {

    protected Camera camera;
    //Temporary variable
    protected Vector3f camPos;
    protected Cell2D currentCell;
    protected float radius;
    protected short halfGridSize;         //(size - 1) / 2
    protected List<PagingListener> listeners;

    public ActivePagingManager(int pageSize, int resolution, float radius, Node rootNode, Camera camera) {
        super(pageSize, resolution, rootNode);
        this.camera = camera;
        this.radius = radius;
        listeners = new ArrayList<PagingListener>();
    }

    @Override
    protected void initGrid() {

        //Limit blocksize min, in case weird values are being used.
        if (blockSize < 32f) {
            log.log(Level.INFO, "Very small block size detected (pageSize: {0}); setting to minimum value: 32f.", pageSize);
            this.resolution = (short) (pageSize / 32f);
            this.blockSize = (pageSize / (float) resolution);
        }

        //Calculate gridsize.
        gridSize = (short) (2 * ((short) (radius / (float) pageSize) + 1) + 1);
        halfGridSize = (short) ((gridSize - 1) / 2);

        // Unload the contents of the grid if it's not null, and notify listeners.
        if (grid != null && !grid.isEmpty()) {
            for (Page page : grid.values()) {
                page.unload();
                if (listeners != null) {
                    for (PagingListener pl : listeners) {
                        pl.notifyUnloaded(page, page.getX(), page.getZ());
                    }
                }
            }
        }

        //Create a new grid.
        grid = new Grid2D<Page>(gridSize, gridSize);
        log.log(Level.INFO, "Grid created (number of pages: {0}).", gridSize * gridSize);

        camPos = camera.getLocation();
        Cell2D camCell = createGridCell(camPos);
        currentCell = camCell;

        for (int k = -halfGridSize; k <= halfGridSize; k++) {
            for (int i = -halfGridSize; i <= halfGridSize; i++) {
                int x = i + camCell.getX();
                int z = k + camCell.getZ();
                Page page = pageLoader.createPage(x, z);
                grid.put(page.hashCode(), page);
            }
        }
        setVisible(true);
    }

    @Override
    public void update(float tpf) {
        camPos = camera.getLocation();
        Cell2D camCell = createGridCell(camPos);

        //Check if the grid should be scrolled.
        if (camCell.hashCode() != currentCell.hashCode()) {
            int x = camCell.getX() - currentCell.getX();
            int z = camCell.getZ() - currentCell.getZ();
            if (x > 1 || x < -1 || z > 1 || z < -1) {
                displaceGrid(camCell, x, z);
            } else {
                scrollGrid(camCell);
            }
        }

        for (Page page : grid.values()) {
            if (page == null) {
                //DEBUG
                throw new RuntimeException(page.toString() + " is null");
            }

            if (!page.isLoaded() && !page.isIdle() && !page.isPending()) {
                Callable<Boolean> task = pageLoader.loadPage(page);

                Future<Boolean> future = EcoManager.getInstance().getExecutor().submit(task);
                page.setFuture(future);
                page.setPending(true);
                continue;

            } else if (page.isPending()) {
                // If the loading is done, try finalizing.
                if (page.getFuture().isDone()) {
                    try {
                        boolean result = page.getFuture().get();
                        if (result == true) {
                            // Check if the page data is up to date. If not, replace
                            // it with a new one.
                            if (page.getPageVersion() == pageLoader.getPageVersion()) {
                                page.setLoaded(true);
                                for (PagingListener l : listeners) {
                                    l.notifyLoaded(page, page.getX(), page.getZ());
                                }
                            } else {
                                page.unload();
                                pageLoader.loadPage(page);
                            }
                            // If the loader returns false the page has no contents.
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
                    page.process(camPos);
                }
            }
        }

        //If the cache is enabled.
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

    protected void displaceGrid(Cell2D camCell, int x, int z) {

        // Get rid of old pages.
        if(useCache){
            cache.clear();
        }
        
        // Clean up the grid from obsolete pages.
        for (Page page : grid.values()) {
            if (page.getX() < camCell.getX() - halfGridSize
                    || page.getX() > camCell.getX() + halfGridSize
                    || page.getZ() < camCell.getZ() - halfGridSize
                    || page.getZ() > camCell.getZ() + halfGridSize) {
                grid.removeCell(page);
                page.unload();
            }
        }

        // Populate
        for (int k = -halfGridSize; k <= halfGridSize; k++) {
            for (int i = -halfGridSize; i <= halfGridSize; i++) {
                int xx = i + camCell.getX();
                int zz = k + camCell.getZ();
                Page page = grid.getCell(xx, zz);
                if (page == null) {
                    page = pageLoader.createPage(x, z);
                    grid.put(page.hashCode(), page);
                }
            }
        }
        currentCell = camCell;
    }

    /**
     * Internal method.
     *
     * This method is called whenever the camera moves from one grid-cell to
     * another, to move the grid along with the camera.
     */
    protected void scrollGrid(Cell2D camCell) {

        int dX = camCell.getX() - currentCell.getX();
        int dZ = camCell.getZ() - currentCell.getZ();

        Page page;

        if (dX == 1 || dX == -1) {

            int oldX = currentCell.getX() - dX * halfGridSize;
            int newX = oldX + dX * gridSize; // gridSize equals 2 * halfGridSize + 1

            for (int k = -halfGridSize; k <= halfGridSize; k++) {
                int z = k + currentCell.getZ();

                if (useCache) {
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    page = cache.removeCell(newX, z);
                    // TODO make pending a separate case.
                    if (page == null || page.isPending()) {
                        Page newPage = pageLoader.createPage(newX, z);
                        page = grid.setCell(oldX, z, newPage); // push the old page out.
                        cache.put(page.hashCode(), page);
                        page.resetCacheTimer();
                    } else {
                        Page oldTile = grid.setCell(oldX, z, page);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", page.toString());

                        cache.put(oldTile.hashCode(), oldTile);
                        oldTile.resetCacheTimer();
                    }
                } else {
                    //Just create a new page and loose the old one.
                    Page newTile = pageLoader.createPage(newX, z);
                    Page oldTile = grid.setCell(oldX, z, newTile);
                    if (oldTile != null) {
                        oldTile.unload();
                    }
                    for (PagingListener l : listeners) {
                        l.notifyUnloaded(oldTile, oldX, z);
                    }
                }

            }
        }

        if (dZ == 1 || dZ == -1) {
            int oldZ = currentCell.getZ() - dZ * halfGridSize;
            int newZ = oldZ + dZ * gridSize;

            for (int i = -halfGridSize; i <= halfGridSize; i++) {
                //Check to make sure that this page was not checked in the
                //previous loop.
                if ((dX == 1 && i == -halfGridSize) || (dX == -1 && i == halfGridSize)) {
                    continue;
                }
                int x = i + currentCell.getX();
                if (useCache) {
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    page = cache.removeCell(x, newZ);
                    if (page == null || page.isPending()) {
                        Page newTile = pageLoader.createPage(x, newZ);
                        page = grid.setCell(x, oldZ, newTile);
                        cache.put(page.hashCode(), page);
                        page.resetCacheTimer();
                    } else {
                        Page oldTile = grid.setCell(x, oldZ, page);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", page.toString());
                        cache.put(oldTile.hashCode(), oldTile);
                        oldTile.resetCacheTimer();
                    }
                } else {
                    Page newTile = pageLoader.createPage(x, newZ);
                    Page oldTile = grid.setCell(x, oldZ, newTile);
                    if (oldTile != null) {
                        oldTile.unload();
                    }
                    for (PagingListener l : listeners) {
                        l.notifyUnloaded(oldTile, x, oldZ);
                    }
                }

            }
        }
        currentCell = camCell;
    }

    public void addListener(PagingListener l) {
        listeners.add(l);
    }

    public void removeListener(PagingListener l) {
        listeners.remove(l);
    }

    public Vector3f getCamPos() {
        return camPos;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        initGrid();
    }

    public Cell2D getCurrentCell() {
        return currentCell;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        reloadPages();
    }

    @Override
    public void setPageSize(int pageSize) {
        super.setPageSize(pageSize);
        if (grid != null) {
            initGrid();
        }
        for (PagingListener l : listeners) {
            l.notifyGridModified();
        }
    }
}
