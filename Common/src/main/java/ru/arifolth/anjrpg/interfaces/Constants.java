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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.ssao.SSAOFilter;

import java.util.logging.Level;

public class Constants {
    public static final Vector3f PLAYER_START_LOCATION = new Vector3f(0, 150, 0);

    public static final float DAMAGE = 25f;

    public static final int NPC_AMOUNT = 1;
    public static final int NPC_LOCATION_RANGE = 250;

    public static final int WATER_LEVEL_HEIGHT = -70;

    public static final int MODEL_ADJUSTMENT = 3;

    public static final float SOUND_VOLUME = 0.5f;
    public static final float MUSIC_VOLUME_MULTIPLIER = 0.225f;

    public static final float SOUND_PITCH = 0.5f;
    public static final SSAOFilter SSAO_FILTER_BASIC = new SSAOFilter(1f, 1.5f, 5.8f, 0.9f);
    public static final String DEBUG = "Debug";
    public static final String ROOT_LOGGER = "";
    public static final Level LOGGING_LEVEL = Level.INFO;
    public static final int MIN_FRAME_RATE = 30;
    public static final int HEIGHT_OFFSET = -250;
    public static final int MOUNTAINS_HEIGHT_OFFSET = -4000;
    public static final Vector3f INITIAL_MOUNTAINS_DIRECTION = new Vector3f(MOUNTAINS_HEIGHT_OFFSET, 0f, 0f);
    public static final float TERRAIN_SCALE_X = 2f;
    public static final float TERRAIN_SCALE_Y = 1f;
    public static final float TERRAIN_SCALE_Z = 2f;
    public static final Vector3f RAY_DOWN = new Vector3f(0, -1, 0);
    public static final int HIT_PROBABILITY = 50;
    public static final float SHOOT_DELAY = 3f;
    public static final float SHOOT_RATE = 3.5f;
    public static final int STARS_COUNT = 500;
    public static final int DISTANCE_TO_STARS = 9000;
    public static final String VERSION_PROPERTIES = "version.properties";
    public static final int STENCIL_BITS = 8;
    public static final int RIGID_BODIES_SIZE = 4;
    public static final String QUAD_GRASS = "quadGrass";
    public static final String QUAD_BUSHES = "quadBushes";
    public static final String QUAD_FOREST = "quadForest";
    public static final String QUAD_MUSHROOMS = "quadMushrooms";
    public static final float FIRING_RANGE = 5f;
    public static final float WALKING_RANGE = 500f;
    public static final float LOCATION_FOUND_RANGE = 10f;
    public static final float WALK_SPEED = .3f;
    public static final String GRASS_NODE = "Grass";
    public static final String MUSHROOMS_NODE = "Mushrooms";
    public static final String BUSHES_NODE = "Bushes";
    public static final String FOREST_NODE = "Forest";
    public static final String ENEMIES_NODE = "Enemies";
    public static final float CHANGE_GAME_STATE_TPF = 0.3f;
    public static final Vector3f ZERO_VECTOR3F = new Vector3f();
    public static final float JUMP_COOLDOWN = 1.0f;
    public static final int TREE_PLANTING_RANGE = 1500;
    public static final int TREE_PLANTING_HEIGHT = 70;
    public static final float UI_PADDING = 20f;
    public static final float FRAME_BORDER_WIDTH = 8f;
    public static final float TURN_RATE = FastMath.QUARTER_PI / 0.75f;
    // Full cone angle
    public static final float CONE_DEGREES = 45.0f;
    public static float MELEE_DISTANCE_LIMIT = 5.5f;

    public static final float MAXIMUM_HEALTH = 100f;
    public static final float MAXIMUM_MANA = 100f;
    public static final float MAXIMUM_STAMINA = 100f;

    // Player bars positioning
    public static final float PLAYER_BARS_SPACING = 8f;
    public static final float PLAYER_BARS_LEFT_MARGIN = 20f;
    public static final float PLAYER_BARS_TOP_MARGIN = 20f;

    // Stamina and mana regeneration rates
    public static final float STAMINA_REGENERATION_RATE = 25f; // per second
    public static final float MANA_REGENERATION_RATE = 15f; // per second
    public static final float STAMINA_CONSUMPTION_RUN = 20f; // per second when running
    public static final float STAMINA_CONSUMPTION_JUMP = 15f; // per jump

