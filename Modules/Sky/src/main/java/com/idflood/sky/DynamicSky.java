package com.idflood.sky;

import com.idflood.sky.items.DynamicSkyBackground;
import com.idflood.sky.items.DynamicStars;
import com.idflood.sky.items.DynamicSun;
import com.idflood.sky.utils.CloudsBillboardItem;
import com.idflood.sky.utils.HorizonBillboardItem;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.util.TangentBinormalGenerator;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.*;

import static ru.arifolth.anjrpg.interfaces.Constants.INITIAL_MOUNTAINS_OFFSET;

public class DynamicSky extends Node implements SkyInterface {
    private final CloudsBillboardItem clouds;
    private final HorizonBillboardItem horizon;

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

        dynamicStars = new DynamicStars(assetManager, viewPort, scaling);
        dynamicStars.setShadowMode(ShadowMode.Off);

        dynamicBackground = new DynamicSkyBackground(assetManager, viewPort, rootNode);

        rootNode.setShadowMode(ShadowMode.Off);
        rootNode.attachChild(this);


        Node mountainNode = new Node();
        horizon = new HorizonBillboardItem(assetManager, "Mountain", 1f);
        mountainNode.attachChild(horizon);
        LodUtils.setUpModelLod(mountainNode);
        mountainNode = GeometryBatchFactory.optimize(mountainNode, true);
        TangentBinormalGenerator.generate(mountainNode, true);
        mountainNode.updateModelBound();
        mountainNode.setLocalTranslation(INITIAL_MOUNTAINS_OFFSET);
        gameLogicCore.getRootNode().attachChild(mountainNode);

        clouds = new CloudsBillboardItem(assetManager, "Clouds", 1f);
        gameLogicCore.getRootNode().attachChild(clouds);

        setQueueBucket(RenderQueue.Bucket.Sky);
        setCullHint(CullHint.Never);

        gameLogicCore.setSky(this);
    }

    public void attachStars() {
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(dynamicStars);
            gameLogicCore.getRootNode().detachChild(dynamicSun);
        });

        dynamicStars.setAttached(true);
    }

    public void detachStars() {
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().detachChild(dynamicStars);
            gameLogicCore.getRootNode().attachChild(dynamicSun);
        });

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

        Vector3f playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation().clone();
        playerLocation.x += Constants.HEIGHT_OFFSET;
        playerLocation.y += Constants.HEIGHT_OFFSET;
        clouds.setLocalTranslation(playerLocation);

        playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation().clone();
        playerLocation.addLocal(INITIAL_MOUNTAINS_OFFSET);
        horizon.setLocalTranslation(playerLocation);
    }

    @Override
    public void update(float tpf){
        updateTime();
    }

    @Override
    public DirectionalLight getSunLight(){
        return dynamicSun.getSunLight();
    }
}
