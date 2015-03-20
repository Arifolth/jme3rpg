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
package basic;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PssmShadowFilter;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.atmosphere.AtmosphereManager;
import se.jod.biomonkey.biomes.BMWorld;
import se.jod.biomonkey.biomes.Biome;
import se.jod.biomonkey.biomes.Biotope;
import se.jod.biomonkey.biomes.plants.Grass;
import se.jod.biomonkey.biomes.plants.Tree;
import se.jod.biomonkey.grass.GrassLayer;
import se.jod.biomonkey.image.AlphaMipmapGenerator;
import se.jod.biomonkey.planting.PAUniform;
import se.jod.biomonkey.terrain.TerrainTextureData.TerrainTextureUsage;
import se.jod.biomonkey.trees.TreeLayer;

/*
 * Vegetation test. Uses a couple of pre-generated low res terrains and some
 * grass and trees. 'useBinaries' is set to true with grass and trees, meaning
 * they will be created the first time the example is run, then re-used.
 * 
 * To generate new terrains, just delete the old terrainData.obj files (from all
 * 'Tile_x_y' folders under assets/biomonkey). There is currently a mechanism
 * in place to stop biomonkey from overwriting terrain data in case the write-flag
 * was accidentally left as 'true'.
 * 
 * @author Andreas
 */
public class VegetationTest extends SimpleApplication implements ActionListener {
    
    protected float texScale = 64;
    protected EcoManager ecoManager;
    
    FilterPostProcessor fpp;
    PssmShadowFilter pssmSF;
    
    boolean shadows = false;
    
    public static void main(String[] args) {
    	//System.out.close();
    	//System.err.close();
        VegetationTest app = new VegetationTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("assets", ClasspathLocator.class);

        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText helloText = new BitmapText(guiFont, false);
        helloText.setSize(guiFont.getCharSet().getRenderedSize());
        helloText.setText("Press 'G' to enable/disable shadows.");
        helloText.setLocalTranslation(300, helloText.getLineHeight(), 0);
        guiNode.attachChild(helloText);
        
        rootNode.setShadowMode(ShadowMode.Off);
        
        cam.setLocation(new Vector3f(0, 80, 0));
        flyCam.setMoveSpeed(25);
        
        // Set up the biomonkey system.
        setupBiomes();
        
        // Set up a few filters (including shadow filter).
        setupFilters();
        
        inputManager.addMapping("Shadows", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "Shadows");
    }

