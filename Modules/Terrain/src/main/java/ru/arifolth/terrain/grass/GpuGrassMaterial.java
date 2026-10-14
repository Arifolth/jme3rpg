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

package ru.arifolth.terrain.grass;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.Constants;

import java.util.List;
import java.util.logging.Logger;

/**
 * GPU layered-shell grass material, ported from the jME 3.1 - 3.7 era
 * jme3-grass-shader project (LayeredMaterial) to jMonkeyEngine 3.9.0-stable.
 *
 * How it works: the terrain geometry is rendered once with the base terrain
 * material, then re-rendered once per grass layer. Every layer is displaced
 * vertically by the vertex shader and masked by a 3D texture in the fragment
 * shader, which produces dense, wind-swaying grass.
 *
 * jME 3.9 compatibility notes (the reasons the original code silently failed):
 *  - RenderManager now dispatches through
 *    {@link Material#render(Geometry, LightList, RenderManager)}; overriding
 *    the old render(Geometry, RenderManager) signature is dead code, so this
 *    class overrides the current one instead;
 *  - grass rendering is centered on the player position (updated every frame)
 *    and the number of rendered layers is reduced with distance
 *    (adaptive layer LOD);
 *  - {@link #layersGloballyEnabled} allows to temporarily turn the grass
 *    layers off (used by the world map tile capturer, which must capture
 *    the bare terrain);
 *  - IMPORTANT: jME 3.9 TerrainGrid CLONES the material for every attached
 *    tile (TerrainGrid: "if (q.getMaterial() == null) q.setMaterial(material.clone())").
 *    FractalTerrainGrid.update() only reaches the original instance, so every
 *    piece of per-frame state the shaders need (player position, wind time,
 *    the initialized flag) lives in STATIC fields and is pushed into each
 *    clone's own uniforms right before rendering. Instance state that is not
 *    refreshed per frame would silently stay at whatever it was when the
 *    tile was attached - grass would then only appear on tiles attached
 *    after the first update() call (this was the "grass appears late" bug).
 */
public class GpuGrassMaterial extends Material {

    private static final Logger LOGGER = Logger.getLogger(GpuGrassMaterial.class.getName());

    /**
     * Global kill-switch for the grass layer passes. When false, only the
     * base terrain material is rendered. Used by the world map tile capturer.
     */
    private static volatile boolean layersGloballyEnabled = true;

    //--- Per-frame state, SHARED by all per-tile material clones ------------
    //TerrainGrid clones this material for every attached tile; update() is
    //only ever called on the original instance held by FractalTerrainGrid,
    //so this state must be static to reach the clones as well.
    /** Player world position - center of the grass circle. */
    private static final com.jme3.math.Vector3f sharedPlayerPosition = new com.jme3.math.Vector3f();
    /** Total game time, drives the wind animation. */
    private static volatile float sharedTimeSeconds = 0f;
    /** True once update() has run at least once (uniforms are then valid). */
    private static volatile boolean sharedInitialized = false;
    /** 1 original instance + one clone per attached terrain tile. */
    private static volatile int instancesCreated = 0;
    //------------------------------------------------------------------------

    private final int maxLayers;
    private int numLayers;
    private List<Texture> layerMasks;
    private Material baseMaterial;
    private float grassDistance;
    private float fadeStart;
    private final com.jme3.math.Vector3f playerPositionVector = new com.jme3.math.Vector3f();

    //Diagnostics: static counters merge the dispatches of ALL material clones
    //into one throttled summary line (checked against a wall-clock interval,
    //never per frame)
    private final boolean debugEnabled = Constants.GPU_GRASS_DEBUG;
    private static long debugLastLogNanos = 0L;
    private static boolean debugFirstDispatchLogged = false;
    private static int dbgDispatches, dbgBaseRenders, dbgLayerRenders, dbgPatches,
                dbgSkippedNotInit, dbgSkippedDistance, dbgSkippedNoTechnique,
                dbgSkippedDisabled, dbgSkippedForcedTech;

