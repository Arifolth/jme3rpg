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
 * This class is just here to make a Key for the LayerHashMap
 * @author Stomrage
 * @version 0.1
 */
    public class HashKey implements Savable{

        private int densityIndex;
        private int colorIndex;
        
        public HashKey(){
            
        }

        /**
         * Create a new key
         * @param densityIndex The density index of the DensityMap
         * @param colorIndex The ColorChannel index
         */
        public HashKey(int densityIndex, int colorIndex) {
            this.densityIndex = densityIndex;
            this.colorIndex = colorIndex;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HashKey)) {
                return false;
            }
            HashKey tempK = (HashKey) obj;
            return this.densityIndex == tempK.densityIndex && this.colorIndex == tempK.colorIndex;
        }

        @Override
        public int hashCode() {
            return densityIndex ^ colorIndex;
        }

        public void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(densityIndex, "densityIndex", 0);
            capsule.write(colorIndex, "colorIndex", 0);
        }

        public void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            densityIndex = capsule.readInt("densityIndex", 0);
            colorIndex = capsule.readInt("colorIndex", 0);
        }
    }
