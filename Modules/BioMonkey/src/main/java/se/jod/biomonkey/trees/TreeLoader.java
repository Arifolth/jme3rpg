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
package se.jod.biomonkey.trees;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.datagrids.DataProvider;
import se.jod.biomonkey.datagrids.TerrainBasedDensityMapGrid;
import se.jod.biomonkey.grass.GrassLoader;
import se.jod.biomonkey.paging.ActivePagingManager;
import se.jod.biomonkey.paging.GeometryPageLoader;
import se.jod.biomonkey.paging.LoadTask;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.terrain.TerrainLoader;

/**
 * Pageloader implementation for trees.
 *
 * @author Andreas
 */
public class TreeLoader extends GeometryPageLoader {

    protected ArrayList<TreeLayer> layers;
    protected TreeGeometryGenerator treeGen;
    protected float impostorDistance;
    protected DataProvider dataProvider;
    protected TerrainLoader terrain;
    protected boolean useBinaries = false;
    protected ShadowMode shadowMode = ShadowMode.Off;

    public TreeLoader(int pageSize,
            int resolution,
            float viewingRange,
            float impostorDistance,
            float transitionRange,
            Node parentNode,
            Camera camera,
            ActivePagingManager apm) {
        super(pageSize, resolution, viewingRange, parentNode, camera, apm);
        this.terrain = (TerrainLoader) (apm.getPageLoader());

        layers = new ArrayList<TreeLayer>();
        treeGen = new TreeGeometryGenerator(terrain);
        linkToTerrain(terrain);
        ID = 2000; // To distinguish from other types of layers
        this.impostorDistance = impostorDistance;

        if (impostorDistance == 0) {
            pagingManager.addDetailLevel(viewingRange, 0);
        } else if (impostorDistance > 0) {
            if (transitionRange <= 0) {
                transitionRange = 20;
            }
            pagingManager.addDetailLevel(impostorDistance, transitionRange);
            pagingManager.addDetailLevel(viewingRange, 0);
        } else {
            throw new RuntimeException("ImpostorDistance must be a positive value.");
        }
        init();
    }

    private void init() {
        pagingManager.setPageLoader(this);
    }

    public TreeLayer addLayer(TreeLayer layer) {
        layer.setID(ID++);
        layers.add(layer);
        layer.setPageLoader(this);
        incrementPageVersion();
        return layer;
    }

    @Override
    public TreePage createPage(int x, int z) {
        Logger.getLogger(TreeLoader.class.getName()).log(Level.INFO, "TreePage created at: ({0},{1})", new Object[]{x, z});
        return new TreePage(x, z, pagingManager);
    }

    @Override
    public Callable<Boolean> loadPage(Page page) {
        page.setPageVersion(PV);
        TreePage tPage = (TreePage) page;
        TreeLoadTask tlt = new TreeLoadTask();
        tlt.setPage(tPage);
        return tlt;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
        incrementPageVersion();
    }

    public DataProvider linkToTerrain(TerrainLoader tl) {
        this.dataProvider = new TerrainBasedDensityMapGrid(tl.getMapProvider());
        incrementPageVersion();
        return dataProvider;
    }

    public ArrayList<TreeLayer> getLayers() {
        return layers;
    }

    public TerrainLoader getTerrainLoader() {
        return terrain;
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }

    public boolean isUseBinaries() {
        return useBinaries;
    }

    public void setUseBinaries(boolean useBinaries) {
        this.useBinaries = useBinaries;
    }

    private class TreeLoadTask extends LoadTask<TreePage> {

