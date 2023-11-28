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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;
import jme3tools.optimize.GeometryBatchFactory;

public enum GrassTypeEnum implements GrassType {
    REGULAR {
        Node grassNode = null;

        @Override
        public void init() {
            Vector2f windDirection = new Vector2f();
            windDirection.x = Utils.nextFloat();
            windDirection.y = Utils.nextFloat();
            windDirection.normalize();

            Geometry grassGeometry = new Geometry("grass", new Quad(2, 2));

            Material grassShader = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            Texture grass = assetManager.loadTexture("Textures/Grass/grass_3.png");
            grass.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture normalMap = assetManager.loadTexture("Textures/Grass/grass_3_normal_map.png");
            normalMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture specularMap = assetManager.loadTexture("Textures/Grass/grass_3_specular.png");
            specularMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            grassShader.setColor("Diffuse", ColorRGBA.White);
            grassShader.setColor("Ambient", ColorRGBA.White);
            grassShader.setColor("Specular", ColorRGBA.White);
            grassShader.setTexture("DiffuseMap", grass);
            grassShader.setTexture("NormalMap", normalMap);
            grassShader.setTexture("ParallaxMap", specularMap);
            grassShader.setTexture("SpecularMap", specularMap);
            grassShader.setBoolean("UseMaterialColors", true);
            grassShader.setBoolean("HardwareShadows", true);
            grassShader.setBoolean("SteepParallax", true);
            grassShader.setBoolean("BackfaceShadows", true);
            grassShader.setFloat("AlphaDiscardThreshold", 0.5f);
            grassShader.setFloat("Shininess", 0f);
            grassShader.getAdditionalRenderState().setDepthTest(true);
            grassShader.getAdditionalRenderState().setDepthWrite(true);
            grassShader.getAdditionalRenderState().setColorWrite(true);
            grassShader.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            grassShader.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            grassGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            grassGeometry.setMaterial(grassShader);
            grassGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);
            grassGeometry.rotate(0, 0.58f, 0);
            grassGeometry.center();

            Node grassBladeNode = new Node();
            grassBladeNode.attachChild(grassGeometry);

            grassGeometry = grassGeometry.clone();
            grassGeometry.rotate(0, 1.58f, 0);
            grassGeometry.center();
            grassBladeNode.attachChild(grassGeometry);

            grassBladeNode.move(0, 1f, 0);

            LodUtils.setUpModelLod(grassBladeNode);
            grassBladeNode = GeometryBatchFactory.optimize(grassBladeNode, true);
            TangentBinormalGenerator.generate(grassBladeNode, true);
            grassBladeNode.updateModelBound();

            grassNode = grassBladeNode;
        }

        @Override
        public Node getGrass() {
            return (Node) grassNode.clone();
        }
    };

    private static AssetManager assetManager;

    public static void setAssetManager(AssetManager assetManager) {
        GrassTypeEnum.assetManager = assetManager;
    }
}
