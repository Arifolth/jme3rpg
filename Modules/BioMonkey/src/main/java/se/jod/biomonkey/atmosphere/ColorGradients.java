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
package se.jod.biomonkey.atmosphere;

import com.jme3.math.ColorRGBA;
import java.util.ArrayList;

/**
 * A list of color values used to emulate the ambient color and sun color during
 * the day, provided that the standard sky is used. This system will be extended
 * when the weather system is added.
 *
 * @author Andreas
 */
public class ColorGradients {

    protected ArrayList<ColorFrame> sunColorGradient;
    protected ArrayList<ColorFrame> skyAmbientGradient;

    public ColorGradients() {

        skyAmbientGradient = new ArrayList<ColorFrame>(7);
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.95f, 0.95f, 0.95f, 1f), 1f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.7f, 0.7f, 0.65f, 1f), 0.625f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.6f, 0.55f, 0.4f, 1f), 0.5625f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.6f, 0.45f, 0.3f, 1f).multLocal(0.4f), 0.5f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.5f, 0.25f, 0.25f, 1f).multLocal(0.1f), 0.45f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.2f, 0.2f, 0.3f, 1f).multLocal(0.1f), 0.35f));
        skyAmbientGradient.add(new ColorFrame(new ColorRGBA(0.2f, 0.2f, 0.5f, 1f).multLocal(0.15f), 0f));

        sunColorGradient = new ArrayList<ColorFrame>(8);
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.95f, 0.95f, 0.95f, 1f), 1f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f), 0.75f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.8f, 0.75f, 0.55f, 1f).multLocal(1.3f), 0.5625f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.6f, 0.5f, 0.2f, 1f).multLocal(0.75f), 0.5f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.6f, 0.5f, 0.2f, 1f).multLocal(0.35f), 0.4725f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f).multLocal(0.15f), 0.45f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.2f, 0.2f, 0.25f, 1f).multLocal(0.4f), 0.3f));
        sunColorGradient.add(new ColorFrame(new ColorRGBA(0.3f, 0.3f, 0.5f, 1f).multLocal(0.2f), 0f));

        for (int i = 0; i < skyAmbientGradient.size(); i++) {
            skyAmbientGradient.get(i).color.a = 1f;
        }

        for (int i = 0; i < sunColorGradient.size(); i++) {
            sunColorGradient.get(i).color.a = 1f;
        }
    }

    /**
     * Get the sun color corresponding to the current position value p. The
     * position value is the suns height, normalized to [0,1].
     * 
     * @param p The sun height in the range [0,1]
     * @return The sun color value.
     */
    public ColorRGBA getSunColor(float p) {
        return getGradientColor(p, sunColorGradient);
    }

//    public ColorRGBA getMoonColor(float p, int phase){
//        ColorRGBA moonColor = new ColorRGBA(0.7f,0.8f,1.0f,1.0f);
//        
//        if(phase > 4){
//                phase = 8 - phase;
//        }
//            moonColor.multLocal(FastMath.clamp((p - 0.1f)*5f, 0f, 1f)*0.1f*(float)phase);
//            moonColor.a = 1.0f;
//        return moonColor;
//    }
    
    /**
     * Same as getSunColor(float p), but returns ambient color instead.
     * @param p
     * @return 
     */
    public ColorRGBA getSkyAmbientColor(float p) {
        return getGradientColor(p, skyAmbientGradient);
    }

    protected ColorRGBA getGradientColor(float p, ArrayList<ColorFrame> gradient) {
        ColorFrame frame;

        int minBoundNr = 0;
        float minBoundVal = -1f;

        for (int i = 0; i < gradient.size(); i++) {
            frame = gradient.get(i);
            if (frame.value < p && frame.value > minBoundVal) {
                minBoundNr = i;
                minBoundVal = frame.value;
            }
        }

        int maxBoundNr = 0;
        float maxBoundVal = 2f;

        for (int i = 0; i < gradient.size(); i++) {
            frame = gradient.get(i);
            if (frame.value > p && frame.value < maxBoundVal) {
                maxBoundNr = i;
                maxBoundVal = frame.value;
            }
        }

        float range = maxBoundVal - minBoundVal;
        ColorRGBA col = new ColorRGBA(gradient.get(minBoundNr).color);

        if (range != 0) {
            float rangePoint = (p - minBoundVal) / range;
            col.interpolate(gradient.get(maxBoundNr).color, rangePoint);
        }
        return col;
    }

    protected class ColorFrame {

        protected ColorRGBA color;
        protected float value;

        protected ColorFrame(ColorRGBA color, float value) {
            this.color = color;
            this.value = value;
        }
    }
}
