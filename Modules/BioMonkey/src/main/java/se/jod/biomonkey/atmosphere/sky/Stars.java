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
package se.jod.biomonkey.atmosphere.sky;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.biomes.generation.TerrainGenerator;
import se.jod.biomonkey.random.FastRandom;
import se.jod.biomonkey.terrain.datagrids.TerrainMapGrid;

/**
 * A point mesh used to display stars. Needs some attention.
 * 
 * @author Andreas
 */
public class Stars {

    protected static final Vector3f AxisVec = new Vector3f(FastMath.cos(1.169f), FastMath.sin(1.169f), 0).normalizeLocal();
    protected static final Quaternion RotationQuat = new Quaternion();
    
    // Different star colors.
    protected static final ColorRGBA[] StarColors = {
        new ColorRGBA(156 / 255f, 175 / 255f, 252 / 255f, 1f), // O
        new ColorRGBA(168 / 255f, 188 / 255f, 249 / 255f, 1f), // B
        new ColorRGBA(201 / 255f, 213 / 255f, 253 / 255f, 1f), // A
        ColorRGBA.White.clone(), // F
        new ColorRGBA(248 / 255f, 238 / 255f, 229 / 255f, 1f), // G
        new ColorRGBA(251 / 255f, 207 / 255f, 160 / 255f, 1f), // K
        new ColorRGBA(251 / 255f, 202 / 255f, 110 / 255f, 1f), // M
    };
    
    protected FastRandom rand;
    protected int amount;
    protected float radius;
    protected Geometry starGeom;
    protected Material starMat;
    protected boolean active;
    protected float time;

    public Stars(int amount, float radius) {
        this.amount = amount;
        this.radius = radius*1.05f;
    }
    
    /**
     * Load the star geometry.
     * @param overwrite If there already is a star mesh available, overwrite it.
     */
    public void load(boolean overwrite) {
        
        String fileName = EcoManager.getInstance().getTextureFolder() + "Sky/StarMesh.j3o";
        
        File data = new File(fileName);
        if (!data.exists() || overwrite) {
            generate();
            return;
        }
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(data);
            BinaryImporter imp = new BinaryImporter();
            AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
            imp.setAssetManager(assetManager);
            starGeom = (Geometry) imp.load(new BufferedInputStream(fis), null, null);
            
        } catch (IOException ex) {
            Logger.getLogger(TerrainMapGrid.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TerrainMapGrid.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void save() {
        String textureFolder = EcoManager.getInstance().getTextureFolder();
        FileOutputStream fos = null;
        try {
            File dir = new File(textureFolder + "Sky");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File object = new File(textureFolder + "Sky/StarMesh.j3o");
            
            fos = new FileOutputStream(object);

            BinaryExporter.getInstance().save(starGeom, new BufferedOutputStream(fos));

            fos.flush();
        } catch (IOException ex) {
            Logger.getLogger(TerrainGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Logger.getLogger(TerrainGenerator.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        starGeom.setMaterial(starMat);
    }
    
    protected void generate() {
        rand = new FastRandom();
        
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);
        FloatBuffer pos = BufferUtils.createFloatBuffer(amount * 3);
        FloatBuffer size = BufferUtils.createFloatBuffer(amount);
        FloatBuffer col = BufferUtils.createFloatBuffer(amount * 4);
        mesh.setBuffer(Type.Position, 3, pos);
        mesh.setBuffer(Type.Size, 1, size);
        mesh.setBuffer(Type.Color, 4, col);

        for (int i = 0; i < amount; i++) {
            float phi = rand.unitRandom() * FastMath.TWO_PI;
            // Hax to get a more even distribution.
            float r = rand.unitRandom();
            float theta = -FastMath.HALF_PI + r * FastMath.PI;

            float cosPhi = FastMath.cos(phi);
            float sinPhi = FastMath.sin(phi);
            float cosTheta = FastMath.cos(theta);
            float sinTheta = FastMath.sin(theta);

            Vector3f loc = new Vector3f(cosPhi * cosTheta, sinTheta, sinPhi * cosTheta).multLocal(radius);
            float sizeSeed = rand.unitRandom();
            float starSize = 2f + sizeSeed * sizeSeed * 3f;
            ColorRGBA color = StarColors[rand.nextInt(6)];

            pos.put(loc.x).put(loc.y).put(loc.z);
            size.put(starSize);
            col.put(color.r).put(color.g).put(color.b).put(color.a);
        }

        mesh.setStatic();
        mesh.setBound(new BoundingBox());
        mesh.updateBound();

        starGeom = new Geometry("Stars", mesh);
        
        AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
        starMat = new Material(assetManager, "se/jod/biomonkey/assets/matdefs/StarBase.j3md");
        starMat.getAdditionalRenderState().setAlphaTest(true);
        starMat.getAdditionalRenderState().setAlphaFallOff(0.01f);

        Texture starTex = assetManager.loadTexture("se/jod/biomonkey/assets/textures/StarSprite.png");
        starMat.setTexture("Texture", starTex);
        
        starGeom.setMaterial(starMat);
        starGeom.setQueueBucket(Bucket.Sky);
        
        save();
    }

    public Geometry getStarGeometry() {
        return starGeom;
    }

    public void update(float tpf) {
        time += tpf;
        if (time > 50000f) {
            time = 0;
        }
        RotationQuat.fromAngleNormalAxis(time * 0.01f, AxisVec);
        starGeom.setLocalRotation(RotationQuat);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
}
