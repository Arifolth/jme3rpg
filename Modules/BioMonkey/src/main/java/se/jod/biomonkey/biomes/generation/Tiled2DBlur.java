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
import se.jod.biomonkey.IntMath;
import se.jod.biomonkey.biomes.generation.TerrainGenerator.BiotopeMapCell;
import se.jod.biomonkey.paging.grid.Grid2D;

/**
 * A method used to blur the biotope maps.
 * 
 * @author Andreas
 */
public class Tiled2DBlur {

    protected static float[] weights;
    protected static int filterWidth;
    protected static float deviation;

    public static void setupWeights(int filterW) {
        
        filterWidth = filterW;
        deviation = filterWidth / 3;

        weights = new float[filterWidth + 1];
        float sum = 0;

        for (int i = 0; i <= filterWidth; i++) {
            weights[i] = gaussian(i, deviation);
            if (i == 0) {
                sum += weights[i];
            } else {
                sum += 2 * weights[i];
            }
        }

        for (int i = 0; i <= filterWidth; i++) {
            weights[i] /= sum;
        }
    }

    public static float gaussian(float x, float sigma) {
        return (1 / FastMath.sqrt(2 * FastMath.PI) / sigma) * FastMath.exp(-0.5f * FastMath.sqr(x / sigma));
    }