    public GpuGrassMaterial(AssetManager am, int numLayers, float grassDistance) {
        super(am, "MatDefs/Grass/Layered.j3md");
        instancesCreated++;
        this.maxLayers = numLayers;
        this.numLayers = numLayers;
        this.grassDistance = grassDistance;
        this.fadeStart = grassDistance * Constants.GPU_GRASS_FADE_START_RATIO;

        setFloat("GrassDistance", grassDistance);
        setFloat("FadeStart", fadeStart);
        setFloat("Length", Constants.GPU_GRASS_LENGTH);
        setFloat("WaveSpeed", Constants.GPU_GRASS_WAVE_SPEED);
        setFloat("WaveSize", Constants.GPU_GRASS_WAVE_SIZE);
        setFloat("MaskScale", Constants.GPU_GRASS_MASK_SCALE);
        setFloat("WaterLevel", Constants.WATER_LEVEL_HEIGHT + Constants.GPU_GRASS_SHORE_MARGIN);
        setFloat("GrassMaxHeight", Constants.GPU_GRASS_MAX_HEIGHT);
        setFloat("MinGroundUp", Constants.GPU_GRASS_MIN_GROUND_UP);
        setFloat("AllowOnUntextured", Constants.GPU_GRASS_ALLOW_ON_UNTEXTURED ? 1f : 0f);
        setFloat("GrassShareFade", Constants.GPU_GRASS_TEXTURE_SHARE_FADE);
        setFloat("AlphaCutoff", Constants.GPU_GRASS_ALPHA_CUTOFF);
        setBoolean("Wave", true);
        setVector3("PlayerPosition", playerPositionVector);
        setFloat("DebugForceAll", Constants.GPU_GRASS_DEBUG_FORCE_ALL ? 1f : 0f);
        setFloat("DebugBrightness", Constants.GPU_GRASS_DEBUG_BRIGHTNESS);
    }

    public static boolean isLayersGloballyEnabled() {
        return layersGloballyEnabled;
    }

    public static void setLayersGloballyEnabled(boolean enabled) {
        layersGloballyEnabled = enabled;
    }

    /**
     * Accepts the base terrain material and synchronizes the HeightBasedTerrain
     * parameters the grass shader needs to replicate the terrain texturing:
     * terrainSize (UV normalization) and the region1/2/3 height ranges
     * (grass/dirt/rock) with their texture scales. Reading them from the base
     * material (instead of duplicating constants) guarantees that the grass
     * placement always follows exactly what the terrain shader paints,
     * even if the region setup in FractalTerrainGrid changes later.
     */
    public void setBaseMaterial(Material baseMaterial) {
        this.baseMaterial = baseMaterial;

        Float terrainSize = floatParam(baseMaterial, "terrainSize");
        if (terrainSize != null) {
            setFloat("TerrainSize", terrainSize);
        }

        com.jme3.math.Vector3f region1 = vector3Param(baseMaterial, "region1");
        if (region1 != null) {
            //region1 = (startHeight, endHeight, textureScale) of the GRASS texture
            setFloat("GrassRegionMin", region1.x);
            setFloat("GrassRegionMax", region1.y);
            setFloat("GrassUVScale", region1.z);
        }

        com.jme3.math.Vector3f region2 = vector3Param(baseMaterial, "region2");
        if (region2 != null) {
            //region2 = DIRT texture range
            setFloat("DirtRegionMin", region2.x);
            setFloat("DirtRegionMax", region2.y);
        }

        com.jme3.math.Vector3f region3 = vector3Param(baseMaterial, "region3");
        if (region3 != null) {
            //region3/region4 = ROCK texture range (both counted, like the shader does)
            setFloat("RockRegionMin", region3.x);
            setFloat("RockRegionMax", region3.y);
        }
    }

    private static Float floatParam(Material material, String name) {
        if (material == null || material.getParam(name) == null) {
            return null;
        }
        Object value = material.getParam(name).getValue();
        return (value instanceof Float) ? (Float) value : null;
    }

    private static com.jme3.math.Vector3f vector3Param(Material material, String name) {
        if (material == null || material.getParam(name) == null) {
            return null;
        }
        Object value = material.getParam(name).getValue();
        return (value instanceof com.jme3.math.Vector3f) ? (com.jme3.math.Vector3f) value : null;
    }

    public void setGrassTexture(Texture grassTexture) {
        setTexture("GrassTexture", grassTexture);
    }