    // The setup consists of four main steps.
    //
    // 1 - Initialize the system.
    // 2 - Add biomes and biotopes (terrain zones).
    // 3 - Add plants (trees and grass).
    // 4 - Generate everything, and do some final tweaks.
    //
    public void setupBiomes() {
        
        /**************************** Initialization ******************************/
        
        // Setup the EcoManager. Call initialize once, then access the ecomanager
        // using a reference, or by calling EcoManager.getInstance()
        ecoManager = EcoManager.initialize(this);
        // Just starting up the atmosphere manager to get some 
        AtmosphereManager atmosphereManager = ecoManager.getAtmosphereManager();
        atmosphereManager.setDynamic(true);
        atmosphereManager.getPositionProvider().getCalendar().reset(2009, 6, 2, 12, 32, 1000.f);
        ecoManager.setWind(new Vector2f(1, 1));
//        ecoManager.getAtmosphereManager();
        // When getBMWorldInstance is called for the first time it creates
        // a default world object.
        BMWorld world = ecoManager.getBMWorldInstance();
                
        // Our world is divided up into tiles of size 128x128 units squared. This
        // The default value is 512.
        world.getTerrainData().setTileSize(128);
        // Units per vert means that the distance between verts is
        // multiplied by four. Our 128x128 tiles will be 512x512 world units
        // in size. Default value is 1. unitsPerVert*tileSize is equal to the
        // "real tile size", and is 512 by default. It can be gotten by calling
        // world.getTerrainData().getRealTileSize();
        world.getTerrainData().setUnitsPerVert(4);
        // There are 2x2 tiles in the world. Default is 1x1. Our world will end
        // up being 1024x1024 m^2, or about 1 km^2, because default is using
        // meters and 1 unit = 1 meter (like bullet does). Also, the distance
        // between 2 terrain verts (in x or z) will be 4 meters, so any features
        // less then 4 meters in size can not be seen.
        world.getTerrainData().setNumTiles(2,2);
        // The height scale is 20. Each height value is multiplied by
        // 20. This is very much a matter of taste, and works in conjunction.
        // with the heightoffset and noise scale.
        world.getTerrainData().setHeightScale(20); 
        // This creates more or less rough terrains. There's more info in the
        // HMFT class. Default is 0.6
        world.getTerrainData().setHeightOffset(0.9f); // <-- non essential
        
        // Used to generate different world (offset the noise).
        world.getTerrainData().setSeed(23); // <-- non essential
        
        // We now configure the grass and treeloaders. The distances are in
        // world units. This does not have to be done, as both tree and
        // grassloader params have default values. The default value for
        // trees are no impostors tho (fadeDistance = 0).
        
        // Grass is fully visible up to 200 units away.
        ecoManager.getGrassLoaderParams().setFarViewingRange(400);
        // Grass is faded out over a distance of 50 units. At 250 units it's completely gone.
        ecoManager.getGrassLoaderParams().setFadeRange(50); 
        
        // Trees use 2x2 batches per tile. Grass uses the default number which is 
        // 4x4 batches per tile.
        ecoManager.getTreeLoaderParams().setResolution(2);
        // Trees fades out and are replaced by impostors at 300 units away.
        ecoManager.getTreeLoaderParams().setFadeDistance(300);
        // The transiton takes place over a distance of 50 units.
        ecoManager.getTreeLoaderParams().setFadeRange(50);
        
        
        /**************************** Biomes ******************************/
        
        // The world will be divided up into two biomes, based on elevation. Each
        // biome will have one single biotope in it.
        
        // We start by creating a biome called lowlands. It's present in low
        // elevation areas. Biomes are created from the world object.
        Biome lowlands = world.createBiome("Lowlands");
        lowlands.getGeoData().setMinMaxElevation(0f, .4f); // Use values between 0 and 1.
        
        // We create a biotope within this biome called grassland. Biotopes are created
        // from the respective biome object.
        Biotope grassLand = lowlands.createBiotope("Grassland");
        
        // Add terrain color- and normal textures for this biotope.
        Texture greenGrassTex = assetManager.loadTexture("Textures/Terrain/grass_dark_512.png");
        greenGrassTex.setWrap(WrapMode.Repeat);
        
        Texture greenGrassTexN = assetManager.loadTexture("Textures/Terrain/grass_512_NORM.png");
        greenGrassTexN.setWrap(WrapMode.Repeat);
        
        // We use these textures for regular areas, not slopes. TerrainTextureUsage.Default
        // Color is not important (only for debug).
        grassLand.setTextureData(greenGrassTex, greenGrassTexN, ColorRGBA.Gray, texScale, TerrainTextureUsage.Default);
        
        // Now we add textures for the slopes in the same way.
        Texture dirtTex = assetManager.loadTexture("Textures/Terrain/dirt_dark_512.png");
        dirtTex.setWrap(WrapMode.Repeat);
        
        Texture dirtTexN = assetManager.loadTexture("Textures/Terrain/dirt_512_NORM.png");
        dirtTexN.setWrap(WrapMode.Repeat);
        
        grassLand.setTextureData(dirtTex, dirtTexN, ColorRGBA.Blue, texScale, TerrainTextureUsage.Slopes);
        
        // Here we specify that the slope texture should be used when the 
        // terrain slope is 50 degrees or more.
        grassLand.getGeoData().setMaxSlope(50);
        
        // We have now created a biome that extends from sea level (0) to about .3 of the 
        // normalized height range. It contains only one biotope (and that biotope therefore 
        // covers the entire biome). Now add one more. It is done in the exact same way, but
        // with different values.
        Biome mountainsbiome = world.createBiome("Mountains");
        mountainsbiome.getGeoData().setMinMaxElevation(0.4f, 1f);
        
        Biotope mountains = mountainsbiome.createBiotope("Mountains");
        
        Texture cliffTex = assetManager.loadTexture("Textures/Terrain/dirt_gray_512.png");
        cliffTex.setWrap(WrapMode.Repeat);
        
        Texture cliffTexN = assetManager.loadTexture("Textures/Terrain/dirt_512_NORM.png");
        cliffTexN.setWrap(WrapMode.Repeat);

        Texture grassTex = assetManager.loadTexture("Textures/Terrain/grass_dead_512.png");
        grassTex.setWrap(WrapMode.Repeat);
        
        Texture grassTexN = assetManager.loadTexture("Textures/Terrain/grass_dead_512_NORM.png");
        grassTexN.setWrap(WrapMode.Repeat);
        
        mountains.setTextureData(grassTex, grassTexN, ColorRGBA.Gray, texScale, TerrainTextureUsage.Default);
        mountains.setTextureData(cliffTex, cliffTexN, ColorRGBA.White, texScale, TerrainTextureUsage.Slopes);
        mountains.getGeoData().setMaxSlope(40);
        
        // ************************ Plants ****************************
        
        // Now that biomes and biotopes are set up, we add plants. 
        
        // Grass 1
        Texture grassPatchTex = assetManager.loadTexture("Textures/flowers1.png");
        Texture grassPatchTexNorm = assetManager.loadTexture("Textures/flowers1_NORM.png");
        // The value here should be different depending on the texture.
        AlphaMipmapGenerator.generateMipMaps(grassPatchTex.getImage(), 1.5f);
        grassPatchTex.setAnisotropicFilter(4);
        // Create a 'Grass' plant object from the color and normal textures.
        Grass grass = ecoManager.createGrass("Yellow flowers", grassPatchTex, grassPatchTexNorm);

        // Now we link this plant to various zones. We want to add it to the grassLand biotope.
        // Each biotope here has two zones - the default zone and the slopes. We want it to grow 
        // on both flat land and slopes, so we add it to both zones. The third param is 'use on slopes', 
        // and is false by default.
        ecoManager.link(grass, grassLand);
        ecoManager.link(grass, grassLand,true);

        // Do some fine tuning of the grass (quad size etc.).
        GrassLayer layer = grass.getGrassLayer();
        
        // Sets the global density of this grass. The value 1 means 1 patch
        // per 1 world unit squared, which is way too dense. 0.1 is the default value.
        layer.setDensityMultiplier(0.02f);

        // Configure the grass quad sizes. Default is 0.8 and 1.2 min/max for both height
        // and width.
        layer.setMaxHeight(2.4f);
        layer.setMinHeight(2.f);

        layer.setMaxWidth(2.4f);
        layer.setMinWidth(2.f);

        // Keeps grass from being planted in areas with too much slope (which
        // distorts the quads). Actually 60 degrees is a bit high.
        layer.setMaxTerrainSlope(60);
        
        // This is the alpha discard threshold value used in the grass shader.
        layer.setAlphaCutOff(0.4f);
        // If the density of the associated biotopes are below 0.3 at a point, 
        // no grass is planted there. This is a way to ensure that grass does not grow
        // in areas where their biotopes are present, but barely even visible.
        //
        // There are several more values that can be set in the planting algorithms,
        // and there are more algorithms then the default PAUniform.
        ((PAUniform) layer.getPlantingAlgorithm()).setThreshold(0.3f);

        
        // Grass 2 - same process
        Texture grassPatchTex2 = assetManager.loadTexture("Textures/grass2.png");
        Texture grassPatchTex2Norm = assetManager.loadTexture("Textures/grass2_NORM.png");
        grassPatchTex2.setAnisotropicFilter(4);
        AlphaMipmapGenerator.generateMipMaps(grassPatchTex2.getImage(), 1.5f);
        // Plant object.
        Grass grass2 = ecoManager.createGrass("Grass", grassPatchTex2,grassPatchTex2Norm);

        // Add to the biotope grassLand. We don't want this on the slopes.
        ecoManager.link(grass2, grassLand);

        // Do some fine tuning of the grass (quad size etc.).
        GrassLayer layer2 = grass2.getGrassLayer();
        
        layer2.setDensityMultiplier(0.85f);

        layer2.setMaxHeight(3.9f);
        layer2.setMinHeight(3.f);

        layer2.setMaxWidth(3.9f);
        layer2.setMinWidth(3.f);

        layer2.setMaxTerrainSlope(60);
        
        layer2.setAlphaCutOff(0.7f);
        ((PAUniform) layer2.getPlantingAlgorithm()).setThreshold(0.3f);
                
        // Grass 3 - mountain slopes
        Texture grassPatchTex3 = assetManager.loadTexture("Textures/tistel.png");
        Texture grassPatchTex3Norm = assetManager.loadTexture("Textures/tistel_NORM.png");
        AlphaMipmapGenerator.generateMipMaps(grassPatchTex3.getImage(), 1.4f);
        grassPatchTex3.setAnisotropicFilter(4);
        // Plant object.
        Grass grass3 = ecoManager.createGrass("Thistel", grassPatchTex3,grassPatchTex3Norm);

        // We only want this grass to grow on mountain slopes.
        ecoManager.link(grass3, mountains,true);
        
        // Do some fine tuning of the grass (quad size etc.).
        GrassLayer layer3 = grass3.getGrassLayer();
        
        layer3.setDensityMultiplier(0.15f);
        
        layer3.setMaxHeight(3.9f);
        layer3.setMinHeight(3.f);
        
        layer3.setMaxWidth(3.9f);
        layer3.setMinWidth(3.f);
        
        layer3.setAlphaCutOff(0.7f);
        layer3.setMaxTerrainSlope(70);
        ((PAUniform) layer3.getPlantingAlgorithm()).setThreshold(0.2f);
        
        
        // That was the grass/flowers, now we move on to the other stuff. Everything
        // with a model should be a tree, everything with only a texture should be
        // a grass. This means rocks and bushes are trees.
        Spatial firTree = assetManager.loadModel("Models/Fir1/fir1_androlo.j3o");
        
        // Create a tree object. Tree, just as 'Grass', are plants. They both implement
        // Plant and works in about the same way. Pass the model to the tree constructor.
        Tree fir = new Tree("Fir",firTree);
        
        // Configure the tree. 
        TreeLayer treeLayer = fir.getTreeLayer();
        // min/max scale is the range that is used when randomly scaling the models. These
        // trees will be scaled between 2 and 3 times, randomly chosen for each individual.
        // Rotation is also random.
        treeLayer.setMaximumScale(3);
        treeLayer.setMinimumScale(2);
        // This value means 0.0003 trees per world unit, which translates to a
        // maximum of 80 trees per (512x512) tile. The actual number depends on
        // the density values of the various biotopes linked to the tree.
        treeLayer.setDensityMultiplier(0.00093f);
        
        // Do some texture stuff..
        Geometry g = (Geometry) fir.getTreeLayer().getModel().getChild(1);
        Texture branches = g.getMaterial().getTextureParam("DiffuseMap").getTextureValue();
        branches.setAnisotropicFilter(4);
        AlphaMipmapGenerator.generateMipMaps(branches.getImage(),2f);
        
        // Just like with the grass, we link this tree to biotopes. We want it to
        // grow in both biomes. Since there's only one biotope per biome, we just
        // have to link it to those two. Note that they will not grow in slopes.
        ecoManager.link(fir, grassLand);
        ecoManager.link(fir, mountains);
        
        // Adding some rocks.
        Node rockNode = (Node) assetManager.loadModel("Models/Rock1/rock1_nobiax.j3o");
        
        Tree rock = new Tree("Rock",rockNode);
        TreeLayer treeLayer2 = rock.getTreeLayer();
        treeLayer2.setMaximumScale(3);
        treeLayer2.setMinimumScale(2);
        treeLayer2.setDensityMultiplier(0.0001f); // max 26 rocks per tile.
        
        // Let this grow anywhere but on slopes, like the trees.
        ecoManager.link(rock, grassLand);
        ecoManager.link(rock, mountains);
        
        // Adding some bushes as well.
        Node bushNode = (Node) assetManager.loadModel("Models/Fern2/fern2_nobiax.j3o");
        Tree bush = new Tree("Bush",bushNode);
        
        Geometry g2 = (Geometry) bush.getTreeLayer().getModel().getChild(0);
        Texture leaves = g2.getMaterial().getTextureParam("DiffuseMap").getTextureValue();
        leaves.setAnisotropicFilter(4);
        AlphaMipmapGenerator.generateMipMaps(leaves.getImage(),1.5f);
        
        TreeLayer treeLayer3 = bush.getTreeLayer();
        treeLayer3.setMaximumScale(3f);
        treeLayer3.setMinimumScale(2f);
        treeLayer3.setDensityMultiplier(0.0001f);
        
        ecoManager.link(bush, grassLand);
        // We let these grow on the grassland slopes as well.
        ecoManager.link(bush, grassLand, true);
        ecoManager.link(bush, mountains);
        
        /**************************** Finalize ******************************/
        
        // Now we generate this world. We use 'false' as argument, because
        // the terrain tiles has already been generated. If we want new tiles,
        // using a different seed, or perhaps a larger amount of tiles, we need
        // to set the argument to 'true', and delete the previous contents in
        // assets/biomonkey (that means all objects named terrainData.j3o in the
        // 'Tile_x_y' folders. If failing to do so, the only thing that happens
        // is that an exception is thrown.
        world.generate(false);  //TODO:  true

        // Setting some values here. We want cast and receive for terrain and
        // models, while grass should only receive.
        //
        // Both tree and grass should be stored into binary files that can be
        // loaded from disk the next time the app is started.
        world.getTerrainLoader().setShadowMode(ShadowMode.CastAndReceive);
        ecoManager.getGrassLoader().setShadowMode(ShadowMode.Receive);
        ecoManager.getGrassLoader().setUseBinaries(true);
        ecoManager.getTreeLoader().setUseBinaries(true);
        ecoManager.getTreeLoader().setShadowMode(ShadowMode.CastAndReceive);
    }

    protected void setupFilters() {
        fpp = new FilterPostProcessor(assetManager);
        
        FXAAFilter fxaa = new FXAAFilter();
        
        fpp.addFilter(fxaa);
        
        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(0);
        dof.setFocusRange(200);
        dof.setBlurScale(0.6f);
        
        fpp.addFilter(dof);
        viewPort.addProcessor(fpp);
        
        pssmSF = new PssmShadowFilter(assetManager, 2024, 2);
        pssmSF.setDirection(ecoManager.getAtmosphereManager().getSun().getDirection());
        pssmSF.setLambda(0.66f);
        pssmSF.setShadowIntensity(0.6f);
        pssmSF.setCompareMode(CompareMode.Hardware);
        pssmSF.setFilterMode(FilterMode.PCFPOISSON);
    }

    @Override
    public void simpleUpdate(float tpf) {
        ecoManager.update(tpf);
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Shadows")) {
            if (!isPressed) {
                if(shadows){
                fpp.removeFilter(pssmSF);
                shadows = false;
                } else {
                fpp.addFilter(pssmSF);
                shadows = true;
                }
            }
        }
    }
    
    
}
