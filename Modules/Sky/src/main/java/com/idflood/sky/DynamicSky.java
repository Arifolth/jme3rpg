package com.idflood.sky;

import com.idflood.sky.items.DynamicSkyBackground;
import com.idflood.sky.items.DynamicStars;
import com.idflood.sky.items.DynamicSun;
import com.idflood.sky.utils.CloudsBillboardItem;
import com.idflood.sky.utils.HorizonBillboardItem;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.GammaCorrectionFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.*;

import static ru.arifolth.anjrpg.interfaces.Constants.INITIAL_MOUNTAINS_OFFSET;

public class DynamicSky extends Node implements SkyInterface {
    private final CloudsBillboardItem clouds;
    private final HorizonBillboardItem horizon;
    private float fadeOut;
    private AmbientLight ambientLight;
    private DynamicSun dynamicSun = null;
    private DynamicStars dynamicStars = null;
    private DynamicSkyBackground dynamicBackground = null;

    private GameLogicCoreInterface gameLogicCore = null;

    private float scaling = 10000;

    public DynamicSky(AssetManager assetManager, ViewPort viewPort, GameLogicCoreInterface gameLogicCore) {
        super("Sky");

        this.gameLogicCore = gameLogicCore;

        Node rootNode = gameLogicCore.getRootNode();

        ambientLight = new AmbientLight();
        ambientLight.setColor(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
        rootNode.addLight(ambientLight);

        dynamicSun = new DynamicSun(assetManager, viewPort, rootNode, scaling);

        dynamicStars = new DynamicStars(assetManager, viewPort, scaling);
        dynamicStars.setShadowMode(ShadowMode.Off);

        dynamicBackground = new DynamicSkyBackground(assetManager, viewPort, rootNode);

        rootNode.setShadowMode(ShadowMode.Off);

        gameLogicCore.getApp().enqueue(() -> {
            rootNode.attachChild(this);
        });

        var lambdaContext = new Object() {
            Node mountainNode = new Node();
        };
        horizon = new HorizonBillboardItem(assetManager, "Mountain", 1f);
        lambdaContext.mountainNode.attachChild(horizon);
        LodUtils.setUpModelLod(lambdaContext.mountainNode);
        lambdaContext.mountainNode = GeometryBatchFactory.optimize(lambdaContext.mountainNode, true);
        MikktspaceTangentGenerator.generate(lambdaContext.mountainNode);
        lambdaContext.mountainNode.updateModelBound();
        lambdaContext.mountainNode.setLocalTranslation(INITIAL_MOUNTAINS_OFFSET);
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(lambdaContext.mountainNode);
        });

        clouds = new CloudsBillboardItem(assetManager, "Clouds", 1f);
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(clouds);
        });
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

    public void fadeLight(float tpf) {
        GammaCorrectionFilter gammaCorrectionFilter = ((ANJRpgInterface) gameLogicCore.getApp()).getFilterManager().getGammaCorrectionFilter();
        if(tpf < 0) {
            if(fadeOut <= -0.5f) {
                if(!gammaCorrectionFilter.isEnabled()) {
                    gammaCorrectionFilter.setGamma(0.5f);
                    gammaCorrectionFilter.setEnabled(true);
                }
            }
            if (fadeOut <= -1.0f) {
                ambientLight.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
                return;
            }
        } else {
            if(fadeOut >= -0.15) {
                if(gammaCorrectionFilter.isEnabled()) {
                    gammaCorrectionFilter.setGamma(1.125f);
                    gammaCorrectionFilter.setEnabled(false);
                }
            }
            if (fadeOut >= 0.5f) {
                ambientLight.setColor(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
                return;
            }

        }
        fadeOut += tpf / 16;
        getSunLight().setColor(ColorRGBA.White.mult(fadeOut));
    }

    public void updateTime(float tpf){
        dynamicSun.updateTime();

        int hours = getHours();
        if (Utils.isBetween(hours, 0, 6) || Utils.isBetween(hours, 19, 23)) {
            if(!dynamicStars.isAttached()) {
                attachStars();
            }
            fadeLight(-tpf);
        } else {
            if(dynamicStars.isAttached()) {
                detachStars();
            }
            fadeLight(tpf);
        }

        dynamicBackground.updateLightPosition(dynamicSun.getSunSystem().getPosition());
        dynamicStars.update(dynamicSun.getSunSystem().getDirection());
        dynamicStars.lookAt(dynamicSun.getSunSystem().getPosition(), Vector3f.ZERO);

        Vector3f playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation().clone();
        playerLocation.x += Constants.HEIGHT_OFFSET;
        playerLocation.y += Constants.HEIGHT_OFFSET;
        clouds.setLocalTranslation(playerLocation);

        playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation().clone();
        playerLocation.x += Constants.MOUNTAINS_HEIGHT_OFFSET;
        playerLocation.y += Constants.HEIGHT_OFFSET;
        horizon.setLocalTranslation(playerLocation);

        playerLocation = gameLogicCore.getPlayerCharacter().getNode().getLocalTranslation().clone();
        playerLocation.x += Constants.MOUNTAINS_HEIGHT_OFFSET;
        playerLocation.y += Constants.HEIGHT_OFFSET;
        dynamicStars.setLocalTranslation(playerLocation);
    }

    @Override
    public void update(float tpf){
        updateTime(tpf);
    }

    @Override
    public DirectionalLight getSunLight(){
        return dynamicSun.getSunLight();
    }
}
