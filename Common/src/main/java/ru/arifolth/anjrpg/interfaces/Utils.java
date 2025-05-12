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

import com.google.common.collect.Iterables;
import com.jme3.bullet.control.CharacterControl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    private Utils() {
    }

    public static boolean getRandom(int probability) {
        if (probability <= 0) {
            return false;
        }
        if (probability >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < probability;
    }


    public static float getRandomNumberInRange(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return min + (max - min) * ThreadLocalRandom.current().nextFloat();
    }

    public static void enableEntityPhysics(CharacterInterface character) {
        CharacterControl characterControl = character.getCharacterControl();
        characterControl.getControllerId().setMaxJumpHeight(10f);
//        characterControl.setJumpSpeed(10);
        characterControl.setFallSpeed(55);
        characterControl.setGravity(9.8f * 3);
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public static <V> V getRandomObject(Collection<V> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        int size = values.size();
        int i = ThreadLocalRandom.current().nextInt(size);
        if (values instanceof List) {
            return ((List<V>) values).get(i);
        } else {
            int index = 0;
            for (V item : values) {
                if (index++ == i) {
                    return item;
                }
            }
            throw new AssertionError("Index out of bounds");
        }
    }

    public static <T> T getSingleObject(Collection<T> from) {
        return Iterables.get(from, 0);
    }
}
