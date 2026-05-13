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

import ru.arifolth.anjrpg.interfaces.graphics.ViewDistanceSettingsInterface;

public enum ViewDistanceSettings implements ViewDistanceSettingsInterface {
    //smaller
    //17/513/3.5/-125
    //med
    //129/513/2.7/-75
    //hi
    //17/1025/2.7
    //129/1025/1.5

    LOW {
        final int framesThrottle = 1;
        final int patchSize = 65;
        final int terrainSize = 513;
        final float lodMultiplier = 3.5f;
        final int localTranslation = -100;
        @Override
        public int getPatchSize() {
            return patchSize;
        }

        @Override
        public int getTerrainSize() {
            return terrainSize;
        }

        @Override
        public int getLocalTranslation() {
            return localTranslation;
        }

        @Override
        public float getLodMultiplier() {
            return lodMultiplier;
        }

        @Override
        public int getFramesThrottle() {
            return framesThrottle;
        }
    },
    MED {
        final int framesThrottle = 2;
        final int patchSize = 257;
        final int terrainSize = 513;
        final float lodMultiplier = 2.7f;
        final int localTranslation = -100;
        @Override
        public int getPatchSize() {
            return patchSize;
        }

        @Override
        public int getTerrainSize() {
            return terrainSize;
        }

        @Override
        public int getLocalTranslation() {
            return localTranslation;
        }

        @Override
        public float getLodMultiplier() {
            return lodMultiplier;
        }

        @Override
        public int getFramesThrottle() {
            return framesThrottle;
        }
    },
    HIGH {
        final int framesThrottle = 8;
        final int patchSize = 65;
        final int terrainSize = 1025;
        final float lodMultiplier = 1.2f;
        final int localTranslation = -100;
        @Override
        public int getPatchSize() {
            return patchSize;
        }

        @Override
        public int getTerrainSize() {
            return terrainSize;
        }

        @Override
        public int getLocalTranslation() {
            return localTranslation;
        }

        @Override
        public float getLodMultiplier() {
            return lodMultiplier;
        }

        @Override
        public int getFramesThrottle() {
            return framesThrottle;
        }
    };

}
