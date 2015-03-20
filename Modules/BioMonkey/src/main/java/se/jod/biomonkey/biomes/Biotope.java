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
package se.jod.biomonkey.biomes;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.List;
import se.jod.biomonkey.biomes.plants.AbstractPlant;
import se.jod.biomonkey.terrain.TerrainTextureData;
import se.jod.biomonkey.terrain.TerrainTextureData.TerrainTextureUsage;

/**
 * Biotopes are small systems within biomes.
 * 
 * @author Andreas
 */
public class Biotope {

    protected static List<TerrainTextureData> ttds = new ArrayList<TerrainTextureData>();

    public static List<TerrainTextureData> getTTDS() {
        return ttds;
    }
    
    protected String name = "Default Biotope";
    protected int ID = 0;
    protected TerrainTextureData groundTexData;
    protected TerrainTextureData groundTexData2;
    protected BiotopeGeoData geoData;
    // Link to biome.
    protected Biome biome;

    public Biotope() {
        geoData = new BiotopeGeoData();
    }

    public Biotope(String name) {
        this();
        this.name = name;
    }

    /**
     * Texture data is used in the terrain material to render the
     * texture onto the terrain.
     * 
     * @param groundTex The texture to use.
     * @param normalTex the normal texture to use (or null for no normal texture)
     * @param scale The texture scale to use.
     * @param usage Where this texture will be used (flat ground, slopes..).
     * 
     * @param scale The texture scale.
     */
    public void setTextureData(Texture groundTex, Texture normalTex, ColorRGBA color, float scale, TerrainTextureUsage usage) {
        TerrainTextureData ttd = new TerrainTextureData();
        ttd.setGroundTexture(groundTex);
        if (normalTex != null) {
            ttd.setNormalTexture(normalTex);
        }
        if (color != null) {
            ttd.setColor(color);
        }
        ttd.setTextureScale(scale);
        if (usage == TerrainTextureUsage.Default) {
            groundTexData = ttd;
        } else if (usage == TerrainTextureUsage.Slopes) {
            groundTexData2 = ttd;
        }
        ttd.setBiotope(this);
        ttds.add(ttd);
    }

    public void removeTextureData(TerrainTextureUsage usage) {
        TerrainTextureData ttd;
        if (usage == TerrainTextureUsage.Default) {
            if (groundTexData != null) {
                ttd = groundTexData;
                ttds.remove(ttd);
                groundTexData = null;
            }
        } else if (usage == TerrainTextureUsage.Slopes) {
            if (groundTexData != null) {
                ttd = groundTexData2;
                ttds.remove(ttd);
                groundTexData2 = null;
            }
        }
        // TODO add some sort of listener. Handle plants that use this
        // texture data.
    }

    /**
     * Get the terrain texture data of this biotope based on usage.
     * @param usage
     * @return 
     */
    public TerrainTextureData getTextureData(TerrainTextureUsage usage) {
        if (usage == TerrainTextureUsage.Default) {
            return groundTexData;
        } else if (usage == TerrainTextureUsage.Slopes) {
            return groundTexData2;
        }
        return null;
    }

    /**
     * Get the default terrain texture data for this biotope.
     * @return 
     */
    public TerrainTextureData getGroundTexData() {
        return groundTexData;
    }

    /**
     * Get the terrain texture data used for slopes.
     * @return 
     */
    public TerrainTextureData getGroundTexData2() {
        return groundTexData2;
    }

    /**
     * Used internally.
     * @param ID 
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The geodata object contains important parameters, such as soil moisture and roughness.
     * @return 
     */
    public BiotopeGeoData getGeoData() {
        return geoData;
    }

    public Biome getBiome() {
        return biome;
    }

    /**
     * Used internally.
     * @param biome 
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Biotope other = (Biotope) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    /**
     *
     * @author Andreas
     */
    public class BiotopeGeoData {

        protected float maxSoilRoughness = 1;
        protected float minSoilRoughness = 0;
        protected float avgSoilRoughness = 0.5f;
        protected float maxSoilMoisture = 1;
        protected float minSoilMoisture = 0;
        protected float avgSoilMoisture = 0.5f;
        // Slope is a special case.
        protected float maxSlope = 0.7f;

        public BiotopeGeoData() {
        }

        public void setMinMaxSoilMoisture(float min, float max) {
            this.minSoilMoisture= min;
            this.maxSoilMoisture = max;
            this.avgSoilMoisture = (min + max) * 0.5f;
        }

        public void setMinMaxSoilRoughness(float min, float max) {
            this.minSoilRoughness = min;
            this.maxSoilRoughness = max;
            this.avgSoilRoughness = (min + max) * 0.5f;
        }

        public void setMaxSlope(float maxSlope) {
            maxSlope = FastMath.clamp(maxSlope, 0, 90);
            this.maxSlope = FastMath.atan(maxSlope * FastMath.DEG_TO_RAD) / FastMath.HALF_PI;
        }

        public float getAvgSoilMoisture() {
            return avgSoilMoisture;
        }

        public float getAvgSoilRoughness() {
            return avgSoilRoughness;
        }

        public float getMaxSoilMoisture() {
            return maxSoilMoisture;
        }
        
        public float getMinSoilMoisture() {
            return minSoilMoisture;
        }

        public float getMaxSoilRoughness() {
            return maxSoilRoughness;
        }

        public float getMinSoilRoughness() {
            return minSoilRoughness;
        }
        
        public float getMaxSlope() {
            return maxSlope;
        }
    }
}
