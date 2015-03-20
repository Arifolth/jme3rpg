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

import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.biomes.BMWorld;
import se.jod.biomonkey.biomes.Biome;
import se.jod.biomonkey.biomes.Biotope;
import se.jod.biomonkey.image.BiotopeMap;
import se.jod.biomonkey.image.FloatMap;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.grid.Grid2D;
import se.jod.biomonkey.terrain.TerrainTextureData;
import se.jod.biomonkey.terrain.TerrainTextureData.TerrainTextureUsage;
import se.jod.biomonkey.terrain.datagrids.TerrainMapBlock;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid.MapCell;

/**
 * The terrain generator is used to create terrains. The first step is
 * heightmaps, the second step is the other related maps (such as slopes and soil
 * maps). The third step is distributing the pre-defined terrain-types (biotopes) 
 * based on that data.
 *
 * @author Andreas
 */
public class TerrainGenerator {

    public static boolean dumpBiotopeMaps;
    
    protected BMWorld world;
    protected float heightScale;
    protected float widthScale;
    protected int sizeX, sizeZ;
    protected int tileSize;
    protected int tileSizeP1;
    protected int unitsPerVert;
    protected float waterLevel;
    protected String textureFolder;
    protected TerrainNoise terrainNoise;
    protected int alphaSmoothing = 10;
    protected float hMin = Float.MAX_VALUE, hMax = Float.MIN_VALUE;
    // Used to weigh elevation, roughness, and moisture.
    float[] wParams = {0.8f, 0.1f, 0.1f};
    // Used to blend zones.
    protected float scaleMargin = 0.1f;
    protected TerrainMapGrid grid;
    protected int seed = 0;
    protected float heightOffset = 0.6f;

    public TerrainGenerator() {
    }

    public TerrainGenerator(BMWorld world) {
        this.world = world;
        this.heightScale = world.getTerrainData().getHeightScale();
        this.widthScale = world.getTerrainData().getWidthScale();
        this.sizeX = world.getTerrainData().getTilesX();
        this.sizeZ = world.getTerrainData().getTilesZ();
        this.tileSize = world.getTerrainData().getTileSize();
        this.tileSizeP1 = tileSize + 1;
        this.unitsPerVert = world.getTerrainData().getUnitsPerVert();
        this.waterLevel = world.getTerrainData().getWaterLevel();
        this.textureFolder = EcoManager.getInstance().getTextureFolder();
        this.heightOffset = world.getTerrainData().getHeightOffset();
        this.seed = world.getTerrainData().getSeed();

        terrainNoise = new TerrainNoise();
        terrainNoise.setHeightOffset(heightOffset);
        terrainNoise.setSeed(seed);
        terrainNoise.setScale(widthScale);
    }

    public TerrainMapGrid generateTerrain() {

        grid = new TerrainMapGrid();

        Grid2D<BiotopeMapCell> bmlg = new Grid2D<BiotopeMapCell>(this.sizeX, this.sizeZ);

        // Create a map of terrain texture data posts and indices. The index
        // associated with each terrain texture data is equal to the objects
        // place in the terrain texture datalist.
        Map<TerrainTextureData, Byte> ttds = new HashMap<TerrainTextureData, Byte>();

        for (int ttdIdx = 0; ttdIdx < Biotope.getTTDS().size(); ttdIdx++) {
            TerrainTextureData ttd = Biotope.getTTDS().get(ttdIdx);
            ttds.put(ttd, (byte) ttdIdx);
        }

        // Step 1 - generate the heightfields.
        for (int j = 0; j < sizeZ; j++) {
            for (int i = 0; i < sizeX; i++) {

                float[] heightMap = new float[tileSizeP1 * tileSizeP1];

                for (int idxZ = 0; idxZ < tileSizeP1; idxZ++) {
                    for (int idxX = 0; idxX < tileSizeP1; idxX++) {
                        float val = terrainNoise.getHeightValue(idxX, idxZ, i, j, tileSize);
                        if (val < hMin) {
                            hMin = val;
                        } else if (val > hMax) {
                            hMax = val;
                        }
                        heightMap[idxX + tileSizeP1 * idxZ] = val * heightScale;
                    }
                }

                FloatMap map = new FloatMap(heightMap, tileSizeP1, tileSize * unitsPerVert);
                grid.setHeightMap(map, i, j);
            }
        }


        // Write slope, roughness and moisture maps, then do the biotope map.
        for (int j = 0; j < sizeZ; j++) {
            for (int i = 0; i < sizeX; i++) {

                MapCell cell = grid.getGrid().getCell(i, j);

                // Lots of work.
                float[] slopes = createSlopeMap(i, j);

                float[] rNoise = new float[tileSizeP1 * tileSizeP1];

                for (int jN = 0; jN < tileSizeP1; jN++) {
                    for (int iN = 0; iN < tileSizeP1; iN++) {
                        rNoise[iN + tileSizeP1 * jN] = terrainNoise.getRoughnessValue(iN, jN, i, j, tileSize);
                    }
                }

                float[] heights = cell.getTerrainMapBlock().getHeightMap().getArray();
                float[] sRoughness = new float[heights.length];
                float[] sMoisture = new float[heights.length];

                for (int l = 0; l < tileSizeP1; l++) {
                    for (int k = 0; k < tileSizeP1; k++) {

                        int pos = k + tileSizeP1 * l;

                        // Normalize to 0,1
                        float rV = (rNoise[pos] + 1) * 0.5f;
                        // Normalize to 0,1
                        float hV = (heights[pos] / heightScale - hMin) / (hMax - hMin);
                        sRoughness[pos] = FastMath.clamp(rV + hV, 0, 1) * (1 - 0.3f * slopes[pos]);

                        sMoisture[pos] = FastMath.sqr(1 - hV) * (1 - 0.2f * sRoughness[pos]) * (1 - slopes[pos]);
                    }
                }

                grid.setSlopeMap(new FloatMap(slopes, tileSizeP1, tileSize * unitsPerVert), i, j);
                grid.setSoilRoughnessMap(new FloatMap(sRoughness, tileSizeP1, tileSize * unitsPerVert), i, j);
                grid.setSoilMoistureMap(new FloatMap(sMoisture, tileSizeP1, tileSize * unitsPerVert), i, j);


//                
//                
//                byte[] buf = new byte[slopes.length];
//                for (int k = 0; k < slopes.length; k++) {
//                    buf[k] = (byte) ((heights[k] - hMin) / (hMax - hMin) / heightScale * 255);
//                }
//                
//                String name = "heightmap";
//                writeByteArray(buf, i, j, folder, name);
//                
//                for (int k = 0; k < slopes.length; k++) {
//                    buf[k] = (byte) (slopes[k] * 255);
//                }
//                name = "slopes";
//                writeByteArray(buf, i, j, folder, name);

//                for (int k = 0; k < slopes.length; k++) {
//                    buf[k] = (byte) (sRoughness[k] * 255);
//                }
//                name = "roughness";
//                writeByteArray(buf, i, j, folder, name);
//                
//                for (int k = 0; k < slopes.length; k++) {
//                    buf[k] = (byte) (FastMath.clamp(sMoisture[k] * 255,0,255));
//                }
//                name = "moisture";
//                writeByteArray(buf, i, j, folder, name);

                // Lots of work.
                byte[][] biotopeMap = generateBiotopeMap(i, j, ttds);

                BiotopeMapCell bmc = new BiotopeMapCell(i, j);
                bmc.setBiotopeMapTemp(biotopeMap);
                bmlg.put(bmc.hashCode(), bmc);
            }
        }

        Tiled2DBlur.setupWeights(alphaSmoothing);

        // Lots of work.
        for (int j = 0; j < sizeZ; j++) {
            for (int i = 0; i < sizeX; i++) {
                BiotopeMapCell cell = bmlg.getCell(i, j);
                Tiled2DBlur.Blur(bmlg, cell, tileSizeP1, Biotope.getTTDS().size());
            }
        }

        // Now generate the alphamaps from the blended biotope data.

        // The textures will be on the form 2^N x 2^N, and each value
        // is the average of the surrounding values from the
        // biotope maps:
        //
        // AM(x,y) = ((BM(x,y) + BM(x + 1,y)),(BM(x,y) + BM(x,y + 1))*0.5
        for (BiotopeMapCell c : bmlg.values()) {
            byte[][] bbm = c.getBiotopeMapBlended();
            byte[] iTemp = new byte[Biotope.getTTDS().size()];

            int size = 0;
            // Check all layers of the biotope map. If a value larger
            // then 0 is found, add it to the alpha map indices data.
            // Alpha maps for this tile will be generated using only
            // non-empty layers.
            for (int k = 0; k < iTemp.length; k++) {
                inner:
                for (int j = 0; j < tileSizeP1; j++) {
                    for (int i = 0; i < tileSizeP1; i++) {
                        byte b = bbm[i + tileSizeP1 * j][k];
                        if (b != 0) {
                            iTemp[size++] = (byte) k;
                            break inner;
                        }
                    }
                }
            }
            
            // We now have a list of indices. These indices indicate which
            // layers to use. We now slim the array down to its real size.
            byte[] indices = new byte[size];
            System.arraycopy(iTemp, 0, indices, 0, size);

            // See how many different alpha textures are needed.
            int sizeD4 = (int) FastMath.ceil(size * 0.25f);

            Texture[] alphaMaps = new Texture[sizeD4];
            
            for (int k = 0; k < sizeD4; k++) {
                // Create a bytebuffer for this alphamap.
                ByteBuffer imgBuf = BufferUtils.createByteBuffer(4 * tileSize * tileSize);
                
                // Check how many channels are within this block (out of 4).
                int channels = size - k * 4;
                
                if (channels > 4) {
                    channels = 4;
                }

                // Iterate over the biotope map and write pixels.
                for (int j = 0; j < tileSize; j++) {
                    for (int i = 0; i < tileSize; i++) {

                        // For each channel of the alpha map (1 to 4)
                        for (int ch = 0; ch < channels; ch++) {
                            // Write using y-flipped coords.
                            int amPos = 4 * (i + tileSize * (tileSize - 1 - j));
                            // Read from the biotope maps (of size tileSizeP1 = tileSize + 1).
                            float val = bbm[i + tileSizeP1 * j][indices[ch + 4 * k]] & 0xff;
                            float valX = bbm[i + 1 + tileSizeP1 * j][indices[ch + 4 * k]] & 0xff;
                            float valY = bbm[i + tileSizeP1 * (j + 1)][indices[ch + 4 * k]] & 0xff;
                            valX = (val + valX) * 0.5f;
                            valY = (val + valY) * 0.5f;
                            imgBuf.put(amPos + 3 - ch, (byte) ((valX + valY) * 0.5f));
                        }
                    }
                }

                Image image = new Image(Format.ABGR8, tileSize, tileSize, imgBuf);
                Texture tex = new Texture2D(image);
                tex.setName("AlphaMap" + k);
//                tex.setMagFilter(Texture.MagFilter.Nearest);
//                tex.setMinFilter(Texture.MinFilter.NearestNearestMipMap);
                alphaMaps[k] = tex;
            }

            grid.setAlphaMaps(alphaMaps, c.getX(), c.getZ());
            grid.setTextureIndices(indices, c.getX(), c.getZ());
            // Can't use the same data as the alpha map objects to
            // read the data, as those are sent to the shader, and that 
            // would mess with the rendering.
            BiotopeMap bm = new BiotopeMap(bbm, tileSizeP1, Biotope.getTTDS().size(), tileSize * unitsPerVert);
            grid.setBiotopeMap(bm, c.getX(), c.getZ());
        }
        
//                    ByteBuffer megaBuf = BufferUtils.createByteBuffer(4 * tileSize * sizeX * tileSize * sizeZ);
//                    for (int j = 0; j < sizeZ; j++) {
//                        for (int i = 0; i < sizeX; i++) {
//                            TerrainMapBlock maps = grid.getMaps(i, j);
//                            Texture[] alphaMaps = maps.getAlphaMaps();
//                            ByteBuffer data = alphaMaps[0].getImage().getData(0);
//                            for (int y = 0; y < tileSize; y++) {
//                                for (int x = 0; x < tileSize; x++) {
//            
//                                    byte a = data.get(4 * (x + tileSize * y));
//                                    byte b = data.get(4 * (x + tileSize * y) + 1);
//                                    byte g = data.get(4 * (x + tileSize * y) + 2);
//                                    byte r = data.get(4 * (x + tileSize * y) + 3);
//                                    int pos = 4 * (tileSize * i + x + tileSize * sizeX * (tileSize * j + y));
//                                    megaBuf.put(pos, a);
//                                    megaBuf.put(pos + 1, b);
//                                    megaBuf.put(pos + 2, g);
//                                    megaBuf.put(pos + 3, r);
//                                }
//                            }
//                        }
//                    }
//            //        Image megaImage = new Image(Format.ABGR8, tileSize*sizeX, tileSize*sizeZ, megaBuf);
//            //        Texture megaTex = new Texture2D(megaImage);
//            //        megaTex.setName("MegaTex");
//                    megaBuf.clear();
//                    writeAlphaMap(megaBuf,0,0,"MegaAlphaMap");
            
//                    byte[] megaArray = new byte[tileSize * sizeX * tileSize * sizeZ];
//                    for (int j = 0; j < sizeZ; j++) {
//                        for (int i = 0; i < sizeX; i++) {
//                            TerrainMapBlock maps = grid.getMaps(i, j);
//                            byte[][] bm = maps.getBiotopeMap().getMDArray();
//                            for (int y = 0; y < tileSize; y++) {
//                                for (int x = 0; x < tileSize; x++) {
//            
//                                    byte b = bm[x + tileSizeP1*y][0];
//                                    int pos = (tileSize * i + x + tileSize * sizeX * (tileSize * j + y));
//                                    megaArray[pos] = b;
//                                }
//                            }
//                        }
//                    }
//                    String folder = this.textureFolder + "Tile_" + 0 + "_" + 0 + "/";
//                    writeByteArray(megaArray,0,0,folder,"mega");
                    
        try {
            writeMapCells(grid);
        } catch (IOException ex) {
            Logger.getLogger(TerrainGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        grid.getGrid().clear();
        return grid;
    }

    protected byte[][] generateBiotopeMap(int idxX, int idxZ, Map<TerrainTextureData, Byte> ttds) {

        // The index values for textures used at each point, and the texture
        // usage type ordinal at point + 1 (default, sloped..).
        byte[][] biotopeMap = new byte[tileSizeP1 * tileSizeP1][Biotope.getTTDS().size()];

        // Get a list of available biomes.
        Set<Biome> biomes = world.getBiomes();

        // Get a list of available biotopes.
        List<Biotope> biotopes = new ArrayList<Biotope>();

        for (Biome bm : biomes) {
            for (Biotope bt : bm.getBiotopes()) {
                // Check that all biotopes has proper textures.
                if (bt.getTextureData(TerrainTextureUsage.Default) == null) {
                    throw new RuntimeException("Biotope: " + bt.getName() + " hasn't got a default texture.");
                }
                biotopes.add(bt);
            }
        }

        int zones = biotopes.size();

        TerrainMapBlock tmb = grid.getMaps(idxX, idxZ);

        float[] heights = tmb.getHeightMap().getArray();
        float[] slopes = tmb.getSlopeMap().getArray();
        float[] sRoughness = tmb.getSoilRoughnessMap().getArray();
        float[] sMoisture = tmb.getSoilMoistureMap().getArray();

        // Sample each point of the heightfield and associate it with a
        // specific texture, using the indices set up in the ttd map.
        for (int j = 0; j < tileSizeP1; j++) {
            for (int i = 0; i < tileSizeP1; i++) {
                int pos = (i + tileSizeP1 * j);

                // Get the height, roughness and moisture values from the arrays.
                // Noise up the height a little.
                float heightVal = (heights[pos] / heightScale - hMin) / (hMax - hMin);
                heightVal += terrainNoise.getDistortionValue(i, j, idxX, idxZ, tileSize) * 0.1f;
                float roughVal = sRoughness[pos];
                float moistVal = sMoisture[pos];

                // Iterate over all biotopes to write their values.
                float bestVal = 0;
                Biotope best = null;

                for (int k = 0; k < zones; k++) {

                    Biotope b = biotopes.get(k);

                    // Get the average height, roughness and moisture preferences
                    // from this biotope.
                    float bHeightVal = 1 - FastMath.abs(heightVal - b.getBiome().getGeoData().getAverageElevation());
                    float bRoughVal = 1 - FastMath.abs(roughVal - b.getGeoData().getAvgSoilRoughness());
                    float bMoistVal = 1 - FastMath.abs(moistVal - b.getGeoData().getAvgSoilMoisture());

                    // Calculate a weighted sum to give this biome a "score" between 0 and 1
                    float bVal = bHeightVal * wParams[0] + bRoughVal * wParams[1] + bMoistVal * wParams[2];

                    if (bVal > bestVal) {
                        bestVal = bVal;
                        best = b;
                    }

                } // End of biotopes loop.

                // Find out if the biotope has a special texture
                // for slopes, and whether or not this position has
                // a heigher slope then the biotopes max slope value.
                TerrainTextureData ttdTemp = best.getGroundTexData();

                if (best.getTextureData(TerrainTextureUsage.Slopes) != null) {

                    if (slopes[pos] > best.getGeoData().getMaxSlope()) {
                        ttdTemp = best.getGroundTexData2();
                    }
                }
                biotopeMap[pos][ttds.get(ttdTemp)] = (byte) 255;
            }
        }

        return biotopeMap;
    }

    protected float[] createSlopeMap(int tileX, int tileZ) {

        MapCell cell = grid.getGrid().getCell(tileX, tileZ);

        float[] slopes = new float[tileSizeP1 * tileSizeP1];

        float[] hts = cell.getTerrainMapBlock().getHeightMap().getArray();
        float[] htsLeft = null;
        float[] htsRight = null;
        float[] htsUp = null;
        float[] htsDown = null;

        MapCell leftCell = grid.getGrid().getCell(cell.getX() - 1, cell.getZ());
        if (leftCell != null) {
            htsLeft = leftCell.getTerrainMapBlock().getHeightMap().getArray();
        }
        MapCell rightCell = grid.getGrid().getCell(cell.getX() + 1, cell.getZ());
        if (rightCell != null) {
            htsRight = rightCell.getTerrainMapBlock().getHeightMap().getArray();
        }
        MapCell upCell = grid.getGrid().getCell(cell.getX(), cell.getZ() + 1);
        if (upCell != null) {
            htsUp = upCell.getTerrainMapBlock().getHeightMap().getArray();
        }
        MapCell downCell = grid.getGrid().getCell(cell.getX(), cell.getZ() - 1);
        if (downCell != null) {
            htsDown = downCell.getTerrainMapBlock().getHeightMap().getArray();
        }

        // Sample pattern.
        //     4
        //   1 2 3
        //     0
        float[] hVals = new float[5];

        for (int j = 0; j < tileSizeP1; j++) {
            for (int i = 0; i < tileSizeP1; i++) {

                // Center value is always inside the current cell.
                hVals[2] = hts[i + tileSizeP1 * j];

                // Check if the point lies somewhere on the borders. Then its
                // up, left, right and down pixel may lie in another cell, and
                // thus inside another heightmap.
                if (i == 0) {
                    if (htsLeft != null) {
                        // Remember the maps overlap, so we need to sample the column
                        // second furthest to the right.
                        hVals[1] = htsLeft[tileSizeP1 - 2 + tileSizeP1 * j];
                    } else {
                        hVals[1] = hts[ tileSizeP1 * j];
                    }
                    hVals[3] = hts[1 + tileSizeP1 * j];
                } else if (i == tileSize) {
                    if (htsRight != null) {
                        hVals[3] = htsRight[1 + tileSizeP1 * j];
                    } else {
                        hVals[3] = hts[tileSize + tileSizeP1 * j];
                    }
                    hVals[1] = hts[tileSize - 1 + tileSizeP1 * j];
                } else {
                    hVals[1] = hts[i - 1 + tileSizeP1 * j];
                    hVals[3] = hts[i + 1 + tileSizeP1 * j];
                }
                // Do the same for j
                if (j == 0) {
                    if (htsDown != null) {
                        hVals[0] = htsDown[i + tileSizeP1 * (tileSizeP1 - 2)];
                    } else {
                        hVals[0] = hts[i];
                    }
                    hVals[4] = hts[i + tileSizeP1 * 1];
                } else if (j == tileSize) {
                    if (htsUp != null) {
                        hVals[4] = htsUp[i + tileSizeP1 * 1];
                    } else {
                        hVals[4] = hts[i + tileSizeP1 * tileSize];
                    }
                    hVals[0] = hts[i + tileSizeP1 * (tileSize - 1)];
                } else {
                    hVals[0] = hts[i + tileSizeP1 * (j - 1)];
                    hVals[4] = hts[i + tileSizeP1 * (j + 1)];
                }
                float slope = calculateMaxSlope(hVals);
                slopes[i + tileSizeP1 * j] = slope;

            }
        }

        return slopes;

//        ret[0] = hts[x + (tileSizeP1) * IntMath.max(z - 1, 0)];
//        ret[1] = hts[IntMath.max(x - 1, 0) + (tileSizeP1) * z];
//        ret[2] = hts[x + (tileSizeP1) * z];
//        ret[3] = hts[IntMath.min(x + 1, (tileSizeP1) - 1) + (tileSizeP1) * z];
//        ret[4] = hts[x + (tileSizeP1) * IntMath.min(z + 1, (tileSizeP1) - 1)];

    }

    // Max slope normalized to -1,1
    protected float calculateMaxSlope(float[] heights) {
        // Forward and back, x and z.
        float xF = FastMath.abs(heights[3] - heights[1]);
        float xB = FastMath.abs(heights[2] - heights[1]);
        float zF = FastMath.abs(heights[4] - heights[2]);
        float zB = FastMath.abs(heights[2] - heights[0]);

        float xM = xF > xB ? xF : xB;
        float zM = zF > zB ? zF : zB;
        float max = xM > zM ? xM : zM;

        return FastMath.atan(max / (float) unitsPerVert) / FastMath.HALF_PI;
    }

    public int getAlphaSmoothing() {
        return alphaSmoothing;
    }

    public void setAlphaSmoothing(int alphaSmoothing) {
        this.alphaSmoothing = alphaSmoothing;
    }

    public String getTextureFolder() {
        return textureFolder;
    }

    public void setTextureFolder(String textureFolder) {
        this.textureFolder = textureFolder;
    }

    
    protected void writeMapCells(TerrainMapGrid grid) throws IOException {

        for (MapCell c : grid.getGrid().values()) {

            TerrainMapBlock tmb = c.getTerrainMapBlock();
            FileOutputStream fos = null;
            try {
                File dir = new File(textureFolder + "Tile_" + c.getX() + "_" + c.getZ());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File object = new File(textureFolder + "Tile_" + c.getX() + "_" + c.getZ() + "/terrainData.j3o");
                if (object.exists()) {
                    throw new RuntimeException("The terrain data for this tile already exists. Delete old contents first. (" + object.getPath() + ")");
                }
                fos = new FileOutputStream(object);
                // we just use the exporter and pass in the terrain
                BinaryExporter.getInstance().save(tmb, new BufferedOutputStream(fos));

                fos.flush();
            } catch (IOException ex) {
                Logger.getLogger(TerrainGenerator.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    Logger.getLogger(TerrainGenerator.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    protected void writeByteArray(byte[] vals, int x, int z, String folder, String name) {

        BufferedImage awtImage = new BufferedImage(tileSize * sizeX, tileSize * sizeZ, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = awtImage.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
        byte[] cpuArray = db.getData();
//        byte[] cpuArray = vals;
        System.arraycopy(vals, 0, cpuArray, 0, vals.length);
        try {

            File dir = new File(folder);
            dir.mkdirs();
            ImageIO.write(awtImage, "png", new File(folder + name + ".png"));
        } catch (IOException ex) {
        }
    }

    protected void writeAlphaMap(ByteBuffer imgBuf, int x, int z, String name) {
        BufferedImage awtImage = new BufferedImage(tileSize * sizeX, tileSize * sizeZ, BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster wr = awtImage.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
        byte[] cpuArray = db.getData();

        // Copy native memory to java memory
        imgBuf.clear();
        imgBuf.get(cpuArray);
        imgBuf.clear();

        int width = wr.getWidth();
        int height = wr.getHeight();

        // Flip the components the way AWT likes them
        for (int yy = 0; yy < height / 2; yy++) {
            for (int xx = 0; xx < width; xx++) {
                int inPtr = (yy * width + xx) * 4;
                int outPtr = ((height - yy - 1) * width + xx) * 4;

                byte b1 = cpuArray[inPtr + 0];
                byte g1 = cpuArray[inPtr + 1];
                byte r1 = cpuArray[inPtr + 2];
                byte a1 = cpuArray[inPtr + 3];

                byte b2 = cpuArray[outPtr + 0];
                byte g2 = cpuArray[outPtr + 1];
                byte r2 = cpuArray[outPtr + 2];
                byte a2 = cpuArray[outPtr + 3];

                cpuArray[outPtr + 0] = a1;
                cpuArray[outPtr + 1] = b1;
                cpuArray[outPtr + 2] = g1;
                cpuArray[outPtr + 3] = r1;

                cpuArray[inPtr + 0] = a2;
                cpuArray[inPtr + 1] = b2;
                cpuArray[inPtr + 2] = g2;
                cpuArray[inPtr + 3] = r2;
            }
        }

        try {
            String fullDir = textureFolder + "Tile_" + x + "_" + z + "/";
            File dir = new File(fullDir);
            dir.mkdirs();
            ImageIO.write(awtImage, "png", new File(fullDir + name + "_" + tileSize + ".png"));
        } catch (IOException ex) {
        }
    }

    public class BiotopeMapCell extends GenericCell2D {

        protected byte[][] biotopeMapTemp;
        protected byte[][] biotopeMapBlended;

        public BiotopeMapCell() {
        }

        public BiotopeMapCell(int x, int z) {
            super(x, z);
        }

        public byte[][] getBiotopeMapTemp() {
            return biotopeMapTemp;
        }

        public void setBiotopeMapTemp(byte[][] biotopeMap) {
            this.biotopeMapTemp = biotopeMap;
        }

        public byte[][] getBiotopeMapBlended() {
            return biotopeMapBlended;
        }

        public void setBiotopeMapBlended(byte[][] biotopeMapBlended) {
            this.biotopeMapBlended = biotopeMapBlended;
        }
        
    }
}
