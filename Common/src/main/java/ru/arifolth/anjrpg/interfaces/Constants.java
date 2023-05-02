/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

    public static final int NPC_AMOUNT = 1;
    public static final int NPC_LOCATION_RANGE = 250;

    public static final int WATER_LEVEL_HEIGHT = -70;

    public static final int MODEL_ADJUSTMENT = 3;

    public static final float SOUND_VOLUME = 3f;
    public static final int MUSIC_VOLUME_MULTIPLIER = 6;

    public static final float SOUND_PITCH = 0.5f;
    public static final SSAOFilter SSAO_FILTER_BASIC = new SSAOFilter(1f, 1.5f, 5.8f, 0.9f);
    public static final String DEBUG = "Debug";
    public static final String POM_XML = "pom.xml";
    public static final String ROOT_LOGGER = "";
    public static final Level LOGGING_LEVEL = Level.INFO;
    public static final int MIN_FRAME_RATE = 30;
    public static final int MOUNTAINS_HEIGHT_OFFSET = -250;
    public static final float TERRAIN_SCALE_X = 2f;
    public static final float TERRAIN_SCALE_Y = 1f;
    public static final float TERRAIN_SCALE_Z = 2f;
    public static final Vector3f RAY_DOWN = new Vector3f(0, -1, 0);
    public static final int HIT_PROBABILITY = 50;
    public static final float SHOOT_DELAY = 3f;
    public static final float SHOOT_RATE = 3.5f;
    public static final MusicType NULL = null;
    public static final int STARS_COUNT = 500;
    public static final int DISTANCE_TO_STARS = 9000;
    public static final String VERSION_PROPERTIES = "version.properties";
    public static float MELEE_DISTANCE_LIMIT = 15f;

    //HKEY_CURRENT_USER\Software\JavaSoft\Prefs\

    private Constants() {
        //NO OP CTOR
    }

}
