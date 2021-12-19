/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

package ru.arifolth.game;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
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

import java.util.logging.Logger;

public class FractalTerrainGrid implements FractalTerrainGridInterface {
    final private static Logger LOGGER = Logger.getLogger(FractalTerrainGrid.class.getName());

    private Material matTerrain;
    private RigidBodyControl landscape;
    private TerrainGrid terrain;

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

    public FractalTerrainGrid(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;
    }

    @Override
    public TerrainGrid generateTerrain() {
        // TERRAIN TEXTURE material
        this.matTerrain = new Material(this.assetManager, "MatDefs/HeightBasedTerrain.j3md");

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
        this.matTerrain.setTexture("region1ColorMap", grass);
        this.matTerrain.setVector3("region1", new Vector3f(15, 200, this.grassScale));

        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        this.matTerrain.setTexture("region2ColorMap", dirt);
        this.matTerrain.setVector3("region2", new Vector3f(0, 20, this.dirtScale));

        // ROCK texture
        Texture rock = this.assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        this.matTerrain.setTexture("region3ColorMap", rock);
        this.matTerrain.setVector3("region3", new Vector3f(198, 260, this.rockScale));

        this.matTerrain.setTexture("region4ColorMap", rock);
        this.matTerrain.setVector3("region4", new Vector3f(198, 260, this.rockScale));

        this.matTerrain.setTexture("slopeColorMap", rock);
        this.matTerrain.setFloat("slopeTileFactor", 32);

        this.matTerrain.setFloat("terrainSize", 513);

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

        this.terrain.setMaterial(this.matTerrain);

        setupPosition();

        setUpLODControl();

        setUpCollision();

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
        control.setLodCalculator(new DistanceLodCalculator(65, 2.7f)); // patch size, and a multiplier
        this.terrain.addControl(control);
    }

    private void setUpCollision() {
        terrain.addListener(new TerrainGridListener() {
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
            }

            @Override
            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                if (quad.getControl(RigidBodyControl.class) != null) {
                    bulletAppState.getPhysicsSpace().remove(quad);
                    quad.removeControl(RigidBodyControl.class);
                }
            }

        });
    }
}
