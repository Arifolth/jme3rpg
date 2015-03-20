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

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.astronomy.PositionProvider;
import se.jod.biomonkey.astronomy.SimplePositionProvider;
import se.jod.biomonkey.atmosphere.clouds.Clouds;
import se.jod.biomonkey.atmosphere.sky.Moon;
import se.jod.biomonkey.atmosphere.sky.Sky;
import se.jod.biomonkey.atmosphere.sky.Stars;

/**
 * The atmosphere manager controlls everything related to the atmosphere,
 * including sky, fog and lighting.
 *
 * @author Andreas
 */
public class AtmosphereManager {

    protected Sky sky;
    protected Clouds clouds;
    protected FogManager fogManager;
    protected ColorGradients gradients;
    protected PositionProvider positionProvider;
    protected DirectionalLight sun;
    protected Moon moon;
//    protected Stars stars;
    protected AmbientLight amb;
    protected ColorRGBA sunColor = ColorRGBA.White;
    protected ColorRGBA ambientColor = ColorRGBA.Gray;
    protected boolean sunUp = false;
    protected boolean starsVisible = false;
    protected boolean dynamic = true;
    protected boolean HDR = false;

    public AtmosphereManager(EcoManager em) {
        ColorRGBA nightCol = new ColorRGBA(0.318f, 0.345f, 0.525f, 1f).multLocal(0.1f);
        nightCol.a = 1f;
        em.getApp().getViewPort().setBackgroundColor(nightCol);
        positionProvider = new SimplePositionProvider();

        gradients = new ColorGradients();
        sky = new Sky();
        em.getRootNode().attachChild(sky.getSkyDome());
        sun = new DirectionalLight();
        sun.setName("Sun");
        moon = new Moon();
        em.getRootNode().attachChild(moon.getMoonGeom());
        clouds = new Clouds(sky);
        fogManager = new FogManager();
//        stars = new Stars(1000, 990);
//        stars.load(false);
//        em.getRootNode().attachChild(stars.getStarGeometry()); // Attach/detach based on time of day later..
        amb = new AmbientLight();
        amb.setColor(ambientColor);
        em.getRootNode().addLight(amb);

        Vector3f location = EcoManager.getInstance().getCamera().getLocation();
        sky.getSkyDome().setLocalTranslation(location.x, 0, location.z);
//        stars.getStarGeometry().setLocalTranslation(location.x, 0, location.z);

        // ------ Positions ------
        _update(0);

    }

    /**
     * Call each frame.
     *
     * @param tpf
     */
    public void update(float tpf) {
        Vector3f location = EcoManager.getInstance().getCamera().getLocation();
        sky.getSkyDome().setLocalTranslation(location.x, 0, location.z);
//        stars.getStarGeometry().setLocalTranslation(location.x, 0, location.z);

        // ------ Positions ------
        positionProvider.update(tpf);

        if (dynamic) {
            _update(tpf);
        }
    }

    protected void _update(float tpf) {

//        stars.update(tpf);


        // ------ Sun ------            
        Vector3f sunDirection = positionProvider.getSunDirection();

        // Get the suns height in the range 0 to 1.
        float position = sunDirection.y / positionProvider.getMaxHeight() * 0.5f + 0.5f;
        sunColor = gradients.getSunColor(position);
        //sunColor = gradients.getSunColor(FastMath.clamp(position + 0.1f,0,1));

        //         Switch lights
        if (sunDirection.y > -0.2f) {
            if (sunUp == false) {
                EcoManager.getInstance().getRootNode().addLight(sun);
                EcoManager.getInstance().getRootNode().removeLight(moon.getMoonLight());
                sunUp = true;
            }
        } else if (sunDirection.y <= -0.2f) {
            if (sunUp == true) {
                EcoManager.getInstance().getRootNode().removeLight(sun);
                EcoManager.getInstance().getRootNode().addLight(moon.getMoonLight());
                sunUp = false;
            }
        }

        if (sunUp) {
            sun.setColor(sunColor.mult(0.7f));
            sun.setDirection(sunDirection.negate());
        } else {
            moon.getMoonLight().setColor(sunColor.mult(2f));
            moon.getMoonLight().setDirection(sunDirection);
        }

        moon.getMoonGeom().setLocalTranslation(sunDirection.mult(-950).addLocal(EcoManager.getInstance().getCamera().getLocation()));
        moon.getMoonGeom().lookAt(EcoManager.getInstance().getCamera().getLocation(), Vector3f.UNIT_Y);
        boolean nextIsDusk = (positionProvider.getCalendar().getHour() > 12);

        // Stuff to avoid using 2 lightsources and detach/attach stars.
        if (sunDirection.y < -0.1 && sunDirection.y >= -0.2f) {
            if (nextIsDusk) {
                sun.getColor().multLocal(1 + (sunDirection.y + 0.1f) * 10);
            } else {
                sun.getColor().multLocal((0.2f + sunDirection.y) * 10);
            }
        } else if (sunDirection.y < -0.2 && sunDirection.y >= -0.3f) {
            if (nextIsDusk) {
                moon.getMoonLight().getColor().multLocal((-sunDirection.y - 0.2f) * 10);
            } else {
                moon.getMoonLight().getColor().multLocal(1 - (sunDirection.y + 0.3f) * 10);
            }
        }

        // ------ Ambient lighting ------
        ambientColor = gradients.getSkyAmbientColor(FastMath.clamp(position + 0.1f, 0, 1));
        amb.setColor(ambientColor.mult(2f));

        // ------ Update Materials ------
        Material skyMat = sky.getMaterial();
        skyMat.setVector3("LightDir", sunDirection);
        skyMat.setColor("SunColor", sunColor);
        skyMat.setColor("AmbientColor", ambientColor);

        // ------ Fog ------

        if (fogManager != null) {
            fogManager.update(this, sunDirection, position);
        }
    }

    /**
     * This will cause some shaders to return HDR color values. Under
     * construction.
     *
     * @param HDR
     */
    public void setUseHDR(boolean HDR) {
        this.HDR = HDR;
        sky.getMaterial().setBoolean("HDR", HDR);
    }

    public boolean getUseHDR() {
        return HDR;
    }

    /**
     * Ambient color is used for various elements in the system. The value is
     * not equal to the ambient light color.
     *
     * @return
     */
    public ColorRGBA getAmbientColor() {
        return ambientColor;
    }

    /**
     * This value is used for various elements in the system. It is not equal to
     * the sun light color.
     *
     * @return
     */
    public ColorRGBA getSunColor() {
        return sun.getColor();
    }

    public ColorGradients getColorGradients() {
        return gradients;
    }

    public PositionProvider getPositionProvider() {
        return positionProvider;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * If the sky is set to dynamic, it'll be updated every frame. This is the
     * default, and should be used if the sun is supposed to change position.
     *
     * @param dynamic
     */
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }
    
    /**
     * Set the timescale. This changes the length of the day. Time increases
     * by timeDelta*timeScale, so setting the value to 60 means 1 second of real
     * time becomes 1 minute in biomonkey time.
     * @param scale 
     */
    public void setTimeScale(float scale){
        this.positionProvider.getCalendar().setTMult(scale);
    }
    
    public float getTimeScale(){
        return this.getPositionProvider().getCalendar().gettMult();
    }

    public Sky getSky() {
        return sky;
    }

    public Clouds getClouds() {
        return clouds;
    }

    public FogManager getFogManager() {
        return fogManager;
    }

    public DirectionalLight getSun() {
        return sun;
    }

    public Moon getMoon() {
        return moon;
    }
}
