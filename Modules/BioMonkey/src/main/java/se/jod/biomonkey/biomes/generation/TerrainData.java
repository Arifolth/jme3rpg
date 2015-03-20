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
package se.jod.biomonkey.biomes.generation;

import com.jme3.math.FastMath;
import se.jod.biomonkey.biomes.BMWorld.MetricUnit;

/**
 * Storage class for terrain data. There are some convenience methods here
 * as well.
 * 
 * @author Andreas
 */
public class TerrainData {
    protected float unitScale = 1;
    protected MetricUnit unit = MetricUnit.Meters;
    
    protected float heightScale = 30;
    protected float widthScale = 1;
    protected int tilesX = 1, tilesZ = 1;
    protected int tileSize = 512;
    protected int unitsPerVert = 1;
    
    protected int seed = 0;
    protected float heightOffset = 0.6f;
    
    protected float waterLevel = 0;
    
    /**
     * Set the number of terrain tiles in x and z.
     * @param tilesX
     * @param tilesZ 
     */
    public void setNumTiles(int tilesX, int tilesZ){
        this.tilesX = tilesX;
        this.tilesZ = tilesZ;
    }

    public float getHeightOffset() {
        return heightOffset;
    }

    /**
     * Height offset is used in the terrain generation algorithm. It should
     * normally be between 0 and 1. See also the HMFT.java offset value.
     * @param heightOffset 
     */
    public void setHeightOffset(float heightOffset) {
        this.heightOffset = heightOffset;
    }

    public float getHeightScale() {
        return heightScale;
    }

    /**
     * This value is multiplied with the terrain heights. Fractal values are 
     * normally single digit values, often in the range -4 to 4, depending on 
     * the settings, so they don't end up being the actual max height.
     * 
     * @param heightScale 
     */
    public void setHeightScale(float heightScale) {
        this.heightScale = heightScale;
    }
    
    public float getWidthScale(){
        return widthScale;
    }
    
    /**
     * Width scale is used to scale the terrain in the xz plane. It calls
     * noise.setScale in turn.
     * 
     * @param scale 
     */
    public void setWidthScale(float scale){
        this.widthScale = scale;
    }
    
    /**
     * Scales the entire terrain - both height and width, using the multiplier
     * value. A multiplier of 2 will scale the terrain noise 2 times, and
     * multiply all values by 2 (i.e. aspect ratio is preserved).
     * 
     * @param multiplier 
     */
    public void scaleTerrain(float multiplier){
        this.widthScale *= multiplier;
        this.heightScale *= multiplier;
    }

    public int getSeed() {
        return seed;
    }

    /**
     * Sets the seed value for the noise. Basically it just offsets all the 
     * points 'seed' nr. of tiles in x,y and z.
     * @param seed 
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getTileSize() {
        return tileSize;
    }

    /**
     * Set the size of each tile. Should be a power of two. The real tile size 
     * (size in the world) depends on the unitsPerVert value.
     * @param tileSize 
     */
    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    public int getTilesX() {
        return tilesX;
    }

    /**
     * Set the number of tiles in the x dir.
     * @param tilesX 
     */
    public void setTilesX(int tilesX) {
        this.tilesX = tilesX;
    }

    public int getTilesZ() {
        return tilesZ;
    }

    /**
     * Set the number of tiles in the z dir.
     * @param tilesZ 
     */
    public void setTilesZ(int tilesZ) {
        this.tilesZ = tilesZ;
    }

    public MetricUnit getUnit() {
        return unit;
    }

    /**
     * Sets the metric unit. This has no effect, only makes it easier to
     * keep track of size.
     * 
     * @param unit 
     */
    public void setUnit(MetricUnit unit) {
        this.unit = unit;
    }

    public float getUnitScale() {
        return unitScale;
    }

    /**
     * Set the unit scale. If unit scale is 10, each jME unit corresponds
     * to 10 metric units.
     * 
     * @param unitScale 
     */
    public void setUnitScale(float unitScale) {
        this.unitScale = unitScale;
    }

    public int getUnitsPerVert() {
        return unitsPerVert;
    }

    /**
     * Set the number of units per vert. This value has to be a positive power
     * of two. If units per vert is 4, that means there is four jME units between
     * each vert (in the world), so it's used to scale tiles. A tile size of 128x128
     * with 4 units per vert results in a real tile size of 512x512.
     * 
     * @param unitsPerVert 
     */
    public void setUnitsPerVert(int unitsPerVert) {
        if(unitsPerVert < 1 || !FastMath.isPowerOfTwo(unitsPerVert)){
            throw new RuntimeException("Verts per unit has to be a positive power of two");
        }
        this.unitsPerVert = unitsPerVert;
    }
    
    /**
     * Set the metric used.
     * 
     * @param unit The unit type, Meter or feets.
     * @param unitScale The amount of units that corresponds to 1 jME unit.
     */
    public void setMetric(MetricUnit unit, float unitScale) {
        if(unitScale <= 0){
            throw new RuntimeException("Unit scale must be a postive real number");
        }
        this.unit = unit;
        this.unitScale = unitScale;
    }
    
    public float getWaterLevel() {
        return waterLevel;
    }

    /**
     * Set the water level.
     * @param waterLevel 
     */
    public void setWaterLevel(float waterLevel) {
        // TODO fix this.
        this.waterLevel = waterLevel;
    }
    
    /**
     * Get the real tile size (the size of a tile in the world). This
     * value is equal to tileSize * unitsPerVert.
     * 
     * @return 
     */
    public int getRealTileSize(){
        return tileSize*unitsPerVert;
    }
    
    /**
     * Get the total size of the terrain in the X dir. This is equal to
     * the real tile size times the number of tiles in the X dir.
     * @return 
     */
    public int getTotalSizeX() {
        return tilesX*tileSize*unitsPerVert;
    }

    /**
     * See getTotalSizeX()
     * @return 
     */
    public int getTotalSizeZ() {
        return tilesZ*tileSize*unitsPerVert;
    }
    
    /**
     * Get the total metric size of the terrain in the x dir.
     * This is the totalSizeX value times the unit scale. See
     * the getTotalSizeX() method doc for more info.
     * 
     * @return 
     */
    public float getTotalMetricSizeX(){
        return getTotalSizeX()*unitScale;
    }
    
    /**
     * See getTotalMetricSizeX()
     * @return 
     */
    public float getTotalMetricSizeZ(){
        return getTotalSizeZ()*unitScale;
    }
    
    /**
     * Gets the area of the terrain in jME world units. This is equal to
     * taking the totalSizeX times the totalSizeZ. See getTotalSizeX() for
     * more info.
     * @return 
     */
    public int getTerrainArea(){
        return getTotalSizeX()*getTotalSizeZ();
    }
    
    /**
     * Gets the total terrain area in terms of the chosen metric unit
     * scale. If total terrain size is 100x100 jME world units, and metric 
     * scale is 10, this function will return 1000x1000.
     * 
     * @return 
     */
    public float getMetricTerrainArea(){
        return getTerrainArea()*FastMath.sqr(unitScale);
    }
    
}
