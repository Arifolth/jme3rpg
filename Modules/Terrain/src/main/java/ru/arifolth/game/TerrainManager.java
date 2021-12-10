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

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.stomrage.grassarea.GrassArea;
import com.stomrage.grassarea.GrassAreaControl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TerrainManager implements TerrainManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(TerrainManager.class.getName());
    private TerrainInterface terrainBuilder;

    private TerrainQuad terrain;

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private RolePlayingGameInterface app;

    public TerrainManager(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;

        terrainBuilder = new FractalTerrainGrid(assetManager, bulletAppState, app);
        generateTerrain();

        getTerrain().setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }

    private void generateTerrain() {
        terrain = terrainBuilder.generateTerrain();
    }

    public void generateGrass(TerrainQuad terrain) {
        GrassArea grassArea = null;
        try {
            grassArea = new GrassArea(terrain, 8, assetManager, 75);
            grassArea.setColorTexture(assetManager.loadTexture("Textures/Grass/tile_1.png"));
            grassArea.setDissolveTexture(assetManager.loadTexture("Textures/Grass/noise.png"));
            grassArea.addDensityMap(assetManager.loadTexture("Textures/Grass/noise.png"));
            grassArea.addDensityMap(assetManager.loadTexture("Textures/Grass/noise_2.png"));
            grassArea.addLayer(0f, 0.5f, 0.75f, GrassArea.ColorChannel.RED_CHANNEL, GrassArea.DensityMap.DENSITY_MAP_1, 2f, 3f);
            grassArea.addLayer(0.5f, 0.5f, 0.75f, GrassArea.ColorChannel.BLUE_CHANNEL, GrassArea.DensityMap.DENSITY_MAP_2, 2f, 3f);
            grassArea.generate();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Grass generation error: ", ex);
        }
        if(null != grassArea) {
            GrassAreaControl grassAreaControl = new GrassAreaControl(this.app.getCamera());
            grassArea.addControl(grassAreaControl);
            grassArea.setAutoUpdate(true);
            this.app.getRootNode().attachChild(grassArea);
        }
    }

    public TerrainQuad getTerrain() {
        return terrain;
    }

    public void update(float tpf) {

    }
}
