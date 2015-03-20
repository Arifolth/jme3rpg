/*
 *  Copyright (c) 2011 David Mignot
 *  
 *  previous version:
 *  Copyright (c) 2008 Adriano Dalpane
 *  All rights reserved.
 *
 *  This file was found in JIVES, a free software licensed under 
 *  GNU General Public License
 *
 *  
 *  This is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.idflood.sky.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

public class SunSystem {
    private static final Logger logger = Logger.getLogger(SunSystem.class.getName());
    private static float distScaleFactor = 10000;
    double lambda;
    double beta;
    double r;
    double lambdaOffset;
    double betaOffset;
    double rOffset;

    Date currentDate;

    Vector3f sunPosition = new Vector3f();
    private boolean debug = false;
    private double xs;
    private double ys;
    private double JD;
    private float siteLat;
    private float siteLon;
    private float HR;
    
    /**
     * CONSTRUCTOR: build a solar system sun giving earth date and ecliptic
     * coordinates offsets.<br>
     * 
     * @param currentDate -
     *            Date from which sun position is set
     * @param lambdaOffset -
     *            Ecliptic longitude offset [h], or 0 for Solar System Sun
     *            longitude
     * @param betaOffset -
     *            Ecliptic latitude offset [°], or 0 for Solar System Sun
     *            latitude
     * @param rOffset -
     *            Distance offset [Km], or 0 for Solar System Sun distance
     */
    public SunSystem(Date currentDate, double lambdaOffset, double betaOffset, double rOffset) {
        this.currentDate = currentDate;
        this.lambdaOffset = lambdaOffset;
        this.betaOffset = betaOffset;
        this.rOffset = rOffset;

        calculateCartesianCoords(currentDate);
    }

    /**
     * SOURCE: http://graphics.ucsd.edu/~henrik/papers/nightsky/nightsky.pdf
     * 
     * @param date -
     *            current date
     */
    private Vector3f calculateCartesianCoords(Date date) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeZone(TimeZone.getTimeZone("UTC"));
        gc.setTime(date);
        int DD = gc.get(Calendar.DAY_OF_MONTH);
        int MM = gc.get(Calendar.MONTH) + 1;
        int YY = gc.get(Calendar.YEAR);
        int HOUR = gc.get(Calendar.HOUR_OF_DAY);
        int MN = gc.get(Calendar.MINUTE);

        // fecha juliana
        HR = HOUR + (MN / 60.f);
        double GGG = 1;
        if (YY <= 1585)
            GGG = 0;
        JD = -1 * Math.floor(7 * (Math.floor((MM + 9) / 12.f) + YY) / 4.f);
        double S = 1;
        if ((MM - 9) < 0)
            S = -1;
        double A = Math.abs(MM - 9);
        double J1 = Math.floor(YY + S * Math.floor(A / 7.f));
        J1 = -1 * Math.floor((Math.floor(J1 / 100.f) + 1) * 3 / 4.f);
        JD = JD + Math.floor(275 * MM / 9.f) + DD + (GGG * J1);
        JD = JD + 1721027 + 2 * GGG + 367 * YY - 0.5;
        JD = JD + (HR / 24.f);

        double T = (JD - 2451545.0) / 36525;
        double M = 6.24 + 628.302 * T;
        lambda = 4.895048 + 628.331951 * T + (0.033417 - 0.000084 * T) * Math.sin(M) + 0.000351 * Math.sin(2 * M);
        r = (1.000140 - (0.016708 - 0.000042 * T) * Math.cos(M) - 0.000141 * Math.cos(2 * M));
        beta = 0;

        // Apply offsetts
        lambda += lambdaOffset;
        beta += betaOffset;
        r += rOffset;

        sunPosition = new Vector3f(
            (float) (r * Math.sin(beta)),
            (float) (r * Math.sin(lambda) * Math.cos(beta)),
            (float) (r * Math.cos(lambda) * Math.cos(beta))
        );

        // latitude, longitude of the place on earth
        float lat = siteLat;
        float lon = siteLon + (float) (Math.PI * 3 / 2);
        // Convert to local horizon coordinates
        double eta = 0.409093 - 0.000227 * T; // obliquity of the ecliptic
        double LMST = 4.894961 + 230121.675315 * T + lon; // local sidereal
        // time
        Matrix3f matRx = new Matrix3f();
        Matrix3f matRy = new Matrix3f();
        Matrix3f matRz = new Matrix3f();

        matRx.fromAngleNormalAxis((float) -eta, new Vector3f(1, 0, 0));
        matRy.fromAngleNormalAxis((float) -(lat - Math.PI / 2), new Vector3f(0,
                        1, 0));
        matRz.fromAngleNormalAxis((float) -LMST, new Vector3f(0, 0, 1));
        sunPosition = matRz.mult(matRx.mult(matRy.mult(sunPosition)));

        // Get long, lat
        xs = Math.atan2(sunPosition.z, -sunPosition.x);
        ys = Math.atan2(sunPosition.y, -sunPosition.x);

        // Scale distance
        sunPosition.multLocal(distScaleFactor);

        if (debug) {
            logger.info("SUN SYSTEM > " + DD + "/" + MM + "/" + YY
                            + " - " + HOUR + ":" + MN);
            logger.info("             > POS " + sunPosition);
        }
        
        return sunPosition;
    }

    public void enableDebug(boolean enable) {
            debug = true;
    }

    /**
     * 
     * @return current date
     */
    public Date getCurrentDate() {
            return currentDate;
    }

    /**
     * 
     * @return sun latitude within (-PI, PI) interval
     */
    public double getLatitude() {
            return ys;
    }

    /**
     * 
     * @return sun longitude within (-PI, PI) interval
     */
    public double getLongitude() {
            return xs;
    }

    /**
     * 
     * @return sun position, centered at the origin
     */
    public Vector3f getPosition() {
            return sunPosition;
    }
    
    /**
     * 
     * @return sun direction as a normalized vector
     */
    public Vector3f getDirection() {
            return sunPosition.normalize().mult(-1);
    }
    
    
    /**
     * 
     * @return scale factor (distance of the sun)
     */
    public float getScaleFactor() {
            return distScaleFactor;
    }
    
    /**
     * 
     * @return earth site latitude
     */
    public float getSiteLatitude() {
            return siteLat;
    }

    /**
     * 
     * @return earth site longitude
     */
    public float getSiteLongitude() {
            return siteLon;
    }

    /**
     * 
     * Set earth site latitude
     */
    public void setSiteLatitude(float siteLat) {
            this.siteLon = siteLat;
    }

    /**
     * 
     * Set earth site longitude
     */
    public void setSiteLongitude(float siteLon) {
            this.siteLon = siteLon;
    }

    /**
     * Updates sun position
     * 
     * @param elapsHH -
     *            Elapsed hours
     * @param elapsMM -
     *            Elapsed minutes
     * @param elapsSS -
     *            Elapsed seconds
     */
    public Vector3f updateSunPosition(int elapsHH, int elapsMM, int elapsSS) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(currentDate);
        gc.add(Calendar.HOUR_OF_DAY, elapsHH);
        gc.add(Calendar.MINUTE, elapsMM);
        gc.add(Calendar.SECOND, elapsSS);
        currentDate = gc.getTime();

        return calculateCartesianCoords(gc.getTime());
    }
}
