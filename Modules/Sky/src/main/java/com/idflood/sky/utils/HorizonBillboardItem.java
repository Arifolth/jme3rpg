/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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

package com.idflood.sky.utils;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class HorizonBillboardItem extends Geometry {
    private Mesh box;

    public HorizonBillboardItem(AssetManager assetManager, String name, Float scale) {
        super(name);

        box = new Quad(8_000, 4_500);
        setMesh(box);

        Material mountainShader = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture grass = assetManager.loadTexture("Textures/Terrain/background/Mountain.png");
        grass.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        Texture normalMap = assetManager.loadTexture("Textures/Terrain/background/MountainNormalMap.png");
        normalMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        Texture specularMap = assetManager.loadTexture("Textures/Terrain/background/MountainSpecularMap.png");
        specularMap.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        mountainShader.setColor("Diffuse", ColorRGBA.White);
        mountainShader.setColor("Ambient", ColorRGBA.White);
        mountainShader.setColor("Specular", ColorRGBA.White);
        mountainShader.setTexture("DiffuseMap", grass);
        mountainShader.setTexture("NormalMap", normalMap);
        mountainShader.setTexture("ParallaxMap", specularMap);
        mountainShader.setTexture("SpecularMap", specularMap);
        mountainShader.setBoolean("UseMaterialColors", true);
        mountainShader.setBoolean("HardwareShadows", true);
        mountainShader.setBoolean("SteepParallax", true);
        mountainShader.setBoolean("BackfaceShadows", false);
        mountainShader.setFloat("AlphaDiscardThreshold", 0.5f);
        mountainShader.setFloat("Shininess", 1f);
        mountainShader.getAdditionalRenderState().setDepthTest(true);
        mountainShader.getAdditionalRenderState().setDepthWrite(true);
        mountainShader.getAdditionalRenderState().setColorWrite(true);
        mountainShader.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mountainShader.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        setCullHint(Spatial.CullHint.Never);
        setQueueBucket(RenderQueue.Bucket.Sky);
        setMaterial(mountainShader);
        setShadowMode(RenderQueue.ShadowMode.Off);
        rotate(0f, 1.57f, 0);
        center();

        BillboardControl billBoadControl = new BillboardControl();
        billBoadControl.setAlignment(BillboardControl.Alignment.AxialY);
        addControl(billBoadControl);
    }
}
