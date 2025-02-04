package com.idflood.sky;

import com.idflood.sky.items.DynamicSkyBackground;
import com.idflood.sky.items.DynamicStars;
import com.idflood.sky.items.DynamicSun;
import com.idflood.sky.utils.CloudsBillboardItem;
import com.idflood.sky.utils.HorizonBillboardItem;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.GammaCorrectionFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.processors.FadeGammaProcessor;
import ru.arifolth.anjrpg.processors.FadeLightProcessor;
import ru.arifolth.anjrpg.interfaces.*;

import static ru.arifolth.anjrpg.interfaces.Constants.INITIAL_MOUNTAINS_OFFSET;

public class DynamicSky extends Node implements SkyInterface {
    private CloudsBillboardItem clouds;
    private HorizonBillboardItem horizon;
    private float fadeOut;
    private AmbientLight ambientLight;
    private DynamicSun dynamicSun = null;
    private DynamicStars dynamicStars = null;
    private DynamicSkyBackground dynamicBackground = null;

    private GameLogicCoreInterface gameLogicCore = null;

    private FadeLightProcessor fadeLightProcessor = null;

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

        gameLogicCore.setSky(this);
    }

    @Override
    public void initialize() {
        var lambdaContext = new Object() {
            Node mountainNode = new Node();
        };
        horizon = new HorizonBillboardItem(gameLogicCore.getAssetManager(), "Mountain", 1f);
        lambdaContext.mountainNode.attachChild(horizon);
        LodUtils.setUpModelLod(lambdaContext.mountainNode);
        lambdaContext.mountainNode = GeometryBatchFactory.optimize(lambdaContext.mountainNode, true);
        MikktspaceTangentGenerator.generate(lambdaContext.mountainNode);
        lambdaContext.mountainNode.updateModelBound();
        lambdaContext.mountainNode.setLocalTranslation(INITIAL_MOUNTAINS_OFFSET);
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(lambdaContext.mountainNode);
        });

        clouds = new CloudsBillboardItem(gameLogicCore.getAssetManager(), "Clouds", 1f);
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(clouds);
        });
        setQueueBucket(RenderQueue.Bucket.Sky);
        setCullHint(CullHint.Never);

        GammaCorrectionFilter gammaCorrectionFilter = ((ANJRpgInterface) gameLogicCore.getApp()).getFilterManager().getGammaCorrectionFilter();
        fadeLightProcessor = new FadeLightProcessor(ambientLight, dynamicSun.getSunLight(), new FadeGammaProcessor(gammaCorrectionFilter));
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

    public void fade(float tpf) {
        fadeLightProcessor.process(tpf);
    }

    public void updateTime(float tpf){
        dynamicSun.updateTime();

        int hours = getHours();
        if (Utils.isBetween(hours, 0, 6) || Utils.isBetween(hours, 19, 23)) {
            if(!dynamicStars.isAttached()) {
                attachStars();
            }
            this.fade(-tpf);
        } else {
            if(dynamicStars.isAttached()) {
                detachStars();
            }
            this.fade(tpf);
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
}
