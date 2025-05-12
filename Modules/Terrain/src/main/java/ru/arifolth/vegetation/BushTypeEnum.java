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

package ru.arifolth.vegetation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.BaseVegetationType;
import ru.arifolth.anjrpg.interfaces.LodUtils;
import ru.arifolth.anjrpg.interfaces.Utils;

public enum BushTypeEnum implements BaseVegetationType {

    BUSH0 {
        Node bushNode = null;
        final int probability = 70;
        @Override
        public void init() {
            Geometry bushGeometry = new Geometry("bush0", new Quad(8, 6));

            Material bushShader = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            Texture texture = assetManager.loadTexture("Textures/Bush/bush0/bush-0.png");
            texture.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture normalMap = assetManager.loadTexture("Textures/Bush/bush0/bush-0_normal.png");
            normalMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture specularMap = assetManager.loadTexture("Textures/Bush/bush0/bush-0_specular.png");
            specularMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            bushShader.setColor("Diffuse", ColorRGBA.White);
            bushShader.setColor("Ambient", ColorRGBA.White);
            bushShader.setColor("Specular", ColorRGBA.White);
            bushShader.setTexture("DiffuseMap", texture);
            bushShader.setTexture("NormalMap", normalMap);
            bushShader.setTexture("SpecularMap", specularMap);
            bushShader.setBoolean("UseMaterialColors", true);
            bushShader.setBoolean("HardwareShadows", true);
            bushShader.setBoolean("SteepParallax", true);
            bushShader.setBoolean("BackfaceShadows", true);
            bushShader.setFloat("AlphaDiscardThreshold", 0.5f);
            bushShader.setFloat("Shininess", 0f);
            bushShader.getAdditionalRenderState().setDepthTest(true);
            bushShader.getAdditionalRenderState().setDepthWrite(true);
            bushShader.getAdditionalRenderState().setColorWrite(true);
            bushShader.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bushShader.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            bushGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            bushGeometry.setMaterial(bushShader);
            bushGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);
            bushGeometry.rotate(0, 0f, 0);
            bushGeometry.center();

            Node bushBladeNode = new Node();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            //few more textured quads, looks much better but it seems to affect performance
            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 0.78f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushBladeNode.move(0, 1f, 0);

            MikktspaceTangentGenerator.generate(bushBladeNode);
            LodUtils.setUpModelLod(bushBladeNode);
            bushBladeNode = GeometryBatchFactory.optimize(bushBladeNode, true);
            bushBladeNode.updateModelBound();

            bushNode = bushBladeNode;
        }

