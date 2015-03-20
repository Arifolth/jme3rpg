/*
 * Copyright (c) 2012, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.jod.biomonkey.atmosphere.sky;

import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import se.jod.biomonkey.EcoManager;

/**
 * A class containing moon geometry and a directional light. It will be
 * extended.
 * 
 * @author Kallsta
 */
public class Moon {
    
    protected Geometry moonGeom;
    protected Material moonMat;
    protected DirectionalLight moonLight;
    
    public Moon(){
        moonLight = new DirectionalLight();
        moonLight.setName("Moon");
        AssetManager am = EcoManager.getInstance().getApp().getAssetManager();
        Quad mq = new Quad(100,100);
        moonGeom = new Geometry("Moon",mq);
        moonMat = new Material(am,"se/jod/biomonkey/assets/matdefs/MoonBase.j3md");
        Texture moonTex = am.loadTexture("se/jod/biomonkey/assets/textures/SkyX_Moon.png");
        moonMat.setTexture("MoonTex", moonTex);
        moonMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        moonGeom.setMaterial(moonMat);
        moonGeom.setQueueBucket(Bucket.Transparent);
    }

    public Geometry getMoonGeom() {
        return moonGeom;
    }

    public Material getMoonMat() {
        return moonMat;
    }

    public DirectionalLight getMoonLight() {
        return moonLight;
    }
}
