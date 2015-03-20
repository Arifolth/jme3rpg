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
package se.jod.biomonkey.grass;

import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
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
import se.jod.biomonkey.paging.ActivePagingManager;
import se.jod.biomonkey.paging.GeometryPageLoader;
import se.jod.biomonkey.paging.LoadTask;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.terrain.TerrainLoader;

/**
 * This class is used to create grass.
 *
 * @author Andreas
 */
public class GrassLoader extends GeometryPageLoader {

    protected static final Logger log = Logger.getLogger(GrassLoader.class.getName());
    //List of grass-layers.
    protected ArrayList<GrassLayer> layers;
    protected Vector2f wind = new Vector2f(1, 1);
    protected GrassGeometryGenerator grassGen;
    protected DataProvider dataProvider;
    protected boolean useBinaries = false;
    protected TerrainLoader terrain;
    protected ShadowMode shadowMode = ShadowMode.Off;

    /**
     * The only constructor.
     *
     * @param pageSize The pagesize.
     * @param resolution The resolution of the pages (amount of subdivisions).
     * @param farViewingDistance The far viewing distance for the grass.
     * @param fadingRange The distance over which grass is faded out (in world
     * units).
     * @param parentNode The parentNode.
     * @param camera The camera used for rendering the scene.
     * @param apm The active paging manager used to control the paging.
     */
    public GrassLoader(int pageSize,
            int resolution,
            float farViewingDistance,
            float fadingRange,
            Node parentNode,
            Camera camera,
            ActivePagingManager apm) {
        super(pageSize, resolution, farViewingDistance, parentNode, camera, apm);
        pagingManager.addDetailLevel(farViewingDistance, fadingRange);
        layers = new ArrayList<GrassLayer>();
        terrain = (TerrainLoader) apm.getPageLoader();
        linkToTerrain(terrain);

        grassGen = new GrassGeometryGenerator(terrain);
        init();
    }

    protected final void init() {
        pagingManager.setPageLoader(this);
    }

    @Override
    public Callable<Boolean> loadPage(Page page) {
        page.setPageVersion(PV);
        GrassPage gPage = (GrassPage) page;
        GrassLoadTask glt = new GrassLoadTask();
        glt.setPage(gPage);
        return glt;
    }

    @Override
    public void update(float tpf) {
        for (GrassLayer layer : layers) {
            layer.update();
        }
        pagingManager.update(tpf);
    }

    @Override
    public GrassPage createPage(int x, int z) {
        Logger.getLogger(GrassLoader.class.getName()).log(Level.INFO, "GrassTile created at: ({0},{1})", new Object[]{x, z});
        return new GrassPage(x, z, pagingManager);
    }

    /**
     * Use a terrainloader to provide data.
     *
     * @param tl
     * @return
     */
    public DataProvider linkToTerrain(TerrainLoader tl) {
        this.dataProvider = new TerrainBasedDensityMapGrid(tl.getMapProvider());
        return dataProvider;
    }

    /**
     * Add a grasslayer to this loader. Should not be called manually.
     *
     * @param layer
     * @return
     */
    public GrassLayer addLayer(GrassLayer layer) {
        layer.setID(ID++);
        layers.add(layer);
        layer.setPageLoader(this);
        layer.setWind(wind);
        incrementPageVersion();
        return layer;
    }

    //***************************Getters and setters***********************
    public ArrayList<GrassLayer> getLayers() {
        return layers;
    }

    public void setWind(Vector2f wind) {
        for (GrassLayer layer : layers) {
            layer.setWind(wind);
        }
    }

    public void setUseBinaries(boolean useBinaries) {
        this.useBinaries = useBinaries;
    }

    public boolean isUseBinaries() {
        return useBinaries;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
        incrementPageVersion();
    }

    public TerrainLoader getTerrain() {
        return terrain;
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
        incrementPageVersion();
    }

    protected class GrassLoadTask extends LoadTask<GrassPage> {

        @Override
        protected boolean generate() {

            if (useBinaries) {
                String textureFolder = EcoManager.getInstance().getTextureFolder();
                String fullName = textureFolder + "Tile_" + page.getX() + "_" + page.getZ() + "/Grass.j3o";

                File data = new File(fullName);
                if (data.exists()) {
                    GrassPage gp = null;
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(data);
                        BinaryImporter imp = new BinaryImporter();
                        AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
                        imp.setAssetManager(assetManager);
                        gp = (GrassPage) imp.load(new BufferedInputStream(fis), null, null);
                    } catch (IOException ex) {
                        Logger.getLogger(GrassLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            if (fis != null) {
                                fis.close();
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(GrassLoader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    boolean pv = (gp.getPageVersion() == PV);
                    boolean res = (gp.getResolution() == pagingManager.getResolution());
                    boolean size = (gp.getPageSize() == pagingManager.getPageSize());
                    // If these values all match, update the materials of each geometry,
                    // and finalize the process.
                    if (pv && res && size) {
                        // Set the paging manager to the currently used one.
                        page.createBlocks();
                        for (int j = 0; j < page.getBlocks().size(); j++) {
                            GrassBlock b = (GrassBlock) page.getBlock(j);
                            GrassBlock loadedB = (GrassBlock) gp.getBlock(j);
                            b.setNodes(loadedB.getNodes());
                            Node node = b.getNode(0);
                            // Find the proper material.
                            for (int i = 0; i < node.getChildren().size(); i++) {
                                Geometry geom = (Geometry) node.getChild(i);
                                Material mat = geom.getMaterial();
                                for(int l = 0; l < layers.size(); l++){
                                    GrassLayer layer = layers.get(l);
                                    if(layer.getMaterial().getTextureParam("DiffuseMap").getValue().equals(mat.getTextureParam("DiffuseMap").getValue())){
                                        geom.setMaterial(layer.getMaterial());
                                        break;
                                    }
                                }
                            }//for each layer
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

            //Creates the empty page objects.
            page.createBlocks();
            ArrayList<Block> blocks = page.getBlocks();
            //Loads grass geometry to each page.

            for (Block b : blocks) {
                GrassBlock block = (GrassBlock) b;
                Node[] nodes = new Node[1];
                nodes[0] = new Node("Grass" + page.hashCode());
                for (int i = 0; i < layers.size(); i++) {
                    GrassLayer layer = layers.get(i);

                    Geometry geom = grassGen.createGrassGeometry(layer,
                            block,
                            page,
                            dataProvider);

                    if (geom != null) {
                        nodes[0].attachChild(geom);
                        geom.setShadowMode(shadowMode);
                    }

                }//for each layer
                nodes[0].setShadowMode(shadowMode);
                block.setNodes(nodes);
            }//for each block

            if (useBinaries) {
                String textureFolder = EcoManager.getInstance().getTextureFolder();
                FileOutputStream fos = null;
                try {
                    File dir = new File(textureFolder + "Tile_" + page.getX() + "_" + page.getZ());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File object = new File(textureFolder + "Tile_" + page.getX() + "_" + page.getZ() + "/Grass.j3o");
                    if (object.exists()) {
                        // Should never happen.
                        throw new RuntimeException("ERROR: The grass data for this tile already exists. Delete old contents first. (" + object.getPath() + ")");
                    }
                    fos = new FileOutputStream(object);
                    // we just use the exporter and pass in the terrain
                    BinaryExporter.getInstance().save(page, new BufferedOutputStream(fos));
                    fos.flush();
                } catch (IOException ex) {
                    Logger.getLogger(GrassLoader.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Logger.getLogger(GrassLoader.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }

            return true;
        } // generate
    }//LoadTask
}//GrassLoader