    public void setThreeDTexture(Texture threeDTexture) {
        setTexture("ThreeDTex", threeDTexture);
    }

    public void setLayerMasks(List<Texture> layerMasks) {
        this.layerMasks = layerMasks;
    }

    public void setNumLayers(int numLayers) {
        this.numLayers = (int) FastMath.clamp(numLayers, 0, maxLayers);
    }

    public int getNumLayers() {
        return numLayers;
    }

    public float getGrassDistance() {
        return grassDistance;
    }

    public void setGrassDistance(float dist) {
        this.grassDistance = dist;
        this.fadeStart = dist * Constants.GPU_GRASS_FADE_START_RATIO;
        setFloat("GrassDistance", dist);
        setFloat("FadeStart", fadeStart);
    }

    /**
     * Must be called every frame before rendering: feeds the shader with the
     * total game time (wind animation) and the player world position (center
     * of the grass circle).
     */
    public void update(float totalTimeSeconds, com.jme3.math.Vector3f playerWorldPosition) {
        //Only writes the SHARED state: this instance is the original kept by
        //FractalTerrainGrid, while the actually-rendered materials are the
        //per-tile clones - they pick the state up in render().
        sharedTimeSeconds = totalTimeSeconds;
        sharedPlayerPosition.set(playerWorldPosition);
        sharedInitialized = true;
    }

    /**
     * Adaptive layer LOD: patches right around the player get the full layer
     * stack, distant patches get a reduced stack. This keeps the draw call and
     * vertex load manageable for a 500 units grass radius.
     */
    private int adaptiveLayerCount(float distanceToEdge) {
        float t = distanceToEdge / grassDistance;
        if (t <= Constants.GPU_GRASS_FULL_LAYERS_RATIO) {
            return numLayers;
        }
        float ratio = 1f - ((t - Constants.GPU_GRASS_FULL_LAYERS_RATIO)
                / (1f - Constants.GPU_GRASS_FULL_LAYERS_RATIO))
                * (1f - Constants.GPU_GRASS_MIN_LAYERS_RATIO);
        return Math.max(Constants.GPU_GRASS_MIN_LAYERS, (int) (numLayers * ratio));
    }

