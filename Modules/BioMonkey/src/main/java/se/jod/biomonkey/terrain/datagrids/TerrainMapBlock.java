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
package se.jod.biomonkey.terrain.datagrids;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.texture.Texture;
import java.io.IOException;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.image.FloatMap;

/**
 * A block of terrain data.
 *
 * @author Andreas
 */
public class TerrainMapBlock implements Savable {

    FloatMap heightMap;
    FloatMap slopeMap;
    FloatMap sRoughnessMap;
    FloatMap sMoistureMap;
    BiotopeMap biotopeMap;
    Texture[] alphaMaps;
    byte[] textureIndices;

    public TerrainMapBlock() {
    }

    public FloatMap getHeightMap() {
        return heightMap;
    }

    public void setHeightMap(FloatMap heightMap) {
        this.heightMap = heightMap;
    }

    public FloatMap getSlopeMap() {
        return slopeMap;
    }

    public void setSlopeMap(FloatMap slopeMap) {
        this.slopeMap = slopeMap;
    }

    public FloatMap getSoilRoughnessMap() {
        return sRoughnessMap;
    }

    public void setSoilRoughnessMap(FloatMap roughnessMap) {
        this.sRoughnessMap = roughnessMap;
    }

    public FloatMap getSoilMoistureMap() {
        return sMoistureMap;
    }

    public void setSoilMoistureMap(FloatMap moistureMap) {
        this.sMoistureMap = moistureMap;
    }

    public BiotopeMap getBiotopeMap() {
        return biotopeMap;
    }

    public void setBiotopeMap(BiotopeMap biotopeMap) {
        this.biotopeMap = biotopeMap;
    }

    public Texture[] getAlphaMaps() {
        return alphaMaps;
    }

    public void setAlphaMaps(Texture[] alphaMaps) {
        this.alphaMaps = alphaMaps;
    }

    public byte[] getTextureIndices() {
        return textureIndices;
    }

    public void setTextureIndices(byte[] textureIndices) {
        this.textureIndices = textureIndices;
    }

    protected void generateAlphaMaps() {
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(heightMap, "heightMap", null);
        capsule.write(slopeMap, "slopeMap", null);
        capsule.write(sMoistureMap, "moistureMap", null);
        capsule.write(sRoughnessMap, "roughnessMap", null);
        capsule.write(alphaMaps, "alphaMaps", null);
        capsule.write(biotopeMap, "biotopeMap", null);
        capsule.write(textureIndices, "textureIndices", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        this.heightMap = (FloatMap) ic.readSavable("heightMap", null);
        this.slopeMap = (FloatMap) ic.readSavable("slopeMap", null);
        this.sMoistureMap = (FloatMap) ic.readSavable("moistureMap", null);
        this.sRoughnessMap = (FloatMap) ic.readSavable("roughnessMap", null);
        this.biotopeMap = (BiotopeMap) ic.readSavable("biotopeMap", null);
        Savable[] aTx = ic.readSavableArray("alphaMaps", null);
        if (aTx != null) {
            alphaMaps = new Texture[aTx.length];
            for (int i = 0; i < aTx.length; i++) {
                alphaMaps[i] = (Texture) aTx[i];
            }
        }
        this.textureIndices = ic.readByteArray("textureIndices", null);
        generateAlphaMaps();
    }
}
