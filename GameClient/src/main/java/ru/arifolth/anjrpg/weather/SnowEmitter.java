package ru.arifolth.anjrpg.weather;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 27.12.12
 * Time: 1:31
 * To change this template use File | Settings | File Templates.
 */
public class SnowEmitter implements Emitter {
    ParticleEmitter snow;
    private Spatial spatial;

    public SnowEmitter(Node rootNode, AssetManager assetManager) {
        snow = new ParticleEmitter("Snow", ParticleMesh.Type.Point, 35000); //amount of rain flakes
        snow.setShape(new EmitterSphereShape(new Vector3f(-1.8f, -1.8f, -1.8f), 50f));

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setBoolean("PointSprite", true);
        mat.setTexture("Texture", assetManager.loadTexture("Textures/weather/snowflake.jpg"));
        snow.setMaterial(mat);

        snow.setImagesX(1); //flakes quality
        snow.setImagesY(1); // 3x3 texture animation
        snow.setStartSize(0.04f);
        snow.setEndSize(0.008f);
        snow.setStartColor(ColorRGBA.White);
        snow.setEndColor(ColorRGBA.White);
        snow.setSelectRandomImage(true);
        snow.setRotateSpeed(4);
        snow.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -0.65f, 0));
        snow.setGravity(0, 0, 0);
        snow.getParticleInfluencer().setVelocityVariation(0.45f);

        rootNode.attachChild(snow);
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    public void simpleUpdate(float tpf) {
        snow.setLocalTranslation(spatial.getLocalTranslation());
        snow.emitAllParticles();
    }

}
