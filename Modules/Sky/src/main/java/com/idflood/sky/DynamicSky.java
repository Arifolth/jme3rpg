package com.idflood.sky;

import com.idflood.sky.items.DynamicSkyBackground;
import com.idflood.sky.items.DynamicStars;
import com.idflood.sky.items.DynamicSun;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import ru.arifolth.anjrpg.interfaces.SkyInterface;

public class DynamicSky extends Node implements SkyInterface {
    private DynamicSun dynamicSun = null;
    private DynamicStars dynamicStars = null;
    private DynamicSkyBackground dynamicBackground = null;
    
    private float scaling = 10000;
    
    public DynamicSky(AssetManager assetManager, ViewPort viewPort, Node rootNode) {
        super("Sky");

        dynamicSun = new DynamicSun(assetManager, viewPort, rootNode, scaling);
        rootNode.attachChild(dynamicSun);
        
        dynamicStars = new DynamicStars(assetManager, viewPort, scaling);
        dynamicStars.setShadowMode(ShadowMode.Off);
        rootNode.attachChild(dynamicStars);
        
        dynamicBackground = new DynamicSkyBackground(assetManager, viewPort, rootNode);

        rootNode.setShadowMode(ShadowMode.Off);
        rootNode.attachChild(this);
    }
    
    @Override
    public Vector3f getSunDirection(){
        return dynamicSun.getSunDirection();
    }

    public void updateTime(){
        dynamicSun.updateTime();
        dynamicBackground.updateLightPosition(dynamicSun.getSunSystem().getPosition());
        dynamicStars.update(dynamicSun.getSunSystem().getDirection());
        dynamicStars.lookAt(dynamicSun.getSunSystem().getPosition(), Vector3f.ZERO);
    }

    @Override
    public void update(float tpf){
        updateTime();
    }
}
