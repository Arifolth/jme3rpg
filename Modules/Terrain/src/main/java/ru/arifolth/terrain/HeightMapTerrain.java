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

package ru.arifolth.terrain;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.HeightMapTerrainInterface;
import ru.arifolth.anjrpg.interfaces.RolePlayingGameInterface;

import java.util.logging.Logger;

public class HeightMapTerrain implements HeightMapTerrainInterface {
    final private static Logger LOGGER = Logger.getLogger(HeightMapTerrain.class.getName());

    private Material matTerrain;
    private RigidBodyControl landscape;
    private TerrainQuad terrain;

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private RolePlayingGameInterface app;

    public HeightMapTerrain(AssetManager assetManager, BulletAppState bulletAppState, RolePlayingGameInterface app) {
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.app = app;
    }

    @Override
    public TerrainQuad generateTerrain() {
        /** 1. Create terrain material and load four textures into it. */
        /*matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);*/
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        matTerrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        //matTerrain.setTexture("GrassAlphaMap", assetManager.loadTexture(
        //"Textures/Terrain/grass-map512.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex1", grass);
        matTerrain.setFloat("Tex1Scale", 256f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex2", dirt);
        matTerrain.setFloat("Tex2Scale", 128f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matTerrain.setTexture("Tex3", rock);
        matTerrain.setFloat("Tex3Scale", 512f);


        /** 2. Create the height map */

        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();
        heightmap.smooth(0.9f, 1);

        /** 3. We have prepared material and heightmap.
         * Now we createCharacter the actual terrain:
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */

        int patchSize = 65;
        terrain = new TerrainQuad(
                "my terrain",
                patchSize,
                513,
                heightmap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(matTerrain);
        //terrain.setLocalTranslation(0, -1000, 0);
        //terrain.setLocalScale(2f, 1f, 2f);
        terrain.setLocalTranslation(0, -1150, 0);
        terrain.setLocalScale(20f, 10f, 20f);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, app.getCamera());
        terrain.addControl(control);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) terrain);
        landscape = new RigidBodyControl(sceneShape, 0);
        terrain.addControl(landscape);

        //debug terrain
        //Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        //terrain.generateDebugTangents(debugMat);
        bulletAppState.getPhysicsSpace().add(landscape);

        return terrain;
    }

    @Override
    public TerrainQuad generateMountains() {
        return null;
    }

    @Override
    public void adjustMountainsPosition() {

    }
}
