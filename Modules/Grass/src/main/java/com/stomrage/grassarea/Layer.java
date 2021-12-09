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
import java.io.IOException;

/**
 * This class hold all the layer needed information to create all the GrassBlade of this layer
 * @author Stomrage
 * @version 0.1
 */
    public class Layer implements Savable{
        
        public float density;
        public float maxSize;
        public float minSize;
        public float minX;
        public float size;
        public int index;
        
        /**
         * An empty constructor @see Savable
         */
        public Layer(){
            
        }

        /**
         * Create a new layer
         * @param density The density of this layer
         * @param minSize The minimum grass size
         * @param maxSize The maximum grass size
         * @param minX The x index of the atlas texture
         * @param texSize The size of the atlas texture
         * @param index The color channel
         */
        public Layer(float density, float minSize, float maxSize, float minX, float texSize, int index) {
            this.density = density;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.minX = minX;
            this.size = texSize;
            this.index = index;
        }

        public void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(density, "density", 0);
            capsule.write(maxSize, "maxSize", 0);
            capsule.write(minSize, "minSize", 0);
            capsule.write(minX, "minX", 0);
            capsule.write(size, "size", 0);
            capsule.write(index, "index", 0);
        }

        public void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            density = capsule.readFloat("density", 0);
            maxSize = capsule.readFloat("maxSize", 0);
            minSize = capsule.readFloat("minSize", 0);
            minX = capsule.readFloat("minX", 0);
            size = capsule.readFloat("size", 0);
            index = capsule.readInt("index", 0);
        }
    }