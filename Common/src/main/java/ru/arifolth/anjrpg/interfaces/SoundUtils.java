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

import com.google.common.collect.Iterables;
import com.jme3.audio.AudioSource;

import java.util.Collection;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoundUtils {
    private static final Random rnd = new Random();
    final private static Logger LOGGER = Logger.getLogger(SoundUtils.class.getName());

    private SoundUtils() {
    }

    public static <T> T getRandomObject(Collection<T> from) {
        int i = rnd.nextInt(from.size());
        return Iterables.get(from, i);
    }

    public static <T> T getSingleObject(Collection<T> from) {
        return Iterables.get(from, 0);
    }
}
