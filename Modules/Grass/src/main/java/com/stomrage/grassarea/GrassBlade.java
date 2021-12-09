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
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * This class is here to hold all the GrassBlade information to create the mesh
 * @author Stomrage
 * @version 0.1
 */
public class GrassBlade implements Savable {

    public Vector3f position;
    public float size;
    public float color;
    public float texSize;
    public float minX;

    /**
     * An empty construct @see Savable
     */
    public GrassBlade() {

    }

    /**
     * Create a new GrassBlade
     * @param pos The position of the GrassBlade
     * @param color The perlin noise color information
     * @param s The size of this blade
     * @param minX x offset of the texture atlas
     * @param texSize size of the texture atlas
     */
    public GrassBlade(Vector3f pos, float color, float s, float minX, float texSize) {
        this.minX = minX;
        this.texSize = texSize;
        this.position = pos;
        this.color = color;
        this.size = s;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(size, "size", 0);
        capsule.write(color, "color", 0);
        capsule.write(minX, "minX", 0);
        capsule.write(texSize, "texSize", 0);
        capsule.write(position, "position", new Vector3f());
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.size = capsule.readFloat("size", 0);
        this.color = capsule.readFloat("color", 0);
        this.texSize = capsule.readFloat("texSize", 0);
        this.minX = capsule.readFloat("minX", 0);
        this.position = (Vector3f) capsule.readSavable("position", new Vector3f());
    }
}