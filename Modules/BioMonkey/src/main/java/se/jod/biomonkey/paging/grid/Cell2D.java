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

/**
 * The 2D cell interface. All implementations should use unique hashcodes,
 * and implement proper hashcode and equals methods.
 * 
 * @author Andreas
 */
public interface Cell2D {
    /**
     * Get the x-value of the cell.
     * @return The x-value of the cell. 
     */
    public short getX();
    /**
     * Get the z-value of the cell.
     * @return The z-value of the cell. 
     */
    public short getZ();
    
    /**
     * Set the x value of the cell.
     * NOTE: Never do this with pages. It messes with the paging system.
     * This is mostly used for serialization.
     * 
     * @param x 
     */
    public void setX(short x);
    
    /**
     * Used for serialization purposes only.
     * NOTE: Never do this with pages. It messes with the paging system.
     * This is mostly used for serialization.
     * 
     * @param x 
     */
    public void setZ(short z);
    
    /**
     * Used for serialization purposes only.
     * NOTE: Never do this with pages. It messes with the paging system.
     * This is mostly used for serialization.
     * 
     * @param x
     * @param z
     */
    public void set(short x, short z);
    
}//Cell2D
