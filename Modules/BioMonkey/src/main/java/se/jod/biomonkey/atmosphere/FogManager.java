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
package se.jod.biomonkey.atmosphere;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles fog.
 *
 * @author Andreas
 */
public class FogManager {

    protected ColorRGBA fogBaseColor = new ColorRGBA(0.5f, 0.6f, 0.7f, 1f);
    protected ColorRGBA fogColor = ColorRGBA.White;
    protected ColorRGBA fogSunColor = ColorRGBA.White;
    protected Vector3f sunDir = new Vector3f(1, 1, 1).normalizeLocal();
    protected List<Material> fogMaterials;
    // x = near fog
    // y = 1 / (far fog - near fog)
    // z = mult (should be between 0 and 1, 1 for full effect at far distance).
    protected Vector4f fogParams = new Vector4f(100, 1 / 600f, 1f,16);

    public FogManager() {
    }
    
    protected void setFogParams(Material mat) {
        mat.setBoolean("Fog", true);
        mat.setVector4("FogParams", fogParams);
        mat.setColor("FogColor", fogColor);
        mat.setColor("FogSunColor", fogSunColor);
        mat.setVector3("SunDir", sunDir);
    }
    
    /**
     * Add a material to the fog system. The material will have its fog
     * parameters updated every frame.
     * 
     * @param mat 
     */
    public void addMaterial(Material mat) {
        if(fogMaterials == null){
            fogMaterials = new ArrayList<Material>();
        }
        this.fogMaterials.add(mat);
    }

    public void removeMaterial(Material mat) {
        this.fogMaterials.remove(mat);
    }

    public void update(AtmosphereManager am, Vector3f sunDir, float position) {

        fogColor = fogBaseColor.mult(am.getSun().getColor().b * 1.1f);
        fogSunColor = am.getSun().getColor();
        this.sunDir = sunDir;
        
        if (fogMaterials != null && !fogMaterials.isEmpty()) {
            for (int i = 0; i < fogMaterials.size(); i++) {
                setFogParams(fogMaterials.get(i));
            }
        }
    }
}