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
package se.jod.biomonkey.paging.grid;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * A lightweight implementation of Cell2D
 * 
 * @author Andreas
 */
public class GenericCell2D implements Cell2D, Savable{
    
    protected short x, z;
    protected int hash;
    
    public GenericCell2D(){}
    
    /**
     * Create a cell with coordinates x and z.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     */
    public GenericCell2D(int x,int z)
    {
        this.x = (short) x;
        this.z = (short) z;
        hash = Grid2D.hash(x,z);
    }

    @Override
    public short getX() {
        return x;
    }
    
    @Override
    public short getZ() {
        return z;
    }

    @Override
    public void setX(short x) {
        this.x = x;
        this.hash = Grid2D.hash(x, z);
    }

    @Override
    public void setZ(short z) {
        this.z = z;
        this.hash = Grid2D.hash(x, z);
    }

    @Override
    public void set(short x, short z) {
        this.x = x;
        this.z = z;
        this.hash = Grid2D.hash(x, z);
    }
    
    @Override
    public String toString() {
        return '(' + Short.toString(x) + ',' + Short.toString(z) + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericCell2D other = (GenericCell2D) obj;
        if (this.hash != other.hash) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(x, "x", 0);
        capsule.write(z, "z", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        x = (short) capsule.readInt("x", 0);
        z = (short) capsule.readInt("z", 0);
        hash = Grid2D.hash(x, z);
    }
    
}//GenericCell2D
