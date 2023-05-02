package com.idflood.sky;

import com.idflood.sky.items.DynamicSkyBackground;
import com.idflood.sky.items.DynamicStars;
import com.idflood.sky.items.DynamicSun;
import com.idflood.sky.utils.CloudsBillboardItem;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SkyInterface;
import ru.arifolth.anjrpg.interfaces.Utils;

public class DynamicSky extends Node implements SkyInterface {
    private final CloudsBillboardItem clouds;
    private DynamicSun dynamicSun = null;
    private DynamicStars dynamicStars = null;
    private DynamicSkyBackground dynamicBackground = null;

    private GameLogicCoreInterface gameLogicCore = null;

    private float scaling = 10000;

    public DynamicSky(AssetManager assetManager, ViewPort viewPort, GameLogicCoreInterface gameLogicCore) {
        super("Sky");

        this.gameLogicCore = gameLogicCore;

        Node rootNode = gameLogicCore.getRootNode();

        dynamicSun = new DynamicSun(assetManager, viewPort, rootNode, scaling);
        rootNode.attachChild(dynamicSun);

        dynamicStars = new DynamicStars(assetManager, viewPort, scaling);
        dynamicStars.setShadowMode(ShadowMode.Off);

        dynamicBackground = new DynamicSkyBackground(assetManager, viewPort, rootNode);

        rootNode.setShadowMode(ShadowMode.Off);
        rootNode.attachChild(this);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/Clouds_L.png"));
        float factor = -1.0f;
        float units = -1.0f;
        mat.getAdditionalRenderState().setPolyOffset(factor, units);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        mat.getAdditionalRenderState().setDepthWrite(false);

        clouds = new CloudsBillboardItem("clouds", 1f);
        clouds.setMaterial(mat);
        gameLogicCore.getRootNode().attachChild(clouds);

        setQueueBucket(RenderQueue.Bucket.Sky);
        setCullHint(CullHint.Never);

        gameLogicCore.setSky(this);
    }

    public void attachStars() {
        gameLogicCore.getRootNode().attachChild(dynamicStars);
        dynamicStars.setAttached(true);
    }

    public void detachStars() {
        gameLogicCore.getRootNode().detachChild(dynamicStars);
        dynamicStars.setAttached(false);
    }

    @Override
    public Vector3f getSunDirection(){
        return dynamicSun.getSunDirection();
    }

    @Override
    public int getHours() {
        return dynamicSun.getSunSystem().getCurrentDate().getHours();
    }

    public void updateTime(){
        dynamicSun.updateTime();

        int hours = getHours();
        if (Utils.isBetween(hours, 0, 6) || Utils.isBetween(hours, 19, 23)) {
            if(!dynamicStars.isAttached()) {
                attachStars();
            }
        } else {
            if(dynamicStars.isAttached()) {
                detachStars();
            }
        }

        dynamicBackground.updateLightPosition(dynamicSun.getSunSystem().getPosition());
        dynamicStars.update(dynamicSun.getSunSystem().getDirection());
        dynamicStars.lookAt(dynamicSun.getSunSystem().getPosition(), Vector3f.ZERO);

        Vector3f playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation();
        playerLocation.x += Constants.MOUNTAINS_HEIGHT_OFFSET;
        clouds.setLocalTranslation(playerLocation);
    }

    @Override
    public void update(float tpf){
        updateTime();
    }
}
