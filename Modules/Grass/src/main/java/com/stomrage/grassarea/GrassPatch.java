/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A grass patch is a generic class to hold GrassObject, an another GrassPatch
 * or a GrassHolder.
 *
 * @author Stomrage
 * @version 0.1
 */
public class GrassPatch implements GrassObject {

    //The list of subdivised GrassObject
    private GrassObject[][] grassPatchs;
    //The GrassPatch x location
    private int locX;
    //The GrassPatch z location
    private int locZ;
    //The location of the GrassPatch
    private Vector3f location;
    private int currentSize;

    /**
     * Needed by
     *
     * @see Savable
     */
    public GrassPatch() {
    }

    /**
     * The GrassPatch Constructor. Create a new GrassPatch at the given location
     * and size
     *
     * @param lX The x location of the GrassPatch
     * @param lZ The z location of the GrassPatch
     * @param currentSize The current size (subdivided)
     * @param holderSize The final size
     * @param totalSize The total size of the GrassArea
     */
    public GrassPatch(int lX, int lZ, int currentSize, int holderSize) {
        this.grassPatchs = new GrassObject[2][2];
        this.locX = lX;
        this.locZ = lZ;
        this.currentSize = currentSize;
        //The location is in the center of the GrassPatch
        location = new Vector3f(locX + currentSize, 0, locZ + currentSize);
        //If the divided size is not the size of the holder then we create a GrassPatch otherwise create a GrassHolder
        if (holderSize != currentSize / 2 - holderSize) {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    grassPatchs[x][z] = new GrassPatch(locX + currentSize / 2 * x, locZ + currentSize / 2 * z, currentSize / 2, holderSize);
                }
            }
        } else {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    grassPatchs[x][z] = new GrassHolder(locX + currentSize / 2 * x, locZ + currentSize / 2 * z, holderSize);
                }
            }
        }
    }

    public Vector3f getLocation() {
        return location;
    }

    public void generate() {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                grassPatchs[x][z].generate();
            }
        }
    }

    public GrassObject[][] getPatch() {
        return grassPatchs;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        location = (Vector3f) capsule.readSavable("location", new Vector3f());
        locX = capsule.readInt("locX", 0);
        locZ = capsule.readInt("locZ", 0);
        currentSize = capsule.readInt("currentSize", 0);
        grassPatchs = GrassUtils.toArray2D(capsule.readSavableArrayList("grassPatchs", new ArrayList<GrassObject>()), 2);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(location, "location", new Vector3f());
        capsule.write(locX, "locX", 0);
        capsule.write(locZ, "locZ", 0);
        capsule.write(currentSize, "currentSize", 0);
        capsule.writeSavableArrayList((ArrayList<GrassObject>) GrassUtils.toArrayList(grassPatchs, 2), "grassPatchs", new ArrayList<GrassObject>());
    }

    public int getSize() {
        return currentSize;
    }

    public int getLocX() {
        return locX;
    }

    public int getLocZ() {
        return locZ;
    }

    public void updateAt(Vector2f pointA, Vector2f pointB) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r1 = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                Rectangle r2 = new Rectangle((int) pointA.x, (int) pointA.y, (int) (pointB.x - pointA.x), (int) (pointB.y - pointA.y));
                if (r1.intersects(r2)) {
                    go.updateAt(pointA, pointB);
                }
            }
        }
    }

    public void generateAt(Vector2f pointA, Vector2f pointB) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r1 = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                Rectangle r2 = new Rectangle((int) pointA.x, (int) pointA.y, (int) (pointB.x - pointA.x), (int) (pointB.y - pointA.y));
                if (r1.intersects(r2)) {
                    go.generateAt(pointA, pointB);
                }
            }
        }
    }

    public void updateAt(Vector2f pointA) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                if (r.contains(pointA.x, pointA.y)) {
                    go.updateAt(pointA);
                }
            }
        }
    }

    public void generateAt(Vector2f pointA) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                if (r.contains(pointA.x, pointA.y)) {
                    go.generateAt(pointA);
                }
            }
        }
    }
}