    /**
     * jME 3.9 render entry point. Renders the base terrain material first,
     * then the grass layers for geometries inside the grass radius.
     */
    @Override
    public void render(Geometry geometry, LightList lights, RenderManager renderManager) {
        if (debugEnabled) {
            debugTick();
        }

        if (renderManager.getForcedTechnique() != null) {
            //A forced-technique pass is in progress (SSAO normal pass, etc.):
            //render a single plain pass with the forced technique - no base
            //material and no grass layers must leak into that buffer
            dbgSkippedForcedTech++;
            super.render(geometry, lights, renderManager);
            return;
        }

        //First, render the base material (the terrain itself)
        if (baseMaterial != null) {
            baseMaterial.render(geometry, lights, renderManager);
            dbgBaseRenders++;
        }

        if (numLayers <= 0 || !layersGloballyEnabled) {
            dbgSkippedDisabled++;
            return;
        }

        BoundingVolume bound = geometry.getWorldBound();
        if (bound == null || !sharedInitialized) {
            dbgSkippedNotInit++;
            return;
        }

        //Push the shared per-frame state into THIS instance's uniforms (each
        //TerrainGrid tile owns a material clone with its own uniform storage)
        playerPositionVector.set(sharedPlayerPosition);
        setVector3("PlayerPosition", playerPositionVector);
        setFloat("Time", sharedTimeSeconds);

        //distance from the player to the closest edge of this terrain patch;
        //0 when the player stands on the patch
        float distFromPlayer = bound.distanceToEdge(playerPositionVector);
        if (distFromPlayer >= grassDistance) {
            dbgSkippedDistance++;
            return;
        }

        if (getActiveTechnique() == null) {
            //no technique selected yet - the plain render below selects it;
            //grass layers start on the next frame automatically
            dbgSkippedNoTechnique++;
            super.render(geometry, lights, renderManager);
            return;
        }

        int layersForGeometry = adaptiveLayerCount(distFromPlayer);
        int curDiffLayer = 0;
        int layerChangeInterval = (layerMasks == null || layerMasks.isEmpty())
                ? Integer.MAX_VALUE
                : (int) FastMath.ceil(numLayers / (float) layerMasks.size());

        dbgPatches++;

        //Main layer render loop: render the same geometry once per layer
        for (int i = 1; i <= layersForGeometry; i++) {
            float layer = i / (float) layersForGeometry;

            //Move to the next layer mask if several 2D masks are in use
            //(with the 3D texture path the masks are not sampled)
            if (i % layerChangeInterval == 0 && layerMasks != null && !layerMasks.isEmpty()) {
                setTexture("GrassMask", layerMasks.get(Math.min(curDiffLayer, layerMasks.size() - 1)));
                curDiffLayer++;
            }

            //Set the layer and pass to the superclass to render the layer
            setFloat("Layer", layer);
            super.render(geometry, lights, renderManager);
            dbgLayerRenders++;
        }
        setFloat("Layer", 0);

        if (debugEnabled && !debugFirstDispatchLogged) {
            debugFirstDispatchLogged = true;
            Object threeDTex = getParam("ThreeDTex") == null ? null : getParam("ThreeDTex").getValue();
            Object grassTex = getParam("GrassTexture") == null ? null : getParam("GrassTexture").getValue();
            LOGGER.info("GPU grass FIRST dispatch OK: technique=" + getActiveTechnique().getDef().getName()
                    + " layersForGeometry=" + adaptiveLayerCount(distFromPlayer)
                    + "/" + numLayers
                    + " distToPatchEdge=" + String.format("%.1f", distFromPlayer)
                    + " ThreeDTex=" + (threeDTex != null ? threeDTex : "NOT SET")
                    + " GrassTexture=" + (grassTex != null ? grassTex : "NOT SET")
                    + " forceAll=" + Constants.GPU_GRASS_DEBUG_FORCE_ALL
                    + " materialInstances=" + instancesCreated);
        }
    }

    /**
     * Throttled diagnostics: prints one summary line per
     * GPU_GRASS_DEBUG_LOG_INTERVAL_MS with the per-guard skip counters, so a
     * silent guard ("why is there no grass?") becomes visible in the log
     * without spamming one line per frame.
     */
    private void debugTick() {
        dbgDispatches++;

        long now = System.nanoTime();
        if (debugLastLogNanos == 0L) {
            debugLastLogNanos = now;
            return;
        }
        long intervalNanos = Constants.GPU_GRASS_DEBUG_LOG_INTERVAL_MS * 1_000_000L;
        if (now - debugLastLogNanos < intervalNanos) {
            return;
        }
        debugLastLogNanos = now;

        LOGGER.info(String.format(
                "GPU grass state: dispatches=%d patchesWithLayers=%d layerRenders=%d baseRenders=%d"
                        + " skipped[notInitialized=%d tooFar=%d noTechnique=%d layersDisabled=%d forcedTech=%d]"
                        + " player=(%.0f, %.0f, %.0f) initialized=%b layers=%d/%d materialInstances=%d",
                dbgDispatches, dbgPatches, dbgLayerRenders, dbgBaseRenders,
                dbgSkippedNotInit, dbgSkippedDistance, dbgSkippedNoTechnique,
                dbgSkippedDisabled, dbgSkippedForcedTech,
                sharedPlayerPosition.x, sharedPlayerPosition.y, sharedPlayerPosition.z,
                sharedInitialized, numLayers, maxLayers, instancesCreated));

        dbgDispatches = dbgBaseRenders = dbgLayerRenders = dbgPatches = 0;
        dbgSkippedNotInit = dbgSkippedDistance = dbgSkippedNoTechnique = 0;
        dbgSkippedDisabled = dbgSkippedForcedTech = 0;
    }

    /**
     * Adjusts the number of layers, if possible. Fewer layers means less detail.
     */
    public void adjustLayers(boolean less) {
        if (less && numLayers > Constants.GPU_GRASS_MIN_LAYERS) {
            setNumLayers(numLayers - 1);
        } else if (!less && numLayers < maxLayers) {
            setNumLayers(numLayers + 1);
        }
    }
}
