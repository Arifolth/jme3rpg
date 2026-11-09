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
 * How the grass is drawn: the base terrain material renders the ground
 * first, then the same geometry is rendered once per grass shell (Layer =
 * 1..n). GpuGrassMaterial pushes the shared per-frame state (player
 * position, wind time) and the tuning uniforms into every per-tile material
 * clone right before rendering, and derives the per-patch shell prefix from
 * the continuous shell-budget LOD line (updateLodUniforms /
 * adaptiveLayerCount). The forced "PostShadow" technique dispatches to
 * renderPostShadowPass, which mirrors the main pass for PSSM shadow
 * receiving.
 *
 * jME 3.9 integration notes:
 *  - RenderManager dispatches through
 *    {@link Material#render(Geometry, LightList, RenderManager)}; overriding
 *    the old render(Geometry, RenderManager) signature would be dead code,
 *    so this class overrides the current one instead;
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
 *    tile was attached.
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
    /** Live sun direction (towards the sun), pushed by FilterManager every frame. */
    private static final com.jme3.math.Vector3f sharedSunDirection = new com.jme3.math.Vector3f(0f, 1f, 0f);
    /**
     * CPU-computed glow axis for the backlight translucency: the sun
     * direction with the elevation clamped to
     * Constants.GPU_GRASS_TRANSLUCENCY_SUN_ELEVATION_FLOOR (a high noon sun
     * can never produce a backlit response) plus the distortion bias, applied
     * to the ALIGNMENT only - the true sun keeps driving shadows, sky and
     * water. Computed once per frame for all material clones.
     */
    private static final com.jme3.math.Vector3f sharedGlowAxis = new com.jme3.math.Vector3f(0f, 1f, 0f);
    /**
     * Effective glow strength: Constants.GPU_GRASS_TRANSLUCENCY_STRENGTH
     * scaled by a day-night fade (0 while the sun is at/below the horizon,
     * so the glow never fires at night).
     */
    private static volatile float sharedGlowStrength = 0f;
    /** One-time INFO line that confirms the translucency wiring is live. */
    private static boolean translucencyStateLogged = false;
    //------------------------------------------------------------------------

    private final int maxLayers;
    private int numLayers;
    private List<Texture> layerMasks;
    private Material baseMaterial;
    private float grassDistance;
    private float fadeStart;
    private final com.jme3.math.Vector3f playerPositionVector = new com.jme3.math.Vector3f();
    //Per-instance uniform carriers for the backlight translucency (each
    //TerrainGrid tile owns a material clone with its own uniform storage;
    //the values are refreshed from the static state on every main-pass render).
    private final com.jme3.math.Vector3f glowAxisVector = new com.jme3.math.Vector3f();
    private final com.jme3.math.Vector3f glowColorVector = new com.jme3.math.Vector3f(
            Constants.GPU_GRASS_TRANSLUCENCY_COLOR.x,
            Constants.GPU_GRASS_TRANSLUCENCY_COLOR.y,
            Constants.GPU_GRASS_TRANSLUCENCY_COLOR.z);

    //Diagnostics: static counters merge the dispatches of ALL material clones
    //into one throttled summary line (checked against a wall-clock interval,
    //never per frame). The summary line is emitted ONLY when
    //Constants.GPU_GRASS_DEBUG is true: if the grass ever silently
    //disappears, switching the flag on makes this line name the guard that
    //skips it (tooFar / layersDisabled / notInitialized / ...) instead of
    //leaving the failure undiagnosable from the outside. Normal play with
    //the flag off pays no logging cost at all.
    private static long debugLastLogNanos = 0L;
    private static boolean debugFirstDispatchLogged = false;
    private static int dbgDispatches, dbgBaseRenders, dbgLayerRenders, dbgPatches,
                dbgSkippedNotInit, dbgSkippedDistance, dbgSkippedNoTechnique,
                dbgSkippedDisabled, dbgSkippedForcedTech;

    //Continuous shell-budget LOD line: budget(d) = lodA - lodB * d (world
    //units from the player). Pushed to the fragment shaders
    //(LodA/LodB/MaxLayers) and used to derive the per-patch shell prefix.
    //See Layered.frag + updateLodUniforms().
    private float lodA;
    private float lodB;

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
        //Wind sway only inside the full-layers circle: the amplitude fades
        //out BEFORE the border and is exactly zero from
        //GrassDistance * GPU_GRASS_FULL_LAYERS_RATIO outwards. The
        //max(1f, ...) keeps the smoothstep edges strictly ordered (no NaN).
        float swayEnd = grassDistance * Constants.GPU_GRASS_FULL_LAYERS_RATIO;
        setFloat("SwayFadeEnd", swayEnd);
        setFloat("SwayFadeStart",
                swayEnd - Math.max(1f, grassDistance * Constants.GPU_GRASS_SWAY_FADE_RATIO));
        setFloat("MaskScale", Constants.GPU_GRASS_MASK_SCALE);
        setFloat("WaterLevel", Constants.WATER_LEVEL_HEIGHT + Constants.GPU_GRASS_SHORE_MARGIN);
        setFloat("ShoreBand", Constants.GPU_GRASS_SHORE_BAND);
        //field shape: edge sink
        setFloat("EdgeHeightStart", grassDistance * Constants.GPU_GRASS_EDGE_HEIGHT_START_RATIO);
        setFloat("GrassMaxHeight", Constants.GPU_GRASS_MAX_HEIGHT);
        setFloat("MinGroundUp", Constants.GPU_GRASS_MIN_GROUND_UP);
        setFloat("AllowOnUntextured", Constants.GPU_GRASS_ALLOW_ON_UNTEXTURED ? 1f : 0f);
        setFloat("GrassShareFade", Constants.GPU_GRASS_TEXTURE_SHARE_FADE);
        setFloat("AlphaCutoff", Constants.GPU_GRASS_ALPHA_CUTOFF);
        //Tone pairing, world-anchored blade mask, distance density culling
        setFloat("ToneMatch", Constants.GPU_GRASS_TONE_MATCH);
        setFloat("MaskWorldSpan", Constants.GPU_GRASS_MASK_WORLD_SPAN);
        setFloat("DensityStart", Constants.GPU_GRASS_DENSITY_START);
        setFloat("DensityEnd", grassDistance * Constants.GPU_GRASS_DENSITY_END_RATIO);
        setFloat("DensityMin", Constants.GPU_GRASS_DENSITY_MIN);
        setBoolean("Wave", true);
        //Continuous shell-budget LOD uniforms
        updateLodUniforms();
        setVector3("PlayerPosition", playerPositionVector);
        setFloat("DebugForceAll", Constants.GPU_GRASS_DEBUG_FORCE_ALL ? 1f : 0f);
        setFloat("DebugBrightness", Constants.GPU_GRASS_DEBUG_BRIGHTNESS);
    }

    /**
     * PssmShadowRenderer receive pass (forced technique "PostShadow").
     *
     * Draw order and layer values mirror the main pass exactly:
     *  - Layer 0 (base): the undisplaced, unmasked ground quad. Darkens the
     *    bare ground between the blades exactly once (where a blade owns the
     *    depth buffer, the base fragment fails the depth test instead).
     *  - Layers 1..n: the same shell stack as the main pass. The post shadow
     *    vertex shader replicates the main displacement bit for bit and the
     *    post shadow fragment shader replicates every discard gate, so only
     *    the shell that actually owns a pixel darkens it - no double (or 45x)
     *    darkening, no bright rings.
     */
    private void renderPostShadowPass(Geometry geometry, LightList lights, RenderManager renderManager) {
        //Base (ground) pass: Layer == 0 -> undisplaced + unmasked in the shader
        setFloat("Layer", 0f);
        super.render(geometry, lights, renderManager);
        dbgBaseRenders++;

        if (numLayers <= 0 || !layersGloballyEnabled) {
            return;
        }

        BoundingVolume bound = geometry.getWorldBound();
        if (bound == null || !sharedInitialized) {
            return;
        }

        //Push the shared per-frame state into THIS instance's uniforms so the
        //displacement (wind time, player position) matches the main pass
        playerPositionVector.set(sharedPlayerPosition);
        setVector3("PlayerPosition", playerPositionVector);
        setFloat("Time", sharedTimeSeconds);

        float distFromPlayer = bound.distanceToEdge(playerPositionVector);
        if (distFromPlayer >= grassDistance) {
            //main pass drew no layers here either (fadeFactor would be 0)
            return;
        }

        int layersForGeometry = adaptiveLayerCount(distFromPlayer);
        int curDiffLayer = 0;
        int layerChangeInterval = (layerMasks == null || layerMasks.isEmpty())
                ? Integer.MAX_VALUE
                : (int) FastMath.ceil(numLayers / (float) layerMasks.size());

        //Shell pass loop: same layer values as the main pass
        //(the same FIXED shell grid, Layer = i / numLayers)
        for (int i = 1; i <= layersForGeometry; i++) {
            float layer = i / (float) numLayers;

            //Move to the next layer mask if several 2D masks are in use
            //(with the 3D texture path the masks are not sampled)
            if (i % layerChangeInterval == 0 && layerMasks != null && !layerMasks.isEmpty()) {
                setTexture("GrassMask", layerMasks.get(Math.min(curDiffLayer, layerMasks.size() - 1)));
                curDiffLayer++;
            }

            setFloat("Layer", layer);
            super.render(geometry, lights, renderManager);
            dbgLayerRenders++;
        }
        setFloat("Layer", 0f);
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
        //The shell-budget line depends on numLayers
        updateLodUniforms();
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
        setFloat("DensityEnd", dist * Constants.GPU_GRASS_DENSITY_END_RATIO);
        //Keep the sway border in sync when the distance changes
        float swayEnd = dist * Constants.GPU_GRASS_FULL_LAYERS_RATIO;
        setFloat("SwayFadeEnd", swayEnd);
        setFloat("SwayFadeStart",
                swayEnd - Math.max(1f, dist * Constants.GPU_GRASS_SWAY_FADE_RATIO));
        //The shell-budget line depends on grassDistance
        updateLodUniforms();
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
     * Feeds the live sun direction (towards the sun) - called from
     * FilterManager.update(), which already refreshes the PSSM, water and
     * light scattering directions from the same source every frame.
     *
     * Computes the per-frame glow state for ALL material clones:
     *  - glow axis: sun elevation clamped to the translucency floor plus
     *    the distortion bias (alignment axis only; shadows keep the true
     *    sun), with a degenerate-azimuth fallback for a sun almost straight
     *    overhead;
     *  - glow strength: Constants strength scaled by a day-night fade, so a
     *    below-horizon sun lights nothing.
     * A NaN/Infinity sun direction is ignored (it would poison every grass
     * fragment); the last valid state is kept instead.
     */
    public static void setSharedSunDirection(com.jme3.math.Vector3f sunDirection) {
        if (sunDirection == null) {
            return;
        }
        float x = sunDirection.x;
        float y = sunDirection.y;
        float z = sunDirection.z;
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return;
        }
        sharedSunDirection.set(x, y, z);

        if (!Constants.GPU_GRASS_TRANSLUCENCY) {
            return;
        }

        float clampedY = FastMath.clamp(y,
                Constants.GPU_GRASS_TRANSLUCENCY_SUN_ELEVATION_FLOOR, 1f);
        float horiz = FastMath.sqrt(x * x + z * z);
        if (horiz < 0.05f) {
            //Sun almost straight overhead: fall back to the previous
            //azimuth (or -Z on the very first call) so the glow axis keeps
            //a usable horizontal component instead of collapsing onto the
            //zenith.
            x = sharedGlowAxis.x;
            z = sharedGlowAxis.z;
            horiz = FastMath.sqrt(x * x + z * z);
            if (horiz < 0.05f) {
                x = 0f;
                z = -1f;
            }
        }
        sharedGlowAxis.set(x, clampedY, z).normalizeLocal();
        sharedGlowAxis.addLocal(0f, Constants.GPU_GRASS_TRANSLUCENCY_DISTORTION, 0f)
                .normalizeLocal();

        //Day-night fade: full effect once the true sun is above the
        //elevation floor, smoothly zero at/below the horizon.
        float fade = FastMath.clamp(
                (y + 0.1f) / (Constants.GPU_GRASS_TRANSLUCENCY_SUN_ELEVATION_FLOOR + 0.1f),
                0f, 1f);
        sharedGlowStrength = Constants.GPU_GRASS_TRANSLUCENCY_STRENGTH * fade;

        if (!translucencyStateLogged) {
            translucencyStateLogged = true;
            //One-time confirmation line: if the glow ever shows no visible
            //effect, this line proves the CPU side computes and feeds it.
            LOGGER.info("GPU grass translucency active: axis=("
                    + String.format("%.2f, %.2f, %.2f",
                            sharedGlowAxis.x, sharedGlowAxis.y, sharedGlowAxis.z)
                    + ") strength=" + String.format("%.3f", sharedGlowStrength)
                    + " power=" + Constants.GPU_GRASS_TRANSLUCENCY_POWER
                    + " tipStart=" + Constants.GPU_GRASS_TRANSLUCENCY_TIP_START
                    + " color=" + Constants.GPU_GRASS_TRANSLUCENCY_COLOR);
        }
    }

    /**
     * Recomputes and pushes the shell-budget LOD line:
     * budget(d) = LodA - LodB * d = the number of shells visible at d world
     * units from the player - full numLayers inside the inner LOD circle
     * (GrassDistance * GPU_GRASS_FULL_LAYERS_RATIO), down to
     * numLayers * GPU_GRASS_MIN_LAYERS_RATIO at GrassDistance. Must be
     * called whenever numLayers or grassDistance changes (the budget is
     * defined by both).
     */
    private void updateLodUniforms() {
        float lodStart = grassDistance * Constants.GPU_GRASS_FULL_LAYERS_RATIO;
        float lodSpan = Math.max(1f, grassDistance - lodStart);
        lodB = numLayers * (1f - Constants.GPU_GRASS_MIN_LAYERS_RATIO) / lodSpan;
        lodA = numLayers + lodB * lodStart;
        setFloat("LodA", lodA);
        setFloat("LodB", lodB);
        setFloat("MaxLayers", numLayers);
    }

    /**
     * Adaptive layer LOD: patches render a PREFIX of the FIXED shell grid
     * (Layer = i / numLayers). The fragment shaders fade shell i out
     * continuously where the shell budget crosses its index, so a shell may
     * be skipped only when it is already fully transparent for the WHOLE
     * patch. The bounding-box edge distance is at most the distance of
     * every real fragment of the patch (the box contains the mesh), so the
     * budget at the edge is an upper bound and rounding UP never skips a
     * visible shell; the field thins continuously inside the shaders.
     */
    private int adaptiveLayerCount(float distanceToEdge) {
        float budget = lodA - lodB * distanceToEdge;
        return Math.max(Constants.GPU_GRASS_MIN_LAYERS,
                Math.min(numLayers, (int) Math.ceil(budget - 0.001f)));
    }

    /**
     * jME 3.9 render entry point. Renders the base terrain material first,
     * then the grass layers for geometries inside the grass radius.
     */
    @Override
    public void render(Geometry geometry, LightList lights, RenderManager renderManager) {
        debugTick();

        String forcedTechnique = renderManager.getForcedTechnique();
        if (forcedTechnique != null && !forcedTechnique.equals("PostShadow")) {
            //A forced-technique pass is in progress (SSAO normal pass, etc.):
            //render a single plain pass with the forced technique - no base
            //material and no grass layers must leak into that buffer
            dbgSkippedForcedTech++;
            super.render(geometry, lights, renderManager);
            return;
        }

        if (forcedTechnique != null) {
            //PssmShadowRenderer receive pass: the forced technique switched
            //THIS material to the PostShadow technique. Re-draw the ground
            //and every grass shell exactly like the main pass, outputting
            //the shadow factor (modulate blend) - that is what makes the
            //grass respect shadows cast by trees, the player, houses, ...
            renderPostShadowPass(geometry, lights, renderManager);
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

        //Backlight translucency (main pass only: the PostShadow pass runs
        //AFTER the main pass and modulates the already-glowing pixels with
        //the shadow factor, so the glow respects PSSM shadows for free and
        //needs no extra terms in the post pass).
        if (Constants.GPU_GRASS_TRANSLUCENCY) {
            glowAxisVector.set(sharedGlowAxis);
            setVector3("TransSunH", glowAxisVector);
            setVector3("TransSunColor", glowColorVector);
            setFloat("TransStrength", sharedGlowStrength);
            setFloat("TransPower", Constants.GPU_GRASS_TRANSLUCENCY_POWER);
        }

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

        //Main layer render loop: render the same geometry once per layer.
        //FIXED shell grid - the layer value must not depend on
        //layersForGeometry, otherwise every shell of a reduced patch sits at
        //a different height and samples different mask slices than its full
        //neighbors (patch-shaped detail bands at the inner LOD border).
        //layersForGeometry only trims already-faded shells off the top.
        for (int i = 1; i <= layersForGeometry; i++) {
            float layer = i / (float) numLayers;

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

        if (Constants.GPU_GRASS_DEBUG && !debugFirstDispatchLogged) {
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
     *
     * Active only when Constants.GPU_GRASS_DEBUG is true. With the flag
     * false this method returns immediately, so normal play does not even
     * pay the per-dispatch System.nanoTime() call.
     */
    private void debugTick() {
        if (!Constants.GPU_GRASS_DEBUG) {
            return;
        }
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
