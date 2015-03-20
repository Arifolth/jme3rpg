/*
 * Copyright (c) 2011, Andreas Olofsson
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
package se.jod.biomonkey;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * A rectangle with a center.
 * 
 * @author Andreas
 */
public class RectBounds implements Savable {

    protected float xMin, xMax, zMin, zMax;
    protected Vector3f center;

    /* Serialization constructor */
    public RectBounds(){}
    
    public RectBounds(float xMin, float zMin, float xMax, float zMax, Vector3f center) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        this.center = center;
    }

    public RectBounds(Vector3f center, float width, float height) {
        float hP = width / 2f;
        this.center = center;
        this.xMin = center.x - hP;
        this.xMax = center.x + hP;
        this.zMin = center.z - hP;
        this.zMax = center.z + hP;
    }
    
    public RectBounds(Vector3f center, float size){
        this(center,size,size);
    }

    public void Set(float xMin, float xMax, float zMin, float zMax, Vector3f center){
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        this.center = center;
    }
    
    public Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center = center;
    }

    public float getHeight() {
        return zMax-zMin;
    }

    public float getWidth() {
        return xMax-xMin;
    }

    public float getxMax() {
        return xMax;
    }

    public void setxMax(float xMax) {
        this.xMax = xMax;
    }

    public float getxMin() {
        return xMin;
    }

    public void setxMin(float xMin) {
        this.xMin = xMin;
    }

    public float getzMax() {
        return zMax;
    }

    public void setzMax(float zMax) {
        this.zMax = zMax;
    }

    public float getzMin() {
        return zMin;
    }

    public void setzMin(float zMin) {
        this.zMin = zMin;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(xMin, "xMin", 0);
        capsule.write(xMax, "xMax", 0);
        capsule.write(zMin, "zMin", 0);
        capsule.write(zMax, "zMax", 0);
        capsule.write(center, "center", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        xMin = capsule.readFloat("xMin", 0);
        xMax = capsule.readFloat("xMax", 0);
        zMin = capsule.readFloat("zMin", 0);
        zMax = capsule.readFloat("zMax", 0);
        center = (Vector3f) capsule.readSavable("center", null);
    }
}//RectBounds2D
