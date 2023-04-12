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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.bullet.control.CharacterControl;

import java.util.SplittableRandom;

public class Utils {
    private static SplittableRandom random = new SplittableRandom();

    private Utils() {
    }

    public static boolean getRandom(int probability) {
        return random.nextInt(1, 101) <= probability;
    }

    public static float getRandomNumber() {
        return random.nextInt(0, 100);
    }

    public static float getRandomNumberInRange(float min, float max) {
        return (float) random.doubles(min, max).findAny().getAsDouble();
    }

    public static void enableEntityPhysics(CharacterInterface character) {
        CharacterControl characterControl = character.getCharacterControl();
        characterControl.setJumpSpeed(20);
        characterControl.setFallSpeed(300);
        characterControl.setGravity(30);
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }
}
