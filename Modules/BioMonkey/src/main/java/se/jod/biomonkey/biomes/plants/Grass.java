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
package se.jod.biomonkey.biomes.plants;

import com.jme3.texture.Texture;
import se.jod.biomonkey.grass.GrassLayer;

/**
 * A grass object. These should be created through the EcoManager.
 * 
 * @author Andreas
 */
public class Grass extends AbstractPlant {
    
    protected GrassLayer grassLayer;
    
    public Grass() {
        this.type = PlantType.Grass;
    }
    
    public Grass(String name, Texture grassTex, Texture normal){
        this.name = name;
        grassLayer = new GrassLayer(grassTex,normal);
    }
    
    public Grass(String name, Texture grassTex){
        this.name = name;
        grassLayer = new GrassLayer(grassTex);
    }

    /**
     * Get the grass layer object for this grass. Grasslayers contain all
     * the grass data, such as quad size, density, material settings etc.
     * @return 
     */
    public GrassLayer getGrassLayer() {
        return grassLayer;
    }

    public void setGrassLayer(GrassLayer grassLayer) {
        this.grassLayer = grassLayer;
    }

}
