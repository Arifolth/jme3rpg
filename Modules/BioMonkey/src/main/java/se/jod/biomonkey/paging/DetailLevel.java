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
package se.jod.biomonkey.paging;

/**
 * This class is used for representing a level of detail. The paging manager
 * handles these objects internally.
 * 
 * @author Andreas
 */
public class DetailLevel {
    
    protected float nearDist, farDist;
    //Fading
    protected float fadingRange;
    protected float farTransDist;
    protected boolean fadeEnabled;
    
    public DetailLevel(){
    }

    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    public float getFadingRange() {
        return fadingRange;
    }

    public float getFarDist() {
        return farDist;
    }

    public float getFarTransDist() {
        return farTransDist;
    }

    public float getNearDist() {
        return nearDist;
    }

    public void setFarDist(float farDist) {
        this.farDist = farDist;
    }

    public void setNearDist(float nearDist) {
        this.nearDist = nearDist;
    }

    public void setTransition(float fadingRange)
    {
        if (fadingRange > 0) {
            //Setup valid transition
            this.fadingRange = fadingRange;
            fadeEnabled = true;
        } else {
            //<= 0 indicates disabled transition
            fadingRange = 0;
            fadeEnabled = false;
        }

        farTransDist = farDist + fadingRange;
    }
    
} //DetailLevel
