/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.*;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.*;

import java.util.logging.Logger;

public class FractalTerrainGrid implements FractalTerrainGridInterface {
    final private static Logger LOGGER = Logger.getLogger(FractalTerrainGrid.class.getName());
    private ViewDistanceSettings viewDistanceSettings;
    private TerrainQuad terrain;

    private final AssetManager assetManager;
    private final BulletAppState bulletAppState;
    private final RolePlayingGameInterface app;

    private final int rigidBodiesSize = Constants.RIGID_BODIES_SIZE;

    private FractalSum base;
    private PerturbFilter perturb;
    private OptimizedErode therm;
    private SmoothFilter smooth;
    private IterativeFilter iterate;

    private final float grassScale = 64;
    private final float dirtScale = 16;
    private final float rockScale = 128;
    VegetationInitialization initializationDelegate = new VegetationInitialization();

    public FractalTerrainGrid(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;
    }

    @Override
    public void initialize() {
        viewDistanceSettings = ViewDistanceSettings.valueOf((String) app.getContext().getSettings().getOrDefault(ViewDistanceSettings.class.getSimpleName(), ViewDistanceSettings.MED.name()));
    }

    @Override
    public TerrainQuad generateTerrain() {
        // TERRAIN TEXTURE material
        Material matTerrain = new Material(this.assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

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

        matTerrain.setFloat("terrainSize", viewDistanceSettings.getTerrainSize());

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
        this.perturb.setMagnitude(0.219f);

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

        this.terrain = new TerrainGrid("Terrain", viewDistanceSettings.getPatchSize(), viewDistanceSettings.getTerrainSize(), new FractalTileLoader(ground, 128f));

        this.terrain.setMaterial(matTerrain);

        setupPosition();

        setupScale();

        setUpLODControl();

        setUpCollision();

        terrain.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        terrain.setQueueBucket(RenderQueue.Bucket.Opaque);
        matTerrain.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        terrain.setCullHint(Spatial.CullHint.Dynamic);

        return terrain;
    }

    private void setupScale() {
        terrain.setLocalScale(Constants.TERRAIN_SCALE_X, Constants.TERRAIN_SCALE_Y, Constants.TERRAIN_SCALE_Z);
    }

    private void setupPosition() {
        //terrain postion
        terrain.setLocalTranslation(0, viewDistanceSettings.getLocalTranslation(), 0);
    }

    private void setUpLODControl() {
        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainGridLodControl control = new TerrainGridLodControl(this.terrain, app.getCamera());
        control.setLodCalculator(new DistanceLodCalculator(viewDistanceSettings.getPatchSize(), viewDistanceSettings.getLodMultiplier())); // patch size, and a multiplier
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
                quad.setLocked(true);
                bulletAppState.getPhysicsSpace().add(quad);

                initializationDelegate.setGameLogicCore(app.getGameLogicCore());
                //plant trees
                initializationDelegate.positionTrees(quad);
                //plant grass
                initializationDelegate.positionGrass(quad);
                //plant bushes
                initializationDelegate.positionBushes(quad);
                //plant bushes
                initializationDelegate.positionMushrooms(quad);
            }

            @Override
            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                if (quad.getControl(RigidBodyControl.class) != null) {
                    bulletAppState.getPhysicsSpace().remove(quad);
                    quad.removeControl(RigidBodyControl.class);
                }
                detachTrees(quad);
                detachGrass(quad);
                detachBushes(quad);
                detachMushrooms(quad);
            }

        });
    }

    private void detachGrass(TerrainQuad quad) {
        Node quadGrass = quad.getUserData(Constants.QUAD_GRASS);
        if(quadGrass != null) {
            app.getThrottledQueue().enqueue(() -> {
                app.getGameLogicCore().getGrassNode().detachChild(quadGrass);
            });
        }
    }

    private void detachTrees(TerrainQuad quad) {
        Node quadForest = quad.getUserData(Constants.QUAD_FOREST);
        if(quadForest != null) {
            app.getThrottledQueue().enqueue(() -> {
                app.getGameLogicCore().getForestNode().detachChild(quadForest);
            });
        }
    }

    private void detachMushrooms(TerrainQuad quad) {
        Node quadMushrooms = quad.getUserData(Constants.QUAD_MUSHROOMS);
        if(quadMushrooms != null) {
            app.getThrottledQueue().enqueue(() -> {
                app.getGameLogicCore().getMushroomsNode().detachChild(quadMushrooms);
            });
        }
    }

    private void detachBushes(TerrainQuad quad) {
        Node quadBushes = quad.getUserData(Constants.QUAD_BUSHES);
        if(quadBushes != null) {
            app.getThrottledQueue().enqueue(() -> {
                app.getGameLogicCore().getBushesNode().detachChild(quadBushes);
            });
        }
    }

    @Override
    public void update() {
    }

    @Override
    public int getRigidBodiesSize() {
        return rigidBodiesSize;
    }
}
