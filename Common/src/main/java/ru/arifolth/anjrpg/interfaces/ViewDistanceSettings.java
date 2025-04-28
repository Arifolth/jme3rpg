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
        int patchSize = 65;
        int terrainSize = 513;
        float lodMultiplier = 3.5f;
        int localTranslation = -100;
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
    },
    MED {
        int patchSize = 257;
        int terrainSize = 513;
        float lodMultiplier = 2.7f;
        int localTranslation = -100;
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
    },
    HIGH {
        int patchSize = 513;
        int terrainSize = 1025;
        float lodMultiplier = 1.7f;
        int localTranslation = -100;
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
    };

}
