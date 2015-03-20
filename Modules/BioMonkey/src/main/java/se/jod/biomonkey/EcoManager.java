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
package se.jod.biomonkey;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.atmosphere.AtmosphereManager;
import se.jod.biomonkey.biomes.BMWorld;
import se.jod.biomonkey.biomes.Biotope;
import se.jod.biomonkey.biomes.plants.Grass;
import se.jod.biomonkey.biomes.plants.Plant;
import se.jod.biomonkey.biomes.plants.Tree;
import se.jod.biomonkey.grass.GrassLoader;
import se.jod.biomonkey.paging.ActivePagingManager;
import se.jod.biomonkey.random.RandomTable;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid;
import se.jod.biomonkey.trees.TreeLoader;

/**
 * This is the root class of the BioMonkey system. It should be instantiated
 * and configured before anything else is done.
 * 
 * @author Andreas
 */
public class EcoManager {

    protected static final Logger log = Logger.getLogger(EcoManager.class.getName());
    protected Node rootNode;
    protected Camera camera;
    protected SimpleApplication app;
    protected ExecutorService executor;
    protected PhysicsSpace physicsSpace;
    protected boolean physicsEnabled;
    // Random table used to generate grass and trees.
    protected RandomTable table;
    protected GrassLoader grassLoader;
    protected TreeLoader treeLoader;
    protected static EcoManager instance;
    protected BMWorld world;
    protected Vector2f wind = new Vector2f(1,1);
    protected LoaderParams grassParams;
    protected LoaderParams treeParams;
    protected TerrainMapGrid terrainMapGrid;
    protected AtmosphereManager atmosphereManager;
    protected String textureFolder = "Assets/BioMonkey/";
    /**
     * Creates a new EcoManager object.
     * 
     * @param app The jME application object.
     */
    synchronized public static EcoManager initialize(SimpleApplication app) {
        instance = getInstance();
        instance.rootNode = app.getRootNode();
        instance.app = app;
        instance.camera = app.getCamera();

        BulletAppState bas = app.getStateManager().getState(BulletAppState.class);
        if (bas == null) {
            log.log(Level.INFO, "No PhysicsSpace provided, physics has been disabled.");
        } else {
            instance.physicsSpace = bas.getPhysicsSpace();
            instance.physicsEnabled = true;
        }

        instance.table = new RandomTable();

        return instance;
    }

    /**
     * Get the EcoManager instance.
     * @return The EcoManager instance.
     */
    synchronized public static EcoManager getInstance() {
        if (instance == null) {
            instance = new EcoManager();
            log.log(Level.INFO, "EcoManager was instantiated.");
        }
        return instance;
    }

    /**
     * Destroy the EcoManager instance.
     */
    synchronized public static void destroy() {
        if (instance != null) {
            instance = null;
            log.log(Level.INFO, "EcoManager was destroyed.");
        }
    }

    /**
     * The update method. Call this method every frame to update the pageloaders.
     * 
     * @param tpf Time passed since the last update (in seconds).
     */
    public void update(float tpf) {
        if (world != null) {
            world.update(tpf);
        }
        if (grassLoader != null) {
            grassLoader.update(tpf);
        }
        if (treeLoader != null) {
            treeLoader.update(tpf);
        }
        if(atmosphereManager != null){
            atmosphereManager.update(tpf);
        }
    }

    /**
     * Get the current BioMonkey world instance.
     * @return 
     */
    public BMWorld getBMWorldInstance() {
        if (world == null) {
            world = new BMWorld(this);
        }
        return world;
    }

    /**
     * Create a grass object.
     * @param name The name of the grass object.
     * @param tex The color texture of the grass.
     * 
     * @return The grass object.
     */
    public Grass createGrass(String name, Texture tex) {
        Grass grass = new Grass(name, tex);
        return grass;
    }
    
    /**
     * Create a grass object.
     * @param name The name of the grass object.
     * @param tex The color texture of the grass.
     * @param normal The normal texture of the grass.
     * @return The grass object.
     */
    public Grass createGrass(String name, Texture tex, Texture normal){
        Grass grass = new Grass(name,tex,normal);
        return grass;
    }

    /**
     * Create a tree object.
     * 
     * @param name The name of the tree object.
     * @param model The tree model.
     * @return The tree object.
     */
    public Tree createTree(String name, Spatial model) {
        Tree tree = new Tree(name, model);
        return tree;
    }

