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

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;

/**
 * A lightweight bounding cylinder object. It does not serve as an actual
 * jME bounding volume, but is used only to calculate certain values when
 * batching foliage meshes.
 * 
 * @author Andreas
 */
public class BoundingCylinder {

    float radius;
    float yExtent;
    Vector3f center = new Vector3f();

    public BoundingCylinder() {
    }

    public BoundingCylinder(Geometry geom) {
        createFromGeom(geom);
    }

    
    
    /**
     * Calculate the bounding cylinder from a given geometry.
     * 
     * @param geom 
     */
    public void createFromGeom(Geometry geom) {
        BoundingBox box = (BoundingBox) geom.getMesh().getBound();
        FloatBuffer points = geom.getMesh().getFloatBuffer(VertexBuffer.Type.Position);
        center = box.getCenter();
        yExtent = box.getYExtent();
        radius = findRadius(points, center);
    }

    /**
     * Find the radius of the bounding cylinder given the center of the
     * bounding box, and the position buffer.
     * 
     * @param points The position buffer.
     * @param center The box center.
     * @return The radius.
     */
    protected float findRadius(FloatBuffer points, Vector3f center) {
        if (points == null) {
            return 0;
        }

        points.rewind();
        if (points.remaining() <= 2) // we need at least a 3 float vector
        {
            return 0;
        }

        TempVars vars = TempVars.get();

        float[] tmpArray = vars.skinPositions;

        float maxRad = 0;

        int iterations = (int) FastMath.ceil(points.limit() / ((float) tmpArray.length));
        for (int i = iterations - 1; i >= 0; i--) {
            int bufLength = Math.min(tmpArray.length, points.remaining());
            points.get(tmpArray, 0, bufLength);

            for (int j = 0; j < bufLength; j += 3) {
                vars.vect1.x = tmpArray[j] - center.x;
                vars.vect1.z = tmpArray[j + 2] - center.z;

                float rad = vars.vect1.x * vars.vect1.x + vars.vect1.z * vars.vect1.z;
                if (rad > maxRad) {
                    maxRad = rad;
                }

            }
        }

        vars.release();

        return FastMath.sqrt(maxRad);
    }

    /**
     * Merge this bounding cylinder with another bounding cylinder. The
     * current cylinder is changed into the merged one.
     * 
     * @param bc The other boundin cylinder.
     */
    public void merge(BoundingCylinder bc) {

        float bc_radius = bc.radius;
        float bc_yExtent = bc.yExtent;
        Vector3f bc_center = bc.center;

        // y extent first

        float y_min = center.y - yExtent;
        if (y_min > bc_center.y - bc_yExtent) {
            y_min = bc_center.y - bc_yExtent;
        }
        float y_Max = center.y + yExtent;
        if (y_Max < bc_center.y + bc_yExtent) {
            y_Max = bc_center.y + bc_yExtent;
        }

        center.y = (y_min + y_Max) * 0.5f;
        yExtent = y_Max - center.y;


        // Radius and final center adjustment.
        Vector2f c2 = new Vector2f(center.x, center.z);
        Vector2f bcc2 = new Vector2f(bc_center.x, bc_center.z);

        Vector2f diff = bcc2.subtract(c2);
        float length = diff.length();

        if (radius + bc_radius >= length) {
            if (bc_radius - radius >= length) {
                radius = bc_radius;
            }
        }

        Vector2f temp = diff.divide(length);

        float min = Math.min(-radius, length - bc_radius);
        float max = (Math.max(radius, length + bc_radius) - min) * 0.5f;

        Vector2f centTemp = new Vector2f(center.x,center.z).addLocal(temp.multLocal(max + min));
        center.x = centTemp.x;
        center.z = centTemp.y;
        
        radius = max;

    }

    public float getyExtent() {
        return yExtent;
    }

    public void setyExtent(float yExtent) {
        this.yExtent = yExtent;
    }

    public Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center = center;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
    
}
