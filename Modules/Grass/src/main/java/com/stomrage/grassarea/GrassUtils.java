/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import java.util.ArrayList;
import java.util.List;

/**
 * All the needed static function to help into the GrassArea process
 * @author Stomrage
 * @version 0.1
 */
public class GrassUtils {

    /**
     * This function convert a 2D array into a savable array list
     * @param array The 2D array
     * @param size The size of the array
     * @return An array list that represent the 2D array
     */
    public static ArrayList<GrassObject> toArrayList(GrassObject[][] array, int size) {
        ArrayList<GrassObject> list = new ArrayList<GrassObject>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                list.add(array[i][j]);
            }
        }
        return list;
    }

    /**
     * This function convert an array list into a 2D array
     * @param list The list to convert
     * @param size The size of the 2d array
     * @return A 2D array with all the ArrayList information converted
     */
    public static GrassObject[][] toArray2D(List<GrassObject> list, int size) {
        GrassObject[][] array = new GrassObject[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                array[i][j] = list.get(i + j * size);
            }
        }
        return array;
    }
    
}