    /**
     * Link a plant object to a biotope. This makes the plant grow
     * in the given biotope - i.e. the distribution of the biotope
     * (its biotope map) is factored into the planting algorithm of 
     * the plant. A plant can be linked to multiple biotopes.
     * 
     * @param p The plant.
     * @param b The biotope.
     * @param slopes Whether or not to use the slopes for planting.
     * If true, the plant will only grow on the slopes (as defined
     * in the biotope settings).
     * 
     */
    public void link(Plant p, Biotope b, boolean slopes) {
        if (p instanceof Grass) {
            Grass g = (Grass) p;
            if (!slopes) {
                boolean exists = false;
                int idx = -1;
                // Check that the texture data is available.
                for (int i = 0; i < Biotope.getTTDS().size(); i++) {
                    if(Biotope.getTTDS().get(i) == b.getGroundTexData()){
                        exists = true;
                        idx = i;
                        break;
                    }
                }
                if(!exists){
                    throw new RuntimeException("The biotope does not have the specified texture data.");
                }
                HashMap<Grass, List<Byte>> grassMap = getBMWorldInstance().getGrassMap();
                if(!grassMap.containsKey(g)){
                    grassMap.put(g, new ArrayList<Byte>());
                }
                grassMap.get(g).add((byte)idx);
            } else if (slopes){
                boolean exists = false;
                int idx = -1;
                // Check that the texture data is available.
                for (int i = 0; i < Biotope.getTTDS().size(); i++) {
                    if(Biotope.getTTDS().get(i) == b.getGroundTexData2()){
                        exists = true;
                        idx = i;
                        break;
                    }
                }
                if(!exists){
                    throw new RuntimeException("The biotope does not have the specified texture data.");
                }
                HashMap<Grass, List<Byte>> grassMap = getBMWorldInstance().getGrassMap();
                if(!grassMap.containsKey(g)){
                    grassMap.put(g, new ArrayList<Byte>());
                }
                grassMap.get(g).add((byte)idx);
            }
        } else if (p instanceof Tree) {
            Tree t = (Tree) p;
            if (!slopes) {
                boolean exists = false;
                int idx = -1;
                // Check that the texture data is available.
                for (int i = 0; i < Biotope.getTTDS().size(); i++) {
                    if(Biotope.getTTDS().get(i) == b.getGroundTexData()){
                        exists = true;
                        idx = i;
                        break;
                    }
                }
                if(!exists){
                    throw new RuntimeException("The biotope does not have the specified texture data.");
                }
                HashMap<Tree, List<Byte>> treesMap = getBMWorldInstance().getTreesMap();
                if(!treesMap.containsKey(t)){
                    treesMap.put(t, new ArrayList<Byte>());
                }
                treesMap.get(t).add((byte)idx);
            } if (slopes) {
                boolean exists = false;
                int idx = -1;
                // Check that the texture data is available.
                for (int i = 0; i < Biotope.getTTDS().size(); i++) {
                    if(Biotope.getTTDS().get(i) == b.getGroundTexData2()){
                        exists = true;
                        idx = i;
                        break;
                    }
                }
                if(!exists){
                    throw new RuntimeException("The biotope does not have the specified texture data.");
                }
                HashMap<Tree, List<Byte>> treesMap = getBMWorldInstance().getTreesMap();
                if(!treesMap.containsKey(t)){
                    treesMap.put(t, new ArrayList<Byte>());
                }
                treesMap.get(t).add((byte)idx);
            }
        }
    }

    /**
     * Link a plant object to a biotope. This makes the plant grow
     * in the given biotope - i.e. the distribution of the biotope
     * (its biotope map) is factored into the planting algorithm of 
     * the plant. A plant can be linked to multiple biotopes.
     * 
     * @param p The plant.
     * @param b The biotope.
     */
    public void link(Plant p, Biotope b) {
        link(p, b, false);
    }

    /**
     * Get the grass loader parameters. The grass loader parameters controls
     * various grass settings, such as number of meshes per terrain tile,
     * the far viewing range of the grass, and the distance where grass begins
     * to fade out.
     * 
     * @return The parameters.
     */
    public LoaderParams getGrassLoaderParams() {
        if (grassParams == null) {
            grassParams = new LoaderParams();
            grassParams.setFarViewingRange(camera.getFrustumFar() * 0.1f);
            grassParams.setFadeRange(grassParams.getFarViewingRange() * 0.2f);
            grassParams.setResolution((world.getTerrainData().getRealTileSize()) / 128);
        }
        return grassParams;
    }

