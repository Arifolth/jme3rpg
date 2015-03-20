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
package se.jod.biomonkey.paging.interfaces;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.concurrent.Future;
import se.jod.biomonkey.paging.grid.Cell2D;

/**
 * Base interface for pages.
 * 
 * @author Andreas
 */
public interface Page extends Cell2D {
    
    /**
     * This method is called every update. It can be used to
     * do visibility and fading calculations, or other things.
     * 
     * @param camPos The position of the camera used by the paging manager.
     */
    public void process(Vector3f camPos);
    
    /**
     * This method could be used for any process related to the page itself.
     * It doesn't have to do anything.
     * 
     * @param tpf Time since last frame in seconds.
     */
    public void update(float tpf);
    
    /**
     * This method is called whenever a page is being removed from the
     * grid.
     */
    public void unload();
    
    /**
     * Set the parent paging manager. NOTE: Only for serialization.
     * 
     * @param manager 
     */
    public void setPagingManager(PagingManager manager);
    
    public PagingManager getPagingManager();
    
    /**
     * Set the future object. It is used internally by the pagingmanager.
     * 
     * @param future
     */
    public void setFuture(Future<Boolean> future);
    
    /**
     * Get the future object. Used internally.
     * 
     * @return The future object.
     */
    public Future<Boolean> getFuture();
    
    //******************* Block management ********************
    
    /**
     * Creates empty blocks. This is normally done during pageloading, before
     * anything else.
     */
    public void createBlocks();
    
    /**
     * Creates a block. Pages are also block-factories.
     * 
     * @param x x-coordinate of the block.
     * @param z z-coordinate of the block.
     * @param center The center of the block in world coordinates.
     * @param manager The paging manager used by the pageloader.
     * @return 
     */
    public Block createBlock(int x, int z, Vector3f center, PagingManager manager);
    
    /**
     * Returns a block from the grid. Normally used when iterating over
     * the entire grid.
     * 
     * @param i Index of the block.
     * @return The block.
     */
    public Block getBlock(int i);
    
    /**
     * Get the list containing all blocks in this page.
     * 
     * @return The blocks.
     */
    public ArrayList<Block> getBlocks();
    
    //*********************** Status **************************
    
    public boolean isIdle();
    public void setIdle(boolean idle);
    public boolean isLoaded();
    public void setLoaded(boolean loaded);
    public boolean isPending();
    public void setPending(boolean pending);
    
    //*********************** Cache **************************
    
    /**
     * @param num Time in seconds.
     */
    public void increaseCacheTimer(float num);
    public float getCacheTimer();
    public void resetCacheTimer();
    
    //************************ Versioning ************************
    public void setPageVersion(int version);
    public int getPageVersion();
}//Tile
