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

import java.util.HashSet;
import java.util.Set;

/**
 * Biomes are large scale eco systems, taiga, savannah etc.
 * 
 * @author Andreas
 */
public class Biome {

    public enum TemperatureZone {

        Cold(0, 0.31f), Temperate(0.3f, 0.7f), Hot(0.69f, 1f);
        private float min, max;

        private TemperatureZone(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }

    public enum PerspirationZone {

        Low(0, 0.31f), Medium(0.3f, 0.7f), High(0.69f, 1f);
        private float min, max;

        private PerspirationZone(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }

    public enum ElevationZone {
        // This biotope is present in deep water areas.

        Water_Deep(-1, -0.42f),
        // This biotope is present in areas with medium/shallow water levels.
        Water_Offshore(-0.45f, -0.18f),
        // This biotope is present in areas where water is shallow and meets the land.
        Water_Coastal(-0.15f, 0.05f),
        // This biome is present at low Elevations.
        Low_Elevation(0, 0.27f),
        // This biome is present at medium Elevations.
        Medium_Elevation(0.25f, 0.63f),
        // This biome is present at high Elevations.
        High_Elevation(0.6f, 87f),
        // This biome is only present at the highest areas.
        Top_Elevation(0.85f, 1f),
        // This biome is present everywhere under water.
        Universal_Water(-1, 0.05f),
        // This biome is present everywhere on land.
        Universal_Land(0, 1),
        // This biome is present everywhere.
        Universal(-1, 1);
        private float min, max;

        private ElevationZone(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }
    protected String name = "DefaultBiome";
    protected Set<Biotope> biotopes;
    protected BiomeGeoData geoData;
    protected int biotopeID = 0;
    protected int ID = 0;

    public Biome() {
        biotopes = new HashSet<Biotope>();
    }

    public Biome(String name) {
        this();
        this.name = name;
        this.geoData = new BiomeGeoData();
    }

    /**
     * Create a new biotope within this biome.
     * 
     * @param name The name of the new biotope.
     * @return 
     */
    public Biotope createBiotope(String name) {
        Biotope bt = new Biotope(name);
        bt.setID(ID + biotopeID++);
        bt.setBiome(this);
        biotopes.add(bt);
        return bt;
    }

    public Set<Biotope> getBiotopes() {
        return biotopes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getID() {
        return ID;
    }

    /**
     * Used internally.
     * @param ID 
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    /**
     * Get the biome data object (contains elevation data, temperature data etc.).
     * @return 
     */
    public BiomeGeoData getGeoData() {
        return geoData;
    }

    public class BiomeGeoData {

        protected float minElevation = 0f, maxElevation = 1f, averageElevation = 0.5f;
        protected float minTemperature = 0f, maxTemperature = 1f, averageTemperature = 0.5f;
        protected float minPerspiration = 0f, maxPerspiration = 1f, averagePerspiration = 0.5f;

        public void setMinMaxElevation(float min, float max) {
            this.minElevation = min;
            this.maxElevation = max;
            this.averageElevation = (min + max)*0.5f;
        }

        public void setMinMaxElevations(ElevationZone zone) {
            this.minElevation = zone.getMin();
            this.maxElevation = zone.getMax();
            this.averageElevation = (minElevation + maxElevation)*0.5f;
        }
        
        public void setMinMaxTemperature(float min, float max){
            this.minTemperature = min;
            this.maxTemperature = max;
            this.averageTemperature = (min + max)*0.5f;
        }
        
        public void setMinMaxTemperature(TemperatureZone zone){
            this.minTemperature = zone.getMin();
            this.maxTemperature = zone.getMax();
            this.averageTemperature = (this.minTemperature + this.maxTemperature)*0.5f;
        }
        
        public void setMinMaxPerspiration(float min, float max){
            this.minPerspiration = min;
            this.maxPerspiration = max;
            this.averagePerspiration = (min + max)*0.5f;
        }
        
        public void setMinMaxPerspiration(PerspirationZone zone){
            this.minPerspiration = zone.getMin();
            this.maxPerspiration = zone.getMax();
            this.averagePerspiration = (this.minPerspiration + this.maxPerspiration)*0.5f;
        }

        public float getAverageElevation() {
            return averageElevation;
        }

        public float getAveragePerspiration() {
            return averagePerspiration;
        }

        public float getAverageTemperature() {
            return averageTemperature;
        }

        public float getMaxPerspiration() {
            return maxPerspiration;
        }

        public float getMaxTemperature() {
            return maxTemperature;
        }

        public float getMaxElevation() {
            return maxElevation;
        }

        public float getMinPerspiration() {
            return minPerspiration;
        }

        public float getMinTemperature() {
            return minTemperature;
        }

        public float getMinElevation() {
            return minElevation;
        }

    }
}
