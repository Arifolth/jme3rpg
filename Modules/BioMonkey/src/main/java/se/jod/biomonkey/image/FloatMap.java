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
package se.jod.biomonkey.image;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import se.jod.biomonkey.IntMath;

/**
 * A float array wrapper class. Used to read and write from float arrays as if
 * they were 2D images.
 * 
 * @author Andreas
 */
public class FloatMap implements Savable {
    protected int imageSize;
    protected float[] array;
    
    protected float scale;
    protected int pageSize;
    
    boolean flipX = false, flipZ = true;
    
    public FloatMap(){}
    
    /**
     * Create a floatmap based on an image (a float array).
     * 
     * @param image the image.
     * @param imageSize the size of the image (width or height).
     * @param pageSize the pagesize used by the pageloader. Used to scale the map.
     */
    public FloatMap(float[] image, int imageSize, int pageSize) {
        this.array = image;
        this.imageSize = imageSize;
        this.pageSize = pageSize;
        this.scale = imageSize/(float)pageSize;
    }

    public float[] getArray() {
        return array;
    }
    
    public void setArray(float[] image){
        this.array = image;
    }

    /**
     * A method to get values.
     * 
     * @param x The x-coordinate.
     * @param z The z-coordinate.
     * @return The density value.
     */
    public float getValueUnfiltered(float x, float z) {
        //Flip
        // TODO shouldn't it be pageSize - 1 ??
        if(flipZ){
            z = pageSize - z;
        }
        if(flipX){
            x = pageSize - x;
        }
        int xx = IntMath.max((int)(x*scale),0);
        int zz = IntMath.min((int)(z*scale),imageSize - 1);
        
        return array[xx + imageSize*zz];
    }

    public int getImageSize() {
        return imageSize;
    }

    /**
     * Set the size of the float map. The array will be treated as an
     * imageSize*imageSize image.
     * @param imageSize 
     */
    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipZ() {
        return flipZ;
    }

    public void setFlipZ(boolean flipZ) {
        this.flipZ = flipZ;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(array, "image", null);
        capsule.write(flipX, "flipX", false);
        capsule.write(flipZ, "flipZ", true);
        capsule.write(imageSize, "imageSize", 0);
        capsule.write(pageSize, "pageSize", 0);
        capsule.write(scale, "scale", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        this.array = ic.readFloatArray("image", null);
        this.flipX = ic.readBoolean("flipX", false);
        this.flipZ = ic.readBoolean("flipZ", true);
        this.imageSize = ic.readInt("imageSize", 0);
        this.pageSize = ic.readInt("pageSize", 0);
        this.scale = ic.readFloat("scale", 1f);
        
    }
    
    /**
     * Get the density using bilinear filtering.
     * 
     * @param x The x-coordinate.
     * @param z The z-coordinate.
     * @param channel The colorchannel to sample from.
     * @return The density value.
     */
//    public float getDensityBilinear(float x, float z, int asdfa){
//        //Flip
//        if(flipZ){
//            z = pageSize - z;
//        }
//        if(flipX){
//            x = pageSize - x;
//        }
//        
//        float fracX = x - (int)x;
//        float fracZ = z - (int)z;
//        
//        int x0 = 0;
//        int x1 = 0;
//        int z0 = 0;
//        int z1 = 0;
//        
//        if (fracX < 0.01){
//            x0 = x1 = clamp((int)(x*scale),0,imageSize - 1);
//        } else {
//            x0 = max((int)(x*scale),0);
//            x1 = min((int)((x + 1)*scale),imageSize - 1);
//        }
//        if (fracZ < 0.01){
//            z0 = z1 = clamp((int)(z*scale),0,imageSize - 1);
//        } else {
//            z0 = max((int)(z*scale),0);
//            z1 = min((int)((z + 1)*scale),pageSize - 1);
//        }
//        
//        float v00 = getValue(x0,z0,channel);
//        float v01 = getValue(x0,z1,channel);
//        float v10 = getValue(x1,z0,channel);
//        float v11 = getValue(x1,z1,channel);
//        
//        float dens = ( v00 * (1 - fracX) + v10 * fracX ) * (1 - fracZ) + 
//                        (v01 * (1 - fracX) + v11 * fracX ) * fracZ;
//        
//        return dens;
//    }
    
}
