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

import com.jme3.math.Vector3f;

public class Constants {
    public static final Vector3f PLAYER_START_LOCATION = new Vector3f(0, 150, 0);

    public static final float DAMAGE = 25f;

    public static final int NPC_AMOUNT = 3;
    public static final int NPC_LOCATION_RANGE = 500;

    public static final int WATER_LEVEL_HEIGHT = -70;

    public static final String POM_XML = "pom.xml";

    public static final int MODEL_ADJUSTMENT = 3;

    public static final float SOUND_VOLUME = 3f;
    public static final float SOUND_PITCH = 0.5f;

    private Constants() {
    }

}
