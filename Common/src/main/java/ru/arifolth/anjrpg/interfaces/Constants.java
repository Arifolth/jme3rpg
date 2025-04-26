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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.math.Vector3f;
import com.jme3.post.ssao.SSAOFilter;

import java.util.logging.Level;

public class Constants {
    public static final Vector3f PLAYER_START_LOCATION = new Vector3f(0, 150, 0);

    public static final float DAMAGE = 25f;

    public static final int NPC_AMOUNT = 0;
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
    public static final MusicTypeEnum NULL = null;
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
    public static final float STARS_HEIGHT = 8000f;
    public static float MELEE_DISTANCE_LIMIT = 5.5f;

    //HKEY_CURRENT_USER\Software\JavaSoft\Prefs\

    private Constants() {
        //NO OP CTOR
    }

}