    /**
     * Get the tree loader parameters. The tree loader parameters controls
     * various tree settings, such as number of batches per terrain tile,
     * the far viewing range of the trees and the distance for impostor
     * transitions.
     * 
     * @return The parameters.
     */
    public LoaderParams getTreeLoaderParams() {
        if (treeParams == null) {
            treeParams = new LoaderParams();
            treeParams.setFarViewingRange(camera.getFrustumFar());
            treeParams.setFadeRange(0);
            treeParams.setFadeDistance(0);
            treeParams.setResolution((world.getTerrainData().getRealTileSize()) / 128);
        }
        return treeParams;
    }

    protected GrassLoader createGrassLoader() {

        getBMWorldInstance();
        getGrassLoaderParams();

        grassLoader = new GrassLoader(
                world.getTerrainData().getTileSize(),
                grassParams.getResolution(),
                grassParams.getFarViewingRange(),
                grassParams.getFadeRange(),
                new Node("GrassLoaderNode"),
                camera,
                (ActivePagingManager) world.getTerrainLoader().getPagingManager());
        grassLoader.setWind(wind);
        return grassLoader;
    }

    protected TreeLoader createTreeLoader() {
        
        getBMWorldInstance();
        getTreeLoaderParams();

        treeLoader = new TreeLoader(
                world.getTerrainData().getTileSize(),
                treeParams.getResolution(),
                treeParams.getFarViewingRange(),
                treeParams.getFadeDistance(),
                treeParams.getFadeRange(),
                new Node("TreeLoaderNode"),
                camera,
                (ActivePagingManager) world.getTerrainLoader().getPagingManager());

        return treeLoader;
    }

    /**
     * Get the grassloader instance. If a grassloader isn't active, a new one
     * will be created. NOTE: This method is used internally and should not
     * be called by users.
     * 
     * @return 
     */
    public GrassLoader getGrassLoader() {
        if (grassLoader != null) {
            return grassLoader;
        } else {
            createGrassLoader();
        }
        return grassLoader;
    }

    /**
     * Get the treeloader instance. If a reeloader isn't active, a new one
     * will be created. NOTE: This method is used internally and should not
     * be called by users.
     * 
     * @return 
     */
    public TreeLoader getTreeLoader() {
        if (treeLoader != null) {
            return treeLoader;
        } else {
            createTreeLoader();
        }
        return treeLoader;
    }

    /**
     * Get the random table used in grass/tree placement.
     * @return The random table.
     */
    public RandomTable getRandomTable() {
        return table;
    }

    /**
     * Set the world seed. This value is used to change the random placement
     * of grass and trees.
     * 
     * @param value The seed
     */
    public void setWorldSeed(long value) {
        table.offsetTable(value);
    }

    /**
     * Get the value used to seed the random table.
     * @return The seed.
     */
    public long getWorldSeed() {
        return table.getWorldSeed();
    }

    /**
     * Generate a new random seed.
     * @return The generated seed value.
     */
    public long setRandomWorldSeed() {
        return table.offsetTable();
    }

    /**
     * Get the application instance.
     * @return 
     */
    public Application getApp() {
        return app;
    }

    /**
     * Get the camera.
     * @return 
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Get the scene root node.
     * @return 
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * Returns true if physics is enabled.
     * @return 
     */
    public boolean isPhysicsEnabled() {
        return physicsEnabled;
    }

    /**
     * Returns the physics space, if physics is enabled.
     * @return 
     */
    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    /**
     * Get the wind vector. 
     * @return 
     */
    public Vector2f getWind() {
        return wind;
    }

    /**
     * Set the wind vector.
     * @param wind 
     */
    public void setWind(Vector2f wind) {
        this.wind = wind;
        if (grassLoader != null) {
            grassLoader.setWind(wind);
        }
    }

    /**
     * Get the atmosphere manager instance. NOTE: This method creates a new 
     * atmosphere manager if one isn't already active. Don't call this method 
     * unless you want to activate the BioMonkey atmosphere system.
     * 
     * @return 
     */
    public AtmosphereManager getAtmosphereManager() {
        if(atmosphereManager == null){
            this.atmosphereManager = new AtmosphereManager(this);
        }
        return atmosphereManager;
    }

    /**
     * Get the root folder for storing BioMonkey textures and other data.
     * @return 
     */
    public String getTextureFolder() {
        return textureFolder;
    }

    /**
     * Set the root folder for storing BioMonkey textures and other data.
     * 
     */
    public void setTextureFolder(String textureFolder) {
        this.textureFolder = textureFolder;
    }

    /**
     * Gets the executor service for multi-threaded page loading. NOTE: Should
     * not be called by the user.
     * 
     * @return 
     */
    public ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("BioMonkey");
                    th.setDaemon(true);
                    return th;
                }
            });
        }
        return executor;
    }
    
} // EcoManager
