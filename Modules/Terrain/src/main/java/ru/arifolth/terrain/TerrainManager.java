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

package ru.arifolth.terrain;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.terrain.geomipmap.TerrainQuad;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.vegetation.BushTypeEnum;
import ru.arifolth.vegetation.GrassTypeEnum;
import ru.arifolth.vegetation.MushroomTypeEnum;
import ru.arifolth.vegetation.TreeTypeEnum;

import java.util.EnumSet;
import java.util.logging.Logger;

public class TerrainManager implements TerrainManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(TerrainManager.class.getName());
    private FractalTerrainGridInterface terrainBuilder;

    private TerrainQuad terrain;
    private final AssetManager assetManager;
    private final BulletAppState bulletAppState;
    private final RolePlayingGameInterface app;

    public TerrainManager(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;

        terrainBuilder = new FractalTerrainGrid(assetManager, bulletAppState, app);

        TreeTypeEnum.setAssetManager(assetManager);
        GrassTypeEnum.setAssetManager(assetManager);
        BushTypeEnum.setAssetManager(assetManager);
        MushroomTypeEnum.setAssetManager(assetManager);
    }

    @Override
    public void initialize() {
        terrainBuilder.initialize();

        EnumSet.allOf(TreeTypeEnum.class).forEach(TreeTypeEnum::init);
        EnumSet.allOf(GrassTypeEnum.class).forEach(GrassTypeEnum::init);
        EnumSet.allOf(BushTypeEnum.class).forEach(BushTypeEnum::init);
        EnumSet.allOf(MushroomTypeEnum.class).forEach(MushroomTypeEnum::init);

        generateTerrain();
    }

    private void generateTerrain() {
        terrain = terrainBuilder.generateTerrain();
    }

    public TerrainQuad getTerrain() {
        return terrain;
    }

    @Override
    public int getRigidBodiesSize() {
        return terrainBuilder.getRigidBodiesSize();
    }

    public void update(float tpf) {
        terrainBuilder.update();
    }

    @Override
    public FractalTerrainGridInterface getTerrainBuilder() {
        return terrainBuilder;
    }
}
