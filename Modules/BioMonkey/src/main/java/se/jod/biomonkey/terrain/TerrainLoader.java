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

import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.MaterialSP;
import se.jod.biomonkey.biomes.Biotope;
import se.jod.biomonkey.paging.BlockListener;
import se.jod.biomonkey.paging.GeometryPageLoader;
import se.jod.biomonkey.paging.LoadTask;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.terrain.datagrids.TerrainMapBlock;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid;
import se.jod.biomonkey.terrain.datagrids.TerrainMapProvider;

/**
 * A pageloader for terrains.
 *
 * @author Andreas
 */
public class TerrainLoader extends GeometryPageLoader {

    protected TerrainMapProvider mapProvider;
    protected ShadowMode shadowMode = ShadowMode.Off;
    protected int unitsPerVert;
    protected Vector3f localScale = new Vector3f(1, 1, 1);
    protected boolean useCollisionShapes = false;

    public TerrainLoader(int pageSize, float viewingRange, Node node, Camera camera) {
        super(pageSize, 1, viewingRange, node, camera);
//        ((ActivePagingManager)pagingManager).addListener(this.node);
//        pagingManager.setPagingNode(this.node);
//        node.attachChild(this.node);
        mapProvider = new TerrainMapGrid();
        pagingManager.addDetailLevel(viewingRange, 0);

        init();
    }

    private void init() {
        pagingManager.setPageLoader(this);
//        node.initGrid((ActivePagingManager)this.pagingManager);
    }

    @Override
    public Callable<Boolean> loadPage(Page page) {
        page.setPageVersion(PV);
        TerrainPage gPage = (TerrainPage) page;
        TerrainLoadTask tlt = new TerrainLoadTask();
        tlt.setPage(gPage);
        return tlt;
    }

    @Override
    public TerrainPage createPage(int x, int z) {
        Logger.getLogger(TerrainLoader.class.getName()).log(Level.INFO, "TerrainPage created at: ({0},{1})", new Object[]{x, z});

        return new TerrainPage(x, z, pagingManager);
    }

    public TerrainMapProvider getMapProvider() {
        return mapProvider;
    }

    public void setMapProvider(TerrainMapProvider mapProvider) {
        this.mapProvider = mapProvider;
        incrementPageVersion();
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
        incrementPageVersion();
    }

    public Vector3f getLocalScale() {
        return localScale;
    }

    public void setUseCollisionShapes(boolean useCollisionShapes) {
        if (useCollisionShapes) {
            if (EcoManager.getInstance().isPhysicsEnabled() == true) {
                this.useCollisionShapes = true;
            } else {
                throw new RuntimeException("Physics is not enabled, terrain collision shapes can not be enabled.");
            }
        } else {
            this.useCollisionShapes = false;
        }
    }

    public boolean isUseCollisionShapes() {
        return this.useCollisionShapes;
    }

    public void setUnitsPerVert(int units) {
        this.unitsPerVert = units;
        this.localScale = new Vector3f(units, 1, units);
        incrementPageVersion();
    }

    public float getTerrainHeight(float x, float z) {
        TerrainPage page = (TerrainPage) pagingManager.getGridCell(x, z);
        if (page != null) {
            Vector2f xz = new Vector2f(x, z);
            return page.getTerrainHeight(xz);
        }
        return 0;
    }

    public Vector3f getTerrainNormal(float x, float z) {
        TerrainPage page = (TerrainPage) pagingManager.getGridCell(x, z);
        short pageSize = pagingManager.getPageSize();
        //TODO fix this when the terrain normal getter is working.
        float mod = pageSize * 0.5f - 3f;
        if (page != null) {
            Vector2f xz = new Vector2f(x, z);
            xz.x = FastMath.clamp(xz.x, -mod + page.getX() * pageSize, mod + page.getX() * pageSize);
            xz.y = FastMath.clamp(xz.y, -mod + page.getZ() * pageSize, mod + page.getZ() * pageSize);
            return page.getTerrainNormal(xz);
        }
        return Vector3f.ZERO;
    }

    public float[] getBiotopeDensities(float x, float z) {
        TerrainPage page = (TerrainPage) pagingManager.getGridCell(x, z);

        if (page != null) {
            float xx = x - pagingManager.getPageSize() * (page.getX() - 0.5f);
            float zz = z - pagingManager.getPageSize() * (page.getZ() - 0.5f);
            return page.getBiotopeDensities(xx, zz);
        }
        return null;
    }

    public float getSoilMoisture(float x, float z) {
        TerrainPage page = (TerrainPage) pagingManager.getGridCell(x, z);
        if (page != null) {
            float xx = x - pagingManager.getPageSize() * (page.getX() - 0.5f) - 1;
            float zz = z - pagingManager.getPageSize() * (page.getZ() - 0.5f) - 1;
            return page.getSoilMoisture(xx, zz);
        }
        return 0;
    }

