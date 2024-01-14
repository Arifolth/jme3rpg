/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import ru.arifolth.anjrpg.interfaces.weather.EmitterInterface;

public class RainEmitter implements EmitterInterface {
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

    public void update(float tpf) {
        rain.setLocalTranslation(spatial.getLocalTranslation());
        rain.emitAllParticles();
    }
}
