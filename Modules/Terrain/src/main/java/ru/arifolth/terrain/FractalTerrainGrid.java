/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2022 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.terrain;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.*;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.FractalTerrainGridInterface;
import ru.arifolth.anjrpg.interfaces.RolePlayingGameInterface;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FractalTerrainGrid implements FractalTerrainGridInterface {
    final private static Logger LOGGER = Logger.getLogger(FractalTerrainGrid.class.getName());

    private RigidBodyControl landscape;
    private TerrainQuad terrain;

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private RolePlayingGameInterface app;


    private FractalSum base;
    private PerturbFilter perturb;
    private OptimizedErode therm;
    private SmoothFilter smooth;
    private IterativeFilter iterate;

    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private TerrainQuad distantTerrain;

    public FractalTerrainGrid(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;
    }

    @Override
    public TerrainQuad generateTerrain() {
        // TERRAIN TEXTURE material
        Material matTerrain = new Material(this.assetManager, "MatDefs/HeightBasedTerrain.j3md");

        AppSettings settings = app.getContext().getSettings();
        matTerrain.getAdditionalRenderState().setWireframe(settings.getBoolean(Constants.DEBUG));

        // Parameters to material:
        // regionXColorMap: X = 1..4 the texture that should be appliad to state X
        // regionX: a Vector3f containing the following information:
        //      regionX.x: the start height of the region
        //      regionX.y: the end height of the region
        //      regionX.z: the texture scale for the region
        //  it might not be the most elegant way for storing these 3 values, but it packs the data nicely :)
        // slopeColorMap: the texture to be used for cliffs, and steep mountain sites
        // slopeTileFactor: the texture scale for slopes
        // terrainSize: the total size of the terrain (used for scaling the texture)
        // GRASS texture
        Texture grass = this.assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("region1ColorMap", grass);
        matTerrain.setVector3("region1", new Vector3f(15, 200, this.grassScale));

        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("region2ColorMap", dirt);
        matTerrain.setVector3("region2", new Vector3f(0, 20, this.dirtScale));

        // ROCK texture
        Texture rock = this.assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("region3ColorMap", rock);
        matTerrain.setVector3("region3", new Vector3f(198, 260, this.rockScale));

        matTerrain.setTexture("region4ColorMap", rock);
        matTerrain.setVector3("region4", new Vector3f(198, 260, this.rockScale));

        matTerrain.setTexture("slopeColorMap", rock);
        matTerrain.setFloat("slopeTileFactor", 32);

        matTerrain.setFloat("terrainSize", 513);

        this.base = new FractalSum();
        this.base.setRoughness(0.82f);
        this.base.setFrequency(1.2f);
        this.base.setAmplitude(1.1f);
        this.base.setLacunarity(2.12f);
        this.base.setOctaves(8);
        this.base.setScale(0.02125f);
        this.base.addModulator(new NoiseModulator() {

            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });

        FilteredBasis ground = new FilteredBasis(this.base);

        this.perturb = new PerturbFilter();
        this.perturb.setMagnitude(0.419f);

        this.therm = new OptimizedErode();
        this.therm.setRadius(1);
        this.therm.setTalus(0.711f);

        this.smooth = new SmoothFilter();
        this.smooth.setRadius(1);
        this.smooth.setEffect(0.7f);

        this.iterate = new IterativeFilter();
        this.iterate.addPreFilter(this.perturb);
        this.iterate.addPostFilter(this.smooth);
        this.iterate.setFilter(this.therm);
        this.iterate.setIterations(1);

        ground.addPreFilter(this.iterate);

        this.terrain = new TerrainGrid("terrain", 65, 1025, new FractalTileLoader(ground, 256f));

        this.terrain.setMaterial(matTerrain);

        setupPosition();

        setUpLODControl();

        setUpCollision();

        terrain.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        return terrain;
    }

    private void setupPosition() {
        //terrain postion
        terrain.setLocalTranslation(0, -200, 0);
        terrain.setLocalScale(2f, 1f, 2f);
    }

    private void setUpLODControl() {
        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainGridLodControl(this.terrain, app.getCamera());
        control.setLodCalculator(new DistanceLodCalculator(257, 2.7f)); // patch size, and a multiplier
        this.terrain.addControl(control);
    }

    private void setUpCollision() {
        ((TerrainGrid)terrain).addListener(new TerrainGridListener() {
            @Override
            public void gridMoved(Vector3f newCenter) {
            }

            @Override
            public void tileAttached(Vector3f cell, TerrainQuad quad) {
                while(quad.getControl(RigidBodyControl.class)!=null){
                    quad.removeControl(RigidBodyControl.class);
                }
                quad.addControl(new RigidBodyControl(new HeightfieldCollisionShape(quad.getHeightMap(), terrain.getLocalScale()), 0));
                bulletAppState.getPhysicsSpace().add(quad);
                //plant trees
                app.getGameLogicCore().getInitializationDelegate().positionTrees(quad, true);
            }

            @Override
            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                /*if (quad.getControl(RigidBodyControl.class) != null) {
                    bulletAppState.getPhysicsSpace().remove(quad);
                    quad.removeControl(RigidBodyControl.class);
                }*/
                List<Spatial> quadForest = quad.getUserData("quadForest");
                Stream<Spatial> stream = quadForest.stream();
                stream.forEach(treeNode -> {
//                    System.out.println("Detached " + treeNode.hashCode() + treeNode.getLocalTranslation().toString());
                    app.getGameLogicCore().getForestNode().detachChild(treeNode);
                });
            }

        });
    }

    @Override
    public void adjustMountainsPosition() {
        Vector3f playerLocation = app.getGameLogicCore().getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        playerLocation.y = Constants.MOUNTAINS_HEIGHT_OFFSET;
        distantTerrain.setLocalTranslation(playerLocation);
    }

    @Override
    public TerrainQuad generateMountains() {
        Material matTerrain;

        /** 1. Create distantTerrain material and load four textures into it. */
        /*matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);*/
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        matTerrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap_horizon.png"));

        //matTerrain.setTexture("GrassAlphaMap", assetManager.loadTexture(
        //"Textures/Terrain/grass-map512.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex1", grass);
        matTerrain.setFloat("Tex1Scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/snow.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex2", dirt);
        matTerrain.setFloat("Tex2Scale", 64f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/rock.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex3", rock);
        matTerrain.setFloat("Tex3Scale", 64f);


        /** 2. Create the height map */

        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/horizon.png");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();
        heightmap.smooth(0.65f, 1);
        heightmap.flatten((byte) 2);

        /** 3. We have prepared material and heightmap.
         * Now we createCharacter the actual distantTerrain:
         * 3.1) Create a TerrainQuad and name it "my distantTerrain".
         * 3.2) A good value for distantTerrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */

        int patchSize = 65;
        distantTerrain = new TerrainQuad(
                "Distant Terrain",
                patchSize,
                2049,
                heightmap.getHeightMap());

        /** 4. We give the distantTerrain its material, position & scale it, and attach it. */
        distantTerrain.setMaterial(matTerrain);
        distantTerrain.setLocalTranslation(0, Constants.MOUNTAINS_HEIGHT_OFFSET, 0);
        distantTerrain.setLocalScale(8f, 25f, 8f);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(distantTerrain, app.getCamera());
        distantTerrain.addControl(control);

        distantTerrain.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        return distantTerrain;
    }
}