    // GPU grass (layered shell technique, ported from jme3-grass-shader)
    // Master switch: renders GPU grass shells on top of the terrain
    public static final boolean GPU_GRASS_ENABLED = true;
    // When true, the billboard grass builder is skipped for new terrain tiles
    public static final boolean GPU_GRASS_REPLACES_BILLBOARD_GRASS = true;
    // Number of grass layers (shells). More layers = denser/longer grass but slower
    public static final int GPU_GRASS_LAYERS = 25;
    // Grass is rendered within this radius around the player (~500 feet)
    public static final float GPU_GRASS_DISTANCE = 275f;
    // Grass starts to thin out at this fraction of the grass distance
    public static final float GPU_GRASS_FADE_START_RATIO = 0.65f;
    // Adaptive layer LOD: full layers within this fraction of the radius...
    public static final float GPU_GRASS_FULL_LAYERS_RATIO = 0.35f;
    // ...down to this fraction of layers at the edge, never fewer than MIN
    public static final float GPU_GRASS_MIN_LAYERS_RATIO = 0.25f;
    public static final int GPU_GRASS_MIN_LAYERS = 6;
    // Maximum grass height in world units (blade length). The original demo
    // used 0.4 on an unscaled terrain; 0.8 reads well in this world (2x scale)
    public static final float GPU_GRASS_LENGTH = 0.8f;
    // Wind sway tuning
    public static final float GPU_GRASS_WAVE_SPEED = 4f;
    public static final float GPU_GRASS_WAVE_SIZE = 0.5f;
    // Mask pattern frequency per terrain patch (blade density)
    public static final float GPU_GRASS_MASK_SCALE = 256f;
    // World-space Y safety cap above which grass never grows. The REAL
    // grass/dirt/rock placement is decided by the texture-dominance gate
    // below, which replicates the HeightBasedTerrain region blending - this
    // cap only guards against pathological cases. Terrain heights top out
    // around 128, so 150 keeps the cap out of the way while still catching
    // extreme terrain setups.
    public static final float GPU_GRASS_MAX_HEIGHT = 250f;
    // Minimum world-space up component of the ground normal (1 = flat);
    // ground tilted more than this is considered a cliff and gets no grass.
    // Secondary safety net - the texture-share gate already removes grass on
    // slopes steep enough for the rock slope texture to win.
    public static final float GPU_GRASS_MIN_GROUND_UP = 0.72f;
    // Smooth shoreline transition margin above the water level
    public static final float GPU_GRASS_SHORE_MARGIN = 0.5f;
    // Height above the water level that stays (almost) bare: blades fade in
    // smoothly from zero at the waterline to full density at this height -
    // a Morrowind-style wet shoreline without grass. 0 = grass right up to
    // the water.
    public static final float GPU_GRASS_SHORE_BAND = 10f;
    // ===== Field shape tweak =====
    // Blades start sinking into the ground at this fraction of the grass
    // radius and are fully sunken at the edge: the grass field melts into
    // the ground texture at the circle border instead of ending abruptly.
    // (A noise-patch height variation was tried and REVERTED - it made the
    // field look like a rugged carpet. Blade lengths are uniform.)
    public static final float GPU_GRASS_EDGE_HEIGHT_START_RATIO = 0.45f;
    // ===== Phase 3: texture-aware placement =====
    // The grass shader replicates the exact region weights of the stock
    // HeightBasedTerrain shader (grass/dirt/rock height ranges + slope blend)
    // and grows grass wherever NO other texture outweighs the grass texture:
    //   - dirt region weight > grass weight  -> dirt/sand band: no grass
    //   - rock region weight > grass weight  -> rock band: no grass
    //   - slope-rock share  > grass weight   -> cliffs: no grass
    // BEWARE with THIS game's region setup: region1 = (15, 200) makes the
    // stock formula paint the grass texture only ABOVE height 15 (rising
    // towards 200), region2 = (0, 20) paints dirt only in the 0..40 band and
    // region3/4 = (198, 260) are unreachable. Below height 0 - the shoreline
    // and lowlands the player walks on - the stock shader paints NO region
    // texture at all. The next switch decides whether grass may grow there.
    public static final boolean GPU_GRASS_ALLOW_ON_UNTEXTURED = true;
    // Width of the smooth transition band of the texture-dominance tests:
    // blades thin out gradually over this range while another texture takes
    // over the ground. Higher = softer borders between grass and dirt/rock.
    public static final float GPU_GRASS_TEXTURE_SHARE_FADE = 0.15f;
    // ALPHA CUTOUT threshold (0..1). The grass renders WITHOUT blending and
    // WITH depth write: fragments whose mask alpha is below this value are
    // discarded, everything else is drawn opaque and occupies real depth
    // buffer space. This makes the player, trees and other objects interleave
    // correctly with the grass (some blades in front of the player's legs,
    // some behind). Higher = thinner/harder blades, lower = denser/softer.
    public static final float GPU_GRASS_ALPHA_CUTOFF = 0.35f;
    // Texture used to color the grass shells. DEFAULT = the terrain's own
    // grass region texture (Textures/Terrain/splat/grass.jpg), sampled with
    // the exact UV mapping of the terrain shader - the blade palette is then
    // pixel-identical to the landscape palette. An empty string falls back
    // to the procedural green noise of the original example project.
    public static final String GPU_GRASS_TEXTURE_PATH = "Textures/Terrain/splat/grass.jpg";

    // ===== GPU grass diagnostics (temporary - switch off once grass is confirmed working) =====
    // Throttled INFO logging from GpuGrassMaterial: one summary line per interval,
    // never per frame. Reports exactly which guard skips the grass (if any).
    public static final boolean GPU_GRASS_DEBUG = false;
    // Bypass the ENVIRONMENT gates in the shader (water level, altitude, slope,
    // texture share) - grass renders everywhere inside the player radius,
    // exactly like the original example project. The 3D blade mask is always
    // applied (it is what shapes individual blades; skipping it renders solid
    // walls instead). Leave FALSE for normal play; set to true only to verify
    // the shader pipeline without any environment filtering.
    public static final boolean GPU_GRASS_DEBUG_FORCE_ALL = false;
    // Brightness multiplier applied to the grass shells.
    // 1.0 = EXACTLY the terrain texture palette (recommended now that the
    // shells sample the terrain grass texture anyway). Raise it (e.g. 1.3)
    // only if the grass should stay visible on very dark nights.
    public static final float GPU_GRASS_DEBUG_BRIGHTNESS = 1.0f;
    // Minimal interval between two GPU grass debug log lines (milliseconds)
    public static final long GPU_GRASS_DEBUG_LOG_INTERVAL_MS = 5000;

    //HKEY_CURRENT_USER\Software\JavaSoft\Prefs\

    private Constants() {
        //NO OP CTOR
    }

}