        @Override
        public Node getNode() {
            return (Node) bushNode.clone();
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    PURPLE_LEAF_PLUM {
        Node bushNode = null;
        final int probability = 75;
        @Override
        public void init() {
            Geometry bushGeometry = new Geometry("purple-leaf-plum", new Quad(15, 12));

            Material bushShader = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            Texture texture = assetManager.loadTexture("Textures/Bush/purple-leaf-plum/purple-leaf-plum.png");
            texture.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture normalMap = assetManager.loadTexture("Textures/Bush/purple-leaf-plum/purple-leaf-plum-normal.png");
            normalMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture specularMap = assetManager.loadTexture("Textures/Bush/purple-leaf-plum/purple-leaf-plum-specular.png");
            specularMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            bushShader.setColor("Diffuse", ColorRGBA.White);
            bushShader.setColor("Ambient", ColorRGBA.White);
            bushShader.setColor("Specular", ColorRGBA.White);
            bushShader.setTexture("DiffuseMap", texture);
            bushShader.setTexture("NormalMap", normalMap);
            bushShader.setTexture("SpecularMap", specularMap);
            bushShader.setBoolean("UseMaterialColors", true);
            bushShader.setBoolean("HardwareShadows", true);
            bushShader.setBoolean("SteepParallax", true);
            bushShader.setBoolean("BackfaceShadows", true);
            bushShader.setFloat("AlphaDiscardThreshold", 0.5f);
            bushShader.setFloat("Shininess", 0f);
            bushShader.getAdditionalRenderState().setDepthTest(true);
            bushShader.getAdditionalRenderState().setDepthWrite(true);
            bushShader.getAdditionalRenderState().setColorWrite(true);
            bushShader.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bushShader.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            bushGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            bushGeometry.setMaterial(bushShader);
            bushGeometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            bushGeometry.rotate(0, 0f, 0);
            bushGeometry.center();

            Node bushBladeNode = new Node();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            //few more textured quads, looks much better but it seems to affect performance
            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 0.78f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushBladeNode.move(0, 1f, 15f);

            MikktspaceTangentGenerator.generate(bushBladeNode);
            LodUtils.setUpModelLod(bushBladeNode);
            bushBladeNode = GeometryBatchFactory.optimize(bushBladeNode, true);
            bushBladeNode.updateModelBound();

            bushNode = bushBladeNode;
        }

        @Override
        public Node getNode() {
            return (Node) bushNode.clone();
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    BUSH {
        Node bushNode = null;
        final int probability = 50;
        @Override
        public void init() {
            Geometry bushGeometry = new Geometry("bush", new Quad(12, 8));

            Material bushShader = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            Texture texture = assetManager.loadTexture("Textures/Bush/bush/bush.png");
            texture.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture normalMap = assetManager.loadTexture("Textures/Bush/bush/bush_normal.png");
            normalMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            Texture specularMap = assetManager.loadTexture("Textures/Bush/bush/bush_specular.png");
            specularMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
            bushShader.setColor("Diffuse", ColorRGBA.White);
            bushShader.setColor("Ambient", ColorRGBA.White);
            bushShader.setColor("Specular", ColorRGBA.White);
            bushShader.setTexture("DiffuseMap", texture);
            bushShader.setTexture("NormalMap", normalMap);
            bushShader.setTexture("SpecularMap", specularMap);
            bushShader.setBoolean("UseMaterialColors", true);
            bushShader.setBoolean("HardwareShadows", true);
            bushShader.setBoolean("SteepParallax", true);
            bushShader.setBoolean("BackfaceShadows", true);
            bushShader.setFloat("AlphaDiscardThreshold", 0.5f);
            bushShader.setFloat("Shininess", 0f);
            bushShader.getAdditionalRenderState().setDepthTest(true);
            bushShader.getAdditionalRenderState().setDepthWrite(true);
            bushShader.getAdditionalRenderState().setColorWrite(true);
            bushShader.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bushShader.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            bushGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            bushGeometry.setMaterial(bushShader);
            bushGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);
            bushGeometry.rotate(0, 0f, 0);
            bushGeometry.center();

            Node bushBladeNode = new Node();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            //few more textured quads, looks much better but it seems to affect performance
            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 0.78f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushGeometry = bushGeometry.clone();
            bushGeometry.rotate(0, 1.58f, 0);
            bushGeometry.center();
            bushBladeNode.attachChild(bushGeometry);

            bushBladeNode.move(0, 1f, 0);

            MikktspaceTangentGenerator.generate(bushBladeNode);
            LodUtils.setUpModelLod(bushBladeNode);
            bushBladeNode = GeometryBatchFactory.optimize(bushBladeNode, true);
            bushBladeNode.updateModelBound();

            bushNode = bushBladeNode;
        }

        @Override
        public Node getNode() {
            return (Node) bushNode.clone();
        }

        @Override
        public int getProbability() {
            return probability;
        }
    };

    public static Node getRandomBush() {
        Node node = null;

        if (Utils.getRandom(BUSH0.getProbability())) {
            node = BUSH0.getNode();
        } else if (Utils.getRandom(PURPLE_LEAF_PLUM.getProbability())) {
            node = PURPLE_LEAF_PLUM.getNode();
        } else {
            node = BUSH.getNode();
        }

        return node;
    }

    private static AssetManager assetManager;

    public static void setAssetManager(AssetManager assetManager) {
        BushTypeEnum.assetManager = assetManager;
    }
}
