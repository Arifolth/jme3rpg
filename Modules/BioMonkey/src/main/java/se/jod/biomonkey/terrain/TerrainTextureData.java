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
package se.jod.biomonkey.terrain;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import se.jod.biomonkey.biomes.Biotope;

/**
 * A class that stores texture data for a biotope. The
 * data is needed for terrains in order to place the ground texture
 * properly.
 * 
 * @author Andreas
 */
public class TerrainTextureData {
    
    public enum TerrainTextureUsage { 
        Default(0), Slopes(1); 
    
        private byte ordinal;
        
        private TerrainTextureUsage(int ordinal){
            this.ordinal = (byte)ordinal;
        }
        
        public byte getOrdinal(){
            return ordinal;
        }
    
    }
    
    protected Biotope biotope;
    
    protected float textureScale;
    protected Texture groundTexture;
    protected Texture normalTexture;
    protected TerrainTextureUsage usage;
    protected ColorRGBA color;
    
    protected boolean useColor = false;
    protected boolean useNormalTexture = false;

    public TerrainTextureData() {
    }

    public TerrainTextureData(Texture groundTexture, Texture normalTexture, float textureScale, TerrainTextureUsage usage, Biotope b) {
        this.biotope = b;
        this.usage = usage;
        this.textureScale = textureScale;
        this.groundTexture = groundTexture;
        if(normalTexture != null){
            this.normalTexture = normalTexture;
            useNormalTexture = true;
        }
    }

    public Texture getGroundTexture() {
        return groundTexture;
    }

    public void setGroundTexture(Texture groundTexture) {
        this.groundTexture = groundTexture;
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public void setNormalTexture(Texture normalTexture) {
        this.normalTexture = normalTexture;
        useNormalTexture = true;
    }

    public float getTextureScale() {
        return textureScale;
    }

    public void setTextureScale(float textureScale) {
        this.textureScale = textureScale;
    }

    public boolean isUseNormalTexture() {
        return useNormalTexture;
    }
    
    public void setColor(ColorRGBA color){
        this.color = color;
        this.useColor = true;
    }

    public ColorRGBA getColor() {
        return color;
    }

    public boolean isUseColor() {
        return useColor;
    }

    public TerrainTextureUsage getUsage() {
        return usage;
    }

    public void setUsage(TerrainTextureUsage usage) {
        this.usage = usage;
    }

    public Biotope getBiotope() {
        return biotope;
    }

    public void setBiotope(Biotope biotope) {
        this.biotope = biotope;
    }
}
