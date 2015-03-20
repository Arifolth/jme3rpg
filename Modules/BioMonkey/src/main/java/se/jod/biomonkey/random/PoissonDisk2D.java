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
package se.jod.biomonkey.random;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import javax.imageio.ImageIO;

/**
 * Poisson disk sampling for semi-random distributions. Adapted from
 * a similar class in this sketch:
 * 
 * "http://www.openprocessing.org/sketch/30809"
 * 
 * @author Andreas
 */
public class PoissonDisk2D {

    /** From "Fast Poisson Disk Sampling in Arbitrary Dimensions"
     * by Robert Bridson
     * http://www.cs.ubc.ca/~rbridson/docs/bridson-siggraph07-poissondisk.pdf
     **/
    protected ArrayList<Vec2List> grid;
    protected float cellSize;
    protected float minDist;
    protected int gridWidth, gridHeight;
    protected float xmin, xmax, ymin, ymax;
    protected Vec2List points;
    protected FastRandom rand;

    public PoissonDisk2D() {
        points = new Vec2List();
        rand = new FastRandom();
    }

    public Vec2List getPoints() {
        return points;
    }

    public Vec2List generate(float xmin, float ymin, float xmax, float ymax, float minDist, int rejectionLimit) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.minDist = minDist;
        this.cellSize = minDist / FastMath.sqrt(2);
        this.gridWidth = (int) FastMath.ceil((xmax - xmin) / cellSize);
        this.gridHeight = (int) FastMath.ceil((ymax - ymin) / cellSize);
        int s = gridWidth * gridHeight;
        grid = new ArrayList<Vec2List>();
        
        for (int i = 0; i < s; i++) {
            grid.add(new Vec2List());
        }

        points.clear();
        LinkedList<Vector2f> processList = new LinkedList<Vector2f>();

        Vector2f p = new Vector2f(random(xmin, xmax), random(ymin, ymax));
        processList.add(p);
        points.add(p);
        addToGrid(p);

        while (processList.size() > 0) {
            int i = (int) (random(processList.size()));
            p = processList.get(i);
            processList.remove(i);
            for (i = 0; i < rejectionLimit; i++) {
                Vector2f n = createRandomPointAround(p, minDist, minDist * 2);
                if (insideBoundaries(n) && testGrid(n, minDist)) {
                    processList.add(n);
                    points.add(n);
                    addToGrid(n);
                }
            }
        }

        return points;
    }

    protected boolean insideBoundaries(Vector2f p) {
        return (p.x >= xmin && p.x < xmax && p.y >= ymin && p.y < ymax);
    }

    protected Vector2f createRandomPointAround(Vector2f p, float minDist, float maxDist) {
        float a = random(2 * FastMath.PI);
        float r = random(minDist, maxDist);
        return new Vector2f(p.x + r * FastMath.cos(a), p.y + r * FastMath.sin(a));
    }

    // return true if there are no points inside the circle of minDist radius around p
    protected boolean testGrid(Vector2f p, float minDist) {

        int minX = (int) FastMath.floor(max(0, (p.x - minDist - xmin) / cellSize));
        int maxX = (int) FastMath.ceil(min(gridWidth - 1, (p.x + minDist - xmin) / cellSize));
        int minY = (int) FastMath.floor(max(0, (p.y - minDist - ymin) / cellSize));
        int maxY = (int) FastMath.ceil(min(gridHeight - 1, (p.y + minDist - ymin) / cellSize));

        minDist *= minDist;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Vec2List cell = grid.get(y * gridWidth + x);
                for (Vector2f t : cell) {
                    if (t.distanceSquared(p) <= minDist) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    protected void addToGrid(Vector2f p) {
        grid.get(index(p.x, p.y)).add(p);
    }

    protected int index(float x, float y) {
        int gx = (int) FastMath.floor((x - xmin) / cellSize);
        int gy = (int) FastMath.floor((y - ymin) / cellSize);
        return gy * gridWidth + gx;
    }

    public boolean validate() {
        for (int i = 0; i < points.size() - 1; i++) {
            Vector2f sample = points.get(i);
            if (!insideBoundaries(sample)) {
                return false;
            }
            for (int k = i + 1; k < points.size(); k++) {
                Vector2f sample2 = points.get(k);
                if (sample.distanceSquared(sample2) < minDist*minDist) {
                    return false;
                }
            }
        }
        return true;
    }

    public void createImage() {
        int xm = (int) xmin;
        int ym = (int) ymin;
        int xM = (int) xmax;
        int yM = (int) ymax;
        
        int width  = (int) FastMath.abs(xM - xm);
        int height = (int) FastMath.abs(yM - ym);
        
        BufferedImage awtImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = awtImage.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();

        for (Vector2f pt : points) {
            int pos = (int) (pt.x) - xm + width * ((int)(pt.y) - ym);
            db.setElem(pos, (byte) 255);
        }
        
        File file = new File("PoissonDisk_" + width + "_" + height + "_minDist_" + (int)minDist + ".png").getAbsoluteFile();

        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            ImageIO.write(awtImage,"png",outStream);
        } catch (IOException ex) {
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    protected float random(float min, float max) {
        return min + rand.unitRandom() * (max - min);
    }

    protected float random(float f) {
        return rand.unitRandom() * f;
    }

    protected float min(float x, float y) {
        return (x < y) ? x : y;
    }

    protected float max(float x, float y) {
        return (x > y) ? x : y;
    }
    
    public class Vec2List extends ArrayList<Vector2f>{

        public Vec2List(Collection<? extends Vector2f> c) {
            super(c);
        }

        public Vec2List() {
        }

        public Vec2List(int initialCapacity) {
            super(initialCapacity);
        }
        
    }
}