        @Override
        public boolean generate() {

            if (useBinaries) {
                String textureFolder = EcoManager.getInstance().getTextureFolder();
                String fullName = textureFolder + "Tile_" + page.getX() + "_" + page.getZ() + "/Trees.j3o";

                File data = new File(fullName);
                if (data.exists()) {
                    TreePage tp = null;
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(data);
                        BinaryImporter imp = new BinaryImporter();
                        AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
                        imp.setAssetManager(assetManager);
                        tp = (TreePage) imp.load(new BufferedInputStream(fis), null, null);
                    } catch (IOException ex) {
                        Logger.getLogger(TreeLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            if (fis != null) {
                                fis.close();
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(TreeLoader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    boolean pv = (tp.getPageVersion() == PV);
                    boolean res = (tp.getResolution() == pagingManager.getResolution());
                    boolean size = (tp.getPageSize() == pagingManager.getPageSize());
                    // If these values all match, update the materials of each geometry,
                    // and finalize the process.
                    if (pv && res && size) {
                        // Set the paging manager to the currently used one.
                        page.createBlocks();
                        for (int j = 0; j < page.getBlocks().size(); j++) {
                            TreeBlock b = (TreeBlock) page.getBlock(j);
                            TreeBlock loadedB = (TreeBlock) tp.getBlock(j);
                            b.setNodes(loadedB.getNodes());
                            if(loadedB.control != null){
                                b.control = loadedB.control;
                                b.initPhysics();
                            }
                            Node node = b.getNode(0);
                            Node node2 = null;
                            if (impostorDistance > 0) {
                                node2 = b.getNode(1);
                            }
                            geomLoop:
                            for (int i = 0; i < node.getChildren().size(); i++) {
                                Geometry geom = (Geometry) node.getChild(i);
                                Material mat = geom.getMaterial();
                                matLookup:
                                for (int k = 0; k < layers.size(); k++) {
                                    for (int l = 0; l < layers.get(k).getModel().getChildren().size(); l++) {
                                        Geometry child = (Geometry) layers.get(k).getModel().getChild(l);
                                        Material m = child.getMaterial();
                                        if (m.getTextureParam("DiffuseMap").getValue().equals(mat.getTextureParam("DiffuseMap").getValue())) {
                                            geom.setMaterial(m);
                                            continue geomLoop;
                                        }
                                    }
                                }
                            }
                            if (node2 != null) {
                                for (int i = 0; i < node2.getChildren().size(); i++) {
                                    Geometry geom = (Geometry) node2.getChild(i);
                                    Material mat = geom.getMaterial();
                                    for (int k = 0; k < layers.size(); k++) {
                                        Material m = layers.get(k).getImpostorMaterial();
                                        if (m.getTextureParam("ImpostorTexture").getValue().equals(mat.getTextureParam("ImpostorTexture").getValue())) {
                                            geom.setMaterial(m);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        return true;
                    } else {
                        throw new RuntimeException("Pages are not up to date, delete old contents or re-set the pageloader values.");
                    }
                }
            }

            if (!dataProvider.hasContents(page)) {
                return false;
            }
            page.createBlocks();

            for (int j = 0; j < page.getBlocks().size(); j++) {

                TreeBlock block = (TreeBlock) page.getBlock(j);

                Node[] nodes = new Node[2];

                Node geomNode = new Node("GeometryNode_" + page.hashCode());
                geomNode.setShadowMode(ShadowMode.Off);

                Node impNode = new Node("ImpostorNode_" + page.hashCode());
                impNode.setShadowMode(ShadowMode.Off);

                CompoundCollisionShape ccs = null;
                if (EcoManager.getInstance().isPhysicsEnabled()) {
                    ccs = new CompoundCollisionShape();
                }

                treeGen.createBatchedGeometry(layers, block, page, dataProvider, ccs, geomNode, impNode);

                geomNode.setShadowMode(shadowMode);
                impNode.setShadowMode(shadowMode);

                nodes[0] = geomNode;
                nodes[1] = impNode;

                block.setNodes(nodes);
                // If ccs is null, no physics.
                block.initPhysics(ccs);
            }//for each block.

            if (useBinaries) {
                String textureFolder = EcoManager.getInstance().getTextureFolder();
                FileOutputStream fos = null;
                try {
                    File dir = new File(textureFolder + "Tile_" + page.getX() + "_" + page.getZ());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File object = new File(textureFolder + "Tile_" + page.getX() + "_" + page.getZ() + "/Trees.j3o");
                    if (object.exists()) {
                        // Should never happen.
                        throw new RuntimeException("ERROR: The grass data for this tile already exists. Delete old contents first. (" + object.getPath() + ")");
                    }
                    fos = new FileOutputStream(object);
                    // we just use the exporter and pass in the terrain
                    BinaryExporter.getInstance().save(page, new BufferedOutputStream(fos));
                    fos.flush();
                } catch (IOException ex) {
                    Logger.getLogger(TreeLoader.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Logger.getLogger(TreeLoader.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }

            return true;
        }
    }//LoadTask
}//TreeLoader
