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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * Provides simple positions for sun and moon, as well as the current moon-phase.
 * This system is efficient and allows the use of only one directional light.
 * 
 * @author Andreas
 */
public class SimplePositionProvider implements PositionProvider {
        
    protected Vector3f sunDir = new Vector3f(0,1,0);
    protected Vector3f moonDir = new Vector3f(0,-1,0);
    
    // Defaults to 29 days which is about the length of a real synodic month.
    protected int daysInMoonCycle = 29;
    protected int moonDaysCount = 0;
    protected int moonPhase = 0;
    
    protected Calendar calendar;
    // The time when the sun position is at its highest (equal to declination).
    protected float solarNoon = 14;
    
    // Declination is the maximum height of the sun. A declination of 0
    // means the sun climbs all the way up to zenith. A value of 90 (degrees)
    // means the sun just floats around the horizon.
    protected float declination = 30; //Default 30 degrees.
    //Cosine and sine of the declination (in radians).
    protected float cDecRad, sDecRad;
    
    protected int oldHour;
    
    public SimplePositionProvider(){
        //Year, month, day, hour, minute, multiplier.
        calendar = new Calendar(0,0,0,12,0,10);
        moonDaysCount = calendar.getDayInYear() % daysInMoonCycle;
        calcDecVals();
        update(0);
    }
    
    private void calcDecVals(){
        // Angle in radians.
        float ang = (90 - declination)*FastMath.PI/180f;
        cDecRad = FastMath.cos(ang);
        sDecRad = FastMath.sin(ang);
    }
    
    @Override
    public void update(float tpf) {
        
        calendar.update(tpf);
        int hour = calendar.getHour();
        float time = (float) (hour + calendar.getMinute()/60f);
        float ang = time / 12f * FastMath.PI - FastMath.PI *0.5f;
        
        float sA = FastMath.sin(ang);
        sunDir.x = -FastMath.cos(ang);
        sunDir.y = sA*cDecRad;
        sunDir.z = -sA*sDecRad;
        
        moonDir = sunDir.negate();
        // Doing this at mid day to avoid updating the moon texture during night.
        if(hour == 12 && oldHour != hour){
            moonDaysCount++;
            if(moonDaysCount > daysInMoonCycle){
                moonDaysCount = 1;
            }
            updateMoonPhase();
            
        }
        oldHour = hour;
    }
    
    protected void updateMoonPhase(){
         moonPhase = (int) ((moonDaysCount % daysInMoonCycle) / (float)daysInMoonCycle * 7f);
    }

    @Override
    public Vector3f getSunDirection() {
        return sunDir;
    }

    @Override
    public Vector3f getMoonDirection() {
        return moonDir;
    }

    @Override
    public int getMoonPhase() {
        return moonPhase;
    }

    @Override
    public Calendar getCalendar() {
        return calendar;
    }
    
    /**
     * Set the number of days in a synodic month ("moon" month). This is
     * the time during which the moon will complete a full cycle.
     * 
     * @param days 
     */
    public void setDaysInSynodicMonth(int days){
        this.daysInMoonCycle = days;
    }

    /**
     * Set the declination value of the sun. The value is between 0
     * and 90, and the lower it is, the higher the sun will climb.
     * A value of 0 means the sun will pass zenith at solar noon.
     * 
     * @param declination 
     */
    public void setDeclination(float declination){
        this.declination = FastMath.clamp(declination,0,90);
        calcDecVals();
    }

    @Override
    public float getMaxHeight() {
        return sDecRad;
    }

    @Override
    public float getMinHeight() {
        return -sDecRad;
    }
    // Get the current moon phase as a string.
    public String getMoonPhaseVerbose(){
        String str;
        switch(moonPhase){
            case 0:
                str = "New Moon";
                break;
            case 1:
                str = "Waxing Crescent";
                break;
            case 2:
                str = "First Quarter Moon";
                break;
            case 3:
                str = "Waxing Gibbous";
                break;
            case 4:
                str = "Full Moon";
                break;
            case 5:
                str = "Waning Gibbous";
                break;
            case 6:
                str = "Third Quarter Moon";
                break;
            case 7:
                str = "Waning Crescent";
                break;
            default:
                str = "No Moonphase!";
                break;
                
        }
        return str;
    }
}
