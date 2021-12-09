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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the final steps into the grid subdivisions process. It hold and
 * generate all the GrassBlade information, and give it to the GrassAreaControl
 *
 * @author Stomrage
 * @version 0.1
 */
public class GrassHolder implements GrassObject {

    private int locX;
    private int locZ;
    private int holderSize;
    private Vector3f location;
    private ArrayList<GrassBlade> grassBlades;

    /**
     * Empty constructor
     *
     * @see Savable
     */
    public GrassHolder() {
    }

    /**
     * Create a new GrassHolder at the given location
     *
     * @param locX The x location
     * @param locZ The z location
     * @param holderSize The holder size (which is the size of a GrassHolder)
     */
    public GrassHolder(int locX, int locZ, int holderSize) {
        this.locX = locX;
        this.locZ = locZ;
        this.holderSize = holderSize;
        location = new Vector3f(locX, 0, locZ);
        grassBlades = new ArrayList<GrassBlade>();
    }

    public Vector3f getLocation() {
        return location;
    }

    public void generate() {
        grassBlades = new ArrayList<GrassBlade>();
        for (int x = locX; x < (locX + holderSize * 2); x++) {
            for (int z = locZ; z < (locZ + holderSize * 2); z++) {
                //We use the GrassFactory to generate the GrassBlade
                grassBlades.addAll(GrassFactory.getInstance().createGrass(locX, locZ, (locX + holderSize * 2), (locZ + holderSize * 2), x, z));
            }
        }
    }

    /**
     * An interface to get the whole GrassBlade information
     *
     * @return An array list that contain this holder grass information
     */
    public List<GrassBlade> getGrass() {
        return grassBlades;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(location, "location", new Vector3f());
        capsule.write(locX, "locX", 0);
        capsule.write(locZ, "locZ", 0);
        capsule.write(holderSize, "holderSize", 0);
        capsule.writeSavableArrayList(grassBlades, "grassBlades", new ArrayList<GrassBlade>());
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        location = (Vector3f) capsule.readSavable("location", new Vector3f());
        locX = capsule.readInt("locX", 0);
        locZ = capsule.readInt("locZ", 0);
        holderSize = capsule.readInt("holderSize", 0);
        grassBlades = capsule.readSavableArrayList("grassBlades", new ArrayList<GrassBlade>());
    }

    public int getSize() {
        return holderSize;
    }

    public int getLocX() {
        return locX;
    }

    public int getLocZ() {
        return locZ;
    }

    public void updateAt(Vector2f pointA, Vector2f pointB) {
        GrassFactory.getInstance().updateHeight(grassBlades);
    }

    public void generateAt(Vector2f pointA, Vector2f pointB) {
        this.generate();        
    }

    public void updateAt(Vector2f pointA) {
        GrassFactory.getInstance().updateHeight(grassBlades);
    }

    public void generateAt(Vector2f pointA) {
        this.generate();
    }
}
