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
package se.jod.biomonkey.terrain;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.ArrayList;
import se.jod.biomonkey.paging.DetailLevel;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.PagingManager;
import se.jod.biomonkey.terrain.datagrids.TerrainMapBlock;

/**
 * A terrain page object.
 * 
 * @author Andreas
 */
public class TerrainPage extends GeometryPage {

    protected TerrainMapBlock maps;

    public TerrainPage() {
        super();
    }

    public TerrainPage(int x, int z, PagingManager manager) {
        super(x, z, manager);
    }

    @Override
    public void createBlocks() {
        blocks = new ArrayList<Block>(1);

        Vector3f center = new Vector3f(x * pageSize, 0, z * pageSize);
        TerrainBlock block = createBlock(0, 0, center, manager);
        blocks.add(block);
    }

    @Override
    public void process(Vector3f camPos) {

        for (Block b : blocks) {
            // If there are no nodes, continue to the next block.
            // TODO this is impossible, remove.
            if (b.getNodes() == null) {
                continue;
            }

            // If the node is created, but there is no geometry, continue.
            if (b.getNode(0).getChildren().isEmpty()) {
                continue;
            }

            DetailLevel lvl = manager.getDetailLevels().get(0);
            boolean vis = false;
            //Get the distance to the page center.
            float dx = b.getCenterPoint().x - camPos.x;
            float dz = b.getCenterPoint().z - camPos.z;
            float dist = dx * dx + dz * dz;
            // If any part of the block is within the far viewing range.
            if (dist < FastMath.sqr(lvl.getFarDist() + b.getRealMax())) {
                vis = true;
            }
            b.setVisible(vis,0);
            continue;
        }//Block loop

    }//Process method

    @Override
    public TerrainBlock createBlock(int x, int y, Vector3f center, PagingManager manager) {
        return new TerrainBlock(x, y, center, manager,this);
    }

    public TerrainMapBlock getMaps() {
        return maps;
    }

    public void setMaps(TerrainMapBlock maps) {
        this.maps = maps;
    }

    public float[] getBiotopeDensities(float x, float z) {
        if (maps != null) {
            return maps.getBiotopeMap().getDensities(x, z);
        }
        return null;
    }

    public float getTerrainHeight(Vector2f xz) {
        if (loaded) {
            return ((TerrainQuad) blocks.get(0).getNode(0)).getHeight(xz);
        }
        return 0;
    }
    
    public Vector3f getTerrainNormal(Vector2f xz) {
        if (loaded) {
            Vector3f normal = ((TerrainQuad) blocks.get(0).getNode(0)).getNormal(xz);
            return normal;
        }
        return Vector3f.ZERO;
    }

    public float getSoilMoisture(float x, float z) {
        if (maps != null && maps.getSoilMoistureMap() != null) {
            return maps.getSoilMoistureMap().getValueUnfiltered(x,z);
        }
        return 0;
    }
    
    public float getSoilRoughness(float x, float z) {
        if (maps != null && maps.getSoilRoughnessMap() != null) {
            return maps.getSoilRoughnessMap().getValueUnfiltered(x,z);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "TerrainPage(" + Short.toString(x) + ',' + Short.toString(z) + ')';
    }

    @Override
    public void unload() {
        super.unload();
        maps = null;
    }
    
    
}
