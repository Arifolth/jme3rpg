/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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
import ru.arifolth.anjrpg.TileTextureCapturer;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.menu.SettingsUtils;
import ru.arifolth.terrain.grass.GpuGrassMaterial;
import ru.arifolth.terrain.grass.GrassTextureFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FractalTerrainGrid implements FractalTerrainGridInterface {
    final private static Logger LOGGER = Logger.getLogger(FractalTerrainGrid.class.getName());
    private ViewDistanceSettings viewDistanceSettings;
    private TerrainQuad terrain;
    private TileTextureCapturer tileCapturer;
    private final AssetManager assetManager;
    private final BulletAppState bulletAppState;
    private final RolePlayingGameInterface app;
    private GpuGrassMaterial gpuGrassMaterial;
    private int gpuGrassUpdateFailures = 0;
    private long gpuGrassLastFailureLogNanos = 0L;
    //GPU grass death-camera anchor: when the player dies, the corpse node is
    //detached from the scene right after the death animation, while the death
    //camera keeps observing the place of death from above. The grass circle
    //must stay rendered there, so the last valid player position is remembered
    //and fed to the material until the player respawns (spawn() re-attaches
    //the node, possibly at a different place - tracking then resumes
    //automatically from there).
    private final Vector3f gpuGrassLastValidAnchor = new Vector3f();
    private boolean gpuGrassHasValidAnchor = false;
    private boolean gpuGrassAnchorHolding = false;

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
        this.tileCapturer = new TileTextureCapturer(app);
        viewDistanceSettings = SettingsUtils.getViewDistanceSettings(app.getContext().getSettings());
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

        //GPU grass: wrap the terrain material with the layered grass material
        //so the terrain is rendered once as the base and once per grass layer
        Material terrainMaterial = matTerrain;
        if (Constants.GPU_GRASS_ENABLED) {
            try {
                gpuGrassMaterial = new GpuGrassMaterial(assetManager, Constants.GPU_GRASS_LAYERS, Constants.GPU_GRASS_DISTANCE);
                gpuGrassMaterial.setBaseMaterial(matTerrain);
                gpuGrassMaterial.setGrassTexture(GrassTextureFactory.loadGrassTexture(assetManager));
                gpuGrassMaterial.setThreeDTexture(GrassTextureFactory.createThreeDLayerMask(Constants.GPU_GRASS_LAYERS));
                terrainMaterial = gpuGrassMaterial;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "GPU grass material setup failed, falling back to plain terrain material", e);
                gpuGrassMaterial = null;
                terrainMaterial = matTerrain;
            }
        }

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

        this.terrain.setMaterial(terrainMaterial);

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

    private static String generateBaseTileId(Vector3f cell) {
        return String.format("tile_%d_%d", (int)cell.x, (int)cell.z);
    }

    private void setUpCollision() {
        ((TerrainGrid)terrain).addListener(new TerrainGridListener() {
            @Override
            public void gridMoved(Vector3f newCenter) {
            }

            @Override
            public void tileAttached(Vector3f cell, TerrainQuad quad) {
                try {
                    while (quad.getControl(RigidBodyControl.class) != null) {
                        quad.removeControl(RigidBodyControl.class);
                    }
                    quad.addControl(new RigidBodyControl(new HeightfieldCollisionShape(quad.getHeightMap(), terrain.getLocalScale()), 0));
                    quad.setLocked(true);
                    bulletAppState.getPhysicsSpace().add(quad);

                    initializationDelegate.setGameLogicCore(app.getGameLogicCore());
                    //plant trees
                    initializationDelegate.positionTrees(quad);
                    //plant grass (skipped when the GPU grass shader renders it)
                    if (!Constants.GPU_GRASS_ENABLED || !Constants.GPU_GRASS_REPLACES_BILLBOARD_GRASS) {
                        initializationDelegate.positionGrass(quad);
                    }
                    //plant bushes
                    initializationDelegate.positionBushes(quad);
                    //plant bushes
                    initializationDelegate.positionMushrooms(quad);
                } finally {
                    if (tileCapturer != null) {
                        String baseTileId = generateBaseTileId(cell);
                        tileCapturer.syncCaptureTileTextures(quad, baseTileId);
                    }
                }
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
        processPendingTileCaptures();
        updateGpuGrass();
    }

    private void updateGpuGrass() {
        if (gpuGrassMaterial == null) {
            return;
        }
        try {
            Spatial playerNode = app.getGameLogicCore().getPlayerCharacter().getNode();
            Vector3f playerPosition = playerNode.getWorldTranslation();
            boolean playerAttached = playerNode.getParent() != null;
            boolean positionFinite = isFinite(playerPosition);

            if (playerAttached && positionFinite) {
                //Normal tracking: the grass circle follows the player and the
                //last valid position is remembered for the death-camera hold.
                gpuGrassLastValidAnchor.set(playerPosition);
                gpuGrassHasValidAnchor = true;
                if (gpuGrassAnchorHolding) {
                    gpuGrassAnchorHolding = false;
                    //fires once per respawn (including a respawn at a different place)
                    LOGGER.info("GPU grass anchor tracking the player again at ("
                            + formatPosition(playerPosition) + ")");
                }
                gpuGrassMaterial.update(app.getTimer().getTimeInSeconds(), playerPosition);
            } else if (gpuGrassHasValidAnchor) {
                //Player node detached (death: the corpse node leaves the scene
                //after the death animation while the death camera observes the
                //spot from above) or the position is not usable (NaN/Infinity
                //would poison every grass vertex). Hold the grass circle at the
                //place of death: the field stays rendered for the death camera,
                //and tracking resumes automatically on respawn.
                if (!gpuGrassAnchorHolding) {
                    gpuGrassAnchorHolding = true;
                    LOGGER.info("GPU grass anchor HOLDING at (" + formatPosition(gpuGrassLastValidAnchor)
                            + ") - " + (playerAttached ? "player position invalid" : "player node detached")
                            + ", grass stays at the place of death");
                }
                gpuGrassMaterial.update(app.getTimer().getTimeInSeconds(), gpuGrassLastValidAnchor);
            }
            //else: no valid anchor yet (loading phase) - keep the shared state
            //untouched instead of feeding (0,0,0) or an invalid position.
            gpuGrassUpdateFailures = 0;
        } catch (Exception e) {
            //player is not available yet (loading phase) - keep last known values,
            //but do not stay fully silent: grass silently not appearing is exactly
            //the class of bug that is impossible to diagnose from the outside
            gpuGrassUpdateFailures++;
            long now = System.nanoTime();
            if (gpuGrassUpdateFailures == 1) {
                LOGGER.log(Level.SEVERE, "GPU grass update failed - grass will not appear while this persists", e);
            } else if (now - gpuGrassLastFailureLogNanos > Constants.GPU_GRASS_DEBUG_LOG_INTERVAL_MS * 1_000_000L) {
                LOGGER.warning("GPU grass update still failing (" + gpuGrassUpdateFailures + " failures): " + e);
            }
            if (gpuGrassUpdateFailures == 1
                    || now - gpuGrassLastFailureLogNanos > Constants.GPU_GRASS_DEBUG_LOG_INTERVAL_MS * 1_000_000L) {
                gpuGrassLastFailureLogNanos = now;
            }
        }
    }

    private static boolean isFinite(Vector3f v) {
        return !Float.isNaN(v.x) && !Float.isNaN(v.y) && !Float.isNaN(v.z)
                && !Float.isInfinite(v.x) && !Float.isInfinite(v.y) && !Float.isInfinite(v.z);
    }

    private static String formatPosition(Vector3f v) {
        return String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f", v.x, v.y, v.z);
    }

    private void processPendingTileCaptures() {
        if (tileCapturer != null) {
            //the world map must capture the bare terrain - no grass shells
            GpuGrassMaterial.setLayersGloballyEnabled(false);
            try {
                tileCapturer.processPendingSubTileCaptures();  // Process one queued tile per frame
            } finally {
                GpuGrassMaterial.setLayersGloballyEnabled(true);
            }
        }
    }
    
    @Override
    public int getRigidBodiesSize() {
        return rigidBodiesSize;
    }

    @Override
    public TileTextureCapturer getTileCapturer() {
        return tileCapturer;
    }
}
