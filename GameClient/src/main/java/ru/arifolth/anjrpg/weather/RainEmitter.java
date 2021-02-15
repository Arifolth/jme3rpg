package ru.arifolth.anjrpg.weather;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 28.12.12
 * Time: 2:14
 * To change this template use File | Settings | File Templates.
 */
public class RainEmitter implements Emitter {
    ParticleEmitter rain;
    private Spatial spatial;

    public RainEmitter(Node rootNode, AssetManager assetManager) {
        rain = new ParticleEmitter("Rain", ParticleMesh.Type.Point, 90000); //amount of rain flakes
        rain.setShape(new EmitterSphereShape(new Vector3f(-1.8f, -1.8f, -1.8f), 50f));

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setBoolean("PointSprite", true);
        mat.setTexture("Texture", assetManager.loadTexture("Textures/weather/raindrop.png"));
        rain.setMaterial(mat);

        rain.setQueueBucket(RenderQueue.Bucket.Transparent);
        rain.setImagesX(1); //flakes quality
        rain.setImagesY(1); // 3x3 texture animation
        rain.setStartSize(0.1f);
        rain.setEndSize(0.1f);
        rain.setStartColor(ColorRGBA.Gray);
        rain.setEndColor(ColorRGBA.Gray);
        rain.setSelectRandomImage(true);
        rain.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -1.65f, 0));
        rain.setGravity(0, 2, 0);
        rain.getParticleInfluencer().setVelocityVariation(0.45f);

        rootNode.attachChild(rain);
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    public void simpleUpdate(float tpf) {
        rain.setLocalTranslation(spatial.getLocalTranslation());
        rain.emitAllParticles();
    }
}