    public float getSoilRoughness(float x, float z) {
        TerrainPage page = (TerrainPage) pagingManager.getGridCell(x, z);
        if (page != null) {
            float xx = x - pagingManager.getPageSize() * (page.getX() - 0.5f) - 1;
            float zz = z - pagingManager.getPageSize() * (page.getZ() - 0.5f) - 1;
            return page.getSoilRoughness(xx, zz);
        }
        return 0;
    }

    protected class TerrainLoadTask extends LoadTask<TerrainPage> {

        @Override
        protected boolean generate() {
            //Get the density and colormaps.
            TerrainMapBlock tmb = mapProvider.getMaps(page);
            if (tmb == null) {
                return false;
            }

            page.setMaps(tmb);
            float[] heightVals;

            if (tmb.getHeightMap() != null) {
                heightVals = tmb.getHeightMap().getArray();
            } else {
                return false;
            }

            Texture[] alphaMaps;
            if (tmb.getAlphaMaps() != null) {
                alphaMaps = tmb.getAlphaMaps();
            } else {
                return false;
            }

            // Get the indices and texture data. This is used to
            // add color textures to the materials.
            byte[] indices = tmb.getTextureIndices();
            List<TerrainTextureData> ttds = Biotope.getTTDS();

            //Creates an empty block to store the terrain in.
            page.createBlocks();
            TerrainBlock block = (TerrainBlock) page.getBlock(0);
            for (BlockListener bl : blockListeners) {
                block.addBlockListener(bl);
            }
//            block.addBlockListener(node);
            // CREATE HEIGHTMAP
            FloatHeightMap heightMap = new FloatHeightMap(heightVals);

            heightMap.load();
            heightMap.scale(1f / unitsPerVert);

            //Creates a terrain quad
            Node[] nodes = new Node[1];
            TerrainQuad tq = new TerrainQuad("TerrainQuad" + page.hashCode(), 64, getPagingManager().getPageSize() / unitsPerVert + 1, heightMap.getHeightMap());
            MaterialSP mat = new MaterialSP(EcoManager.getInstance().getApp().getAssetManager(), "se/jod/biomonkey/assets/matdefs/TerrainBaseSplat.j3md");
            mat.setBoolean("useTriPlanarMapping", false);
            // Add all diffuse and normal textures etc. to the material.
            addTextures(ttds, indices, mat);

            mat.setTexture("AlphaMap", alphaMaps[0]);
            if (alphaMaps.length > 1) {
                mat.setTexture("AlphaMap_1", alphaMaps[1]);
                if (alphaMaps.length > 2) {
                    mat.setTexture("AlphaMap_2", alphaMaps[2]);
                }
            }
            EcoManager.getInstance().getAtmosphereManager().getFogManager().addMaterial(mat);
            tq.setMaterial(mat);
            tq.scale(unitsPerVert);

            if (useCollisionShapes) {
                RigidBodyControl control = new RigidBodyControl(new HeightfieldCollisionShape(tq.getHeightMap(), tq.getLocalScale()), 0);
                tq.addControl(control);
                control.setEnabled(false);
            }

//            TerrainLodControl control = new TerrainLodControl(tq, EcoManager.getInstance().getCamera());
//            control.setLodCalculator( new DistanceLodCalculator(65, 1.7f) ); // patch size, and a multiplier
//            tq.addControl(control);
//            tq.recalculateAllNormals();
            nodes[0] = tq;
            nodes[0].setShadowMode(shadowMode);
            block.setNodes(nodes);
            // pageSize / sqrt(2). The radius of the smallest circle
            // enclosing the terrain.
            block.setRealMax(pagingManager.getPageSize() * 0.707107f);

            return true;
        } // generate

        protected void addTextures(List<TerrainTextureData> ttds, byte[] indices, Material mat) {

            for (int i = 0; i < indices.length; i++) {
                int idx = indices[i];
                TerrainTextureData ttd = ttds.get(idx);
                // use texture "indices[i]" at slot i.
                if (i == 0) {
                    mat.setTexture("DiffuseMap", ttd.getGroundTexture());
                    if (ttd.isUseNormalTexture()) {
                        mat.setTexture("NormalMap", ttd.getNormalTexture());
                    }
                } else {
                    mat.setTexture("DiffuseMap_" + i, ttd.getGroundTexture());
                    if (ttd.isUseNormalTexture()) {
                        mat.setTexture("NormalMap_" + i, ttd.getNormalTexture());
                    }
                }
                // Same format whether or not i = 0.
                mat.setFloat("DiffuseMap_" + i + "_scale", ttd.getTextureScale());
            }
        }
    }//LoadTask

    public class FloatHeightMap extends AbstractHeightMap {

        public FloatHeightMap(float[] heights) {
            this.heightData = heights;
        }

        public void scale(float scale) {
            for (int i = 0; i < heightData.length; i++) {
                heightData[i] *= scale;
            }
        }

        @Override
        public boolean load() {
            return true;
        }
    }
}
