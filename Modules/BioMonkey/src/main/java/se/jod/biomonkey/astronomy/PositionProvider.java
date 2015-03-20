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
package se.jod.biomonkey.astronomy;

import com.jme3.math.Vector3f;

/**
 * This interface should be implemented by classes that provide
 * sun and moon data to the sky.
 * 
 * @author Andreas
 */
public interface PositionProvider {
    
    /**
     * This method should return the direction to the sun. It needs to
     * be normalized.
     * 
     * @return The sun direction.
     */
    public Vector3f getSunDirection();
    
    /**
     * See getSunDirection.
     * 
     * @return The moon direction.
     */
    public Vector3f getMoonDirection();
    
    /**
     * Get the current phase of the moon.
     * 
     * @return The moon-phase. 0 is new mooon.
     */
    public int getMoonPhase();
    
    /** 
     * Get the maximum/minimum normalized height (y-max/y-min).
     */
    public float getMaxHeight();
    public float getMinHeight();
    
    /**
     * Get the calendar used by the positions provider.
     * @return 
     */
    public Calendar getCalendar();
    
    /**
     * Should be run every frame.
     * @param tpf 
     */
    public void update(float tpf);
}
