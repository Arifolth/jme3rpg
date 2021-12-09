/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.export.Savable;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * The GrassObject interface. This interface is used to make the grass holding process more generic
 * and also make all of this GrassObject savable
 * @author Stomrage
 * @version 0.1
 */
public interface GrassObject extends Savable{
    /**
     * Give the location of the GrassObject
     * @return grass object location (center)
     */
    public Vector3f getLocation();
    /**
     * This function run through the GrassArea tree to generate the GrassBlade information
     */
    public void generate();
    
    /**
     * Get the size of this GrassObject
     * @return The size of the GrassObject
     */
    public int getSize();
    
    /**
     * This method update the GrassBlade y position of the grass holder at the given location
     * @param x The x position
     * @param z The z position
     */
    public void updateAt(Vector2f pointA);
    
    /**
     * This method update all the GrassBlade y position in a rectangle starting from pointA to pointB
     * @param pointA Starting position of the square
     * @param pointB End position of the square
     */
    public void updateAt(Vector2f pointA, Vector2f pointB);
    
    /**
     * Same as updateAt but this time the GrassBlades are generated
     * @param x The x position
     * @param z The z position
     */
    public void generateAt(Vector2f pointA);
    
    /**
     * Same as updateAt but this time the GrassBlades are generated
     * @param pointA Starting position of the square
     * @param pointB End position of the square
     */    
    public void generateAt(Vector2f pointA, Vector2f pointB);
    
    public int getLocX();
    public int getLocZ();
    
}
