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
package se.jod.biomonkey;

/**
 * Important parameters for page loaders.
 * 
 * @author Andreas
 */
public class LoaderParams {
    
    protected int resolution;
    protected float farViewingRange;
    protected float fadeDistance;
    protected float fadeRange;

    public LoaderParams() {
    }

    public LoaderParams(int resolution, float farViewingRange, float fadeDistance, float fadeRange) {
        this.resolution = resolution;
        this.farViewingRange = farViewingRange;
        this.fadeDistance = fadeDistance;
        this.fadeRange = fadeRange;
    }

    public LoaderParams(int pageSize, int resolution, float farViewingRange) {
        this.resolution = resolution;
        this.farViewingRange = farViewingRange;
    }
    
    public float getFadeDistance() {
        return fadeDistance;
    }

    public void setFadeDistance(float fadeDistance) {
        this.fadeDistance = fadeDistance;
    }

    public float getFadeRange() {
        return fadeRange;
    }

    public void setFadeRange(float fadeRange) {
        this.fadeRange = fadeRange;
    }

    public float getFarViewingRange() {
        return farViewingRange;
    }

    public void setFarViewingRange(float farViewingRange) {
        this.farViewingRange = farViewingRange;
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        this.resolution = resolution;
    }
}
