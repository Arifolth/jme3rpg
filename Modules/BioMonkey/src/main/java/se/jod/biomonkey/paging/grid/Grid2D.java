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

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * This class is a container for Cell2D objects. It extends hashmap, and uses
 * custom methods to add and remove Cell2D objects from the map.
 * 
 * @author Andreas
 */
public class Grid2D<T extends Cell2D> extends HashMap<Integer,T> {
    
    protected static final Logger log = Logger.getLogger(Grid2D.class.getName());
    protected int sizeX = 2, sizeZ = 2;
    
    /**
     * The default constructor. Creates a grid with
     * x and z dimensions both 4 (the initial map size is 16).
     */
    public Grid2D(){
        this(4,4);
    }
    
    /**
     * Creates a grid with size sizeX*sizeZ.
     * 
     * @param sizeX The x-size of the grid.
     * @param sizeZ The z-size of the grid.
     */
    public Grid2D(int sizeX, int sizeZ){
        super(sizeX*sizeZ);
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
    }
    
    /**
     * Replaces the cell at the specified coords with the cell newCell, and 
     * returns the old cell. If no cell exists at the specified coords, 
     * the method returns null.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @param newCell The new cell.
     * @return The cell currently at position (x,z), or null.
     */
    public T setCell(int x, int z, T newCell){
        return setCell(hash(x,z),newCell);
    }
    
    /**
     * Replaces the cell oldCell with the cell newCell, and returns the 
     * old cell. If no cell exists at the specified coords,  the method 
     * returns null.
     * 
     * @param oldCell The old cell.
     * @param newCell The new cell.
     * @return oldCell or null.
     */
    public T setCell(T oldCell, T newCell){
        return setCell(oldCell.hashCode(),newCell);
    }
    
    /**
     * Replaces the cell using the hashcode "hash", with the cell newCell, 
     * and returns the old cell. If no cell with hashcode "hash" exists, 
     * the Method returns null.
     * 
     * @param hash The hash of the current cell.
     * @param newCell The new cell.
     * @return The gridcell with hashcode "hash", or null.
     */
    public T setCell(int hash, T newCell){
        T c = remove(hash);
        put(newCell.hashCode(),newCell);
        return c;
    }
    
    public T getCell(Cell2D cell){
        return getCell(cell.hashCode());
    }
    
    /**
     * Gets the cell at position x,z.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @return The cell at (x,z), or null if no such cell exists.
     */
    public T getCell(int x, int z){
        return getCell(hash(x,z));
    }
   
    /**
     * Gets the cell with hashcode "hash".
     * 
     * @param hash the hashcode of the cell.
     * @return The cell with hashCode "hash", or null if no such cell exists.
     */
    public T getCell(int hash){
        return get(hash);
    }
    
    /**
     * Removes the cell with the given coordinates, if it's in the grid.
     * 
     * @param x The x-coordinate of the cell.
     * @param z The z-coordinate of the cell.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(int x, int z){
        return removeCell(hash(x,z));
    }
    
    /**
     * Removes a cell if it's in the grid.
     * 
     * @param cell The cell to be removed.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(T cell){
        return removeCell(cell.hashCode());
    }
    
    /**
     * Removes the cell with the given hashcode if it's in the grid.
     * 
     * @param hash The hashcode of the cell.
     * @return The cell, or null if it's not in the grid.
     */
    public T removeCell(int hash){
        return remove(hash);
    }
    
    protected static final short hashRadius = (1 << 14);
    /**
     * This method generates a 32 bit hashcode for each pair of 
     * integers. The first 15 bits are used for x, and the next 15 for z. 
     * The last two aren't used. The hash is unique assuming x and z are 
     * both in the interval [-hashRadius,hashRadius - 1].
     * 
     * @param x the x-coordinate of the cell
     * @param z the z-coordinate of the cell
     */
    public static int hash(int x, int z){
        return x + hashRadius + ((z + hashRadius) << 15);
    }
    
} // Grid2D