    /**
     * 2D blur of radius "filterWidth" over N different layers.
     */
    public static void Blur(Grid2D<BiotopeMapCell> bmlg, BiotopeMapCell c,int size, int depth) {

        int tempImgSize = size + 2 * filterWidth;
        
        // Writing to a temporary image with a filterWidth-sized frame around
        // it. Writing horizontal blur onto that image, then read it during
        // the vertical blur and write the colors back to the original image.
        byte[][] tempImg = new byte[tempImgSize * tempImgSize][depth];
        
        byte[][] blendImg = new byte[size*size][depth];

        // Images
        byte[][] img = c.getBiotopeMapTemp();
        byte[][] imgLeft = null;
        byte[][] imgRight = null;
        byte[][] imgUp = null;
        byte[][] imgDown = null;
        byte[][] imgUpLeft = null;
        byte[][] imgUpRight = null;
        byte[][] imgDownLeft = null;
        byte[][] imgDownRight = null;

        BiotopeMapCell leftCell = bmlg.get(Grid2D.hash(c.getX() - 1, c.getZ()));
        if (leftCell != null) {
            imgLeft = leftCell.getBiotopeMapTemp();
        }
        BiotopeMapCell rightCell = bmlg.get(Grid2D.hash(c.getX() + 1, c.getZ()));
        if (rightCell != null) {
            imgRight = rightCell.getBiotopeMapTemp();
        }
        BiotopeMapCell upCell = bmlg.get(Grid2D.hash(c.getX(), c.getZ() + 1));
        if (upCell != null) {
            imgUp = upCell.getBiotopeMapTemp();
        }
        BiotopeMapCell downCell = bmlg.get(Grid2D.hash(c.getX(), c.getZ() - 1));
        if (downCell != null) {
            imgDown = downCell.getBiotopeMapTemp();
        }
        BiotopeMapCell upLeftCell = bmlg.get(Grid2D.hash(c.getX() - 1, c.getZ() + 1));
        if (upLeftCell != null) {
            imgUpLeft = upLeftCell.getBiotopeMapTemp();
        }
        BiotopeMapCell upRightCell = bmlg.get(Grid2D.hash(c.getX() + 1, c.getZ() + 1));
        if (upRightCell != null) {
            imgUpRight = upRightCell.getBiotopeMapTemp();
        }
        BiotopeMapCell downLeftCell = bmlg.get(Grid2D.hash(c.getX() - 1, c.getZ() - 1));
        if (downLeftCell != null) {
            imgDownLeft = downLeftCell.getBiotopeMapTemp();
        }
        BiotopeMapCell downRightCell = bmlg.get(Grid2D.hash(c.getX() + 1, c.getZ() - 1));
        if (downRightCell != null) {
            imgDownRight = downRightCell.getBiotopeMapTemp();
        }

        // Horizontal pass. Write the larger image.
        for (int j = -filterWidth; j < size + filterWidth; j++) {
            for (int i = -filterWidth; i < size + filterWidth; i++) {

                int tempImgIdxX = i + filterWidth;
                int tempImgIdxY = j + filterWidth;

                // This array keeps an aggregate value for each layer.
                float[] values = new float[depth];

                for (int ii = i - filterWidth; ii <= i + filterWidth; ii++) {
                    // This are values between -filterWidth and filterWidth. It determines
                    // what weight to use.
                    int weightIdx = IntMath.abs(ii - i);

                    // We need to use the images of neighbouring cells near
                    // the exges, when they exist, otherwise we will get seams at
                    // the edges.
                    int iii = ii;
                    int jjj = j;

                    byte[][] thisImg = img;
                    // If sampling the bottom row.
                    if (j < 0) {
                        if (ii < 0) {
                            if (imgDownLeft != null) {
                                thisImg = imgDownLeft;
                                iii = ii + (size - 1);
                                jjj = j + (size - 1);
                            } else {
                                jjj = iii = 0;
                            }
                        } else if (ii > size - 1) {
                            if (imgDownRight != null) {
                                thisImg = imgDownRight;
                                iii = ii - (size - 1);
                                jjj = j + (size - 1);
                            } else {
                                jjj = 0;
                                iii = size - 1;
                            }
                        } else if (imgDown != null) {
                            thisImg = imgDown;
                            jjj = j + (size - 1);
                        } else {
                            jjj = 0;
                        }
                        // If sampling from the top row.
                    } else if (j > size - 1) {
                        if (ii < 0) {
                            if (imgUpLeft != null) {
                                thisImg = imgUpLeft;
                                iii = ii + (size - 1);
                                jjj = j - (size - 1);
                            } else {
                                jjj = size - 1;
                                iii = 0;
                            }
                        } else if (ii > size - 1) {
                            if (imgUpRight != null) {
                                thisImg = imgUpRight;
                                iii = ii - (size - 1);
                                jjj = j - (size - 1);
                            } else {
                                jjj = iii = size - 1;
                            }
                        } else if (imgUp != null) {
                            thisImg = imgUp;
                            jjj = j - (size - 1);
                        } else {
                            jjj = size - 1;
                        }
                        // If sampling from the center row.
                    } else {
                        if (ii < 0) {
                            if (imgLeft != null) {
                                thisImg = imgLeft;
                                iii = ii + (size - 1);
                            } else {
                                iii = 0;
                            }
                        } else if (ii > size - 1) {
                            if (imgRight != null) {
                                thisImg = imgRight;
                                iii = ii - (size - 1);
                            } else {
                                iii = size - 1;
                            }
                        }
                    }

                    for (int k = 0; k < depth; k++) {
                        // Add to the aggregate value of each layer.
                        values[k] += weights[weightIdx] * (thisImg[iii + size * jjj][k] & 0xff);
                    }

                }

                // Write the aggreate to each layer at this pixel, and move on to the next..
                for (int k = 0; k < depth; k++) {
                    tempImg[tempImgIdxX + tempImgSize * tempImgIdxY][k] = (byte) (values[k]);
                }

            } // for i
        } // for j



        // Vertical pass.
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                // This array keeps an aggregate value for each layer.
                float[] values = new float[depth];


                // Used to normalize.
                float sum = 0;

                for (int jj = j - filterWidth; jj <= j + filterWidth; jj++) {
                    // This are values between -filterWidth and filterWidth. It determines
                    // what weight to use.
                    int weightIdx = IntMath.abs(jj - j);

                    // Read the value from the scaled up temporary image. It has values from 
                    // -filterWidth to size + filterWidth (indexed from 0 to size + 2*filterwith),
                    // so no range check is needed on i and jj.
                    for (int k = 0; k < depth; k++) {
                        // Add to the aggregate value of each layer.
                        values[k] += weights[weightIdx] * (tempImg[i + filterWidth + tempImgSize * (jj + filterWidth)][k] & 0xff);
                    }
                }

                for (int k = 0; k < depth; k++) {
                    sum += values[k] / 255f;
                }

                for (int k = 0; k < depth; k++) {
                    blendImg[i + size * j][k] = (byte) (values[k] / sum);
                }
            }
        }
        c.biotopeMapBlended = blendImg;
    }
}
