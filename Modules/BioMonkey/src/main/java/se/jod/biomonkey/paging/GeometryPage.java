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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * Base class for geometry pages.
 * 
 * @author Andreas
 */
public class GeometryPage extends GenericCell2D implements Page, Savable {

    protected transient Future<Boolean> future;
    protected transient float cacheTimer = 0;
    protected transient int bumps = 0;
    protected ArrayList<Block> blocks;
    protected short resolution;
    protected short pageSize;
    protected float blockSize;
    protected transient PagingManager manager;
    protected Vector3f centerPoint;
    protected transient boolean loaded;
    protected transient boolean pending;
    protected transient boolean idle;
        
    protected int PV;

    /** Serialization constructor */
    public GeometryPage() {}
    
    public GeometryPage(int x, int z, PagingManager manager) {
        this.x = (short) x;
        this.z = (short) z;
        this.hash = Grid2D.hash(x, z);
        this.manager = manager;
        this.resolution = manager.getResolution();
        this.pageSize = manager.getPageSize();
        this.blockSize = manager.getBlockSize();
        this.centerPoint = new Vector3f(x * manager.getPageSize(), 0, z * manager.getPageSize());
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public Block getBlock(int i) {
        return blocks.get(i);
    }

    @Override
    public ArrayList<Block> getBlocks() {
        return blocks;
    }

    @Override
    public void createBlocks() {
        blocks = new ArrayList<Block>(resolution * resolution);

        Block block;
        for (int j = 0; j < resolution; j++) {
            for (int i = 0; i < resolution; i++) {
                float posX = (i + 0.5f) * blockSize + (x - 0.5f) * pageSize;
                float posZ = (j + 0.5f) * blockSize + (z - 0.5f) * pageSize;
                Vector3f center = new Vector3f(posX, 0, posZ);
                block = createBlock(i, j, center, manager);
                blocks.add(block);
            }
        }
    }

    @Override
    public void unload() {
        if (blocks == null) {
            return;
        }
        //TODO Clean up better..
        if (future != null) {
            future.cancel(false);
        }
        if (blocks != null) {
            for (Block page : blocks) {
                if (page != null) {
                    page.unload();
                }
            }
        }
        blocks = null;
    }

    public float getBlockSize() {
        return blockSize;
    }

    public Vector3f getCenterPoint() {
        return centerPoint;
    }

    public PagingManager getPagingManager() {
        return manager;
    }

    @Override
    public void setPagingManager(PagingManager manager) {
        this.manager = manager;
    }
    
    

    public short getPageSize() {
        return pageSize;
    }

    public short getResolution() {
        return resolution;
    }

    @Override
    public void increaseCacheTimer(float num) {
        cacheTimer = cacheTimer + num/bumps;
    }

    @Override
    public float getCacheTimer() {
        return cacheTimer;
    }

    @Override
    public void resetCacheTimer() {
        cacheTimer = 0;
        bumps++;
    }

    @Override
    public boolean isIdle() {
        return idle;
    }

    @Override
    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public boolean isPending() {
        return pending;
    }

    @Override
    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public Future<Boolean> getFuture() {
        return future;
    }

    @Override
    public void setFuture(Future<Boolean> future) {
        this.future = future;
    }
    
    @Override
    public void process(Vector3f camPos) {
    }

    @Override
    public Block createBlock(int x, int z, Vector3f center, PagingManager manager) {
        return null;
    }

    @Override
    public int getPageVersion() {
        return PV;
    }

    @Override
    public void setPageVersion(int version) {
        this.PV = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeometryPage other = (GeometryPage) obj;
        if (this.hash != other.hash) {
            return false;
        }
        return true;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(blockSize, "blockSize", 0);
        capsule.write(pageSize, "pageSize", 0);
        capsule.write(resolution, "resolution", 0);
        capsule.write(centerPoint, "centerPoint", null);
        capsule.write(PV, "pageVersion", 0);
        capsule.writeSavableArrayList(blocks, "blocks", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        blockSize = capsule.readFloat("blockSize", 0);
        pageSize = (short) capsule.readInt("pageSize", 0);
        resolution = (short) capsule.readInt("resolution", 0);
        centerPoint = (Vector3f) capsule.readSavable("centerPoint", null);
        PV = capsule.readInt("pageVersion", 0);
        List tempList = capsule.readSavableArrayList("blocks", null);
        blocks = new ArrayList<Block>(tempList.size());
        for(Object e : tempList){
            blocks.add((Block)e);
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
