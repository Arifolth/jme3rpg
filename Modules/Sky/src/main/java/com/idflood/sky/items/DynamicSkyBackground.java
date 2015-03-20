package com.idflood.sky.items;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;

public class DynamicSkyBackground {
    private static final Sphere sphereMesh = new Sphere(40, 40, 10000, false, true);
    
    private ViewPort viewPort = null;
    private AssetManager assetManager = null;
    private Vector3f lightPosition = new Vector3f();
    
    private Geometry skyGeom = null;
    private Material skyMaterial = null;
    
    public DynamicSkyBackground(AssetManager assetManager, ViewPort viewPort, Node rootNode){
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        
        skyGeom = getSkyGeometry();
        rootNode.attachChild(skyGeom);
    }
    
    protected Geometry getSkyGeometry(){
        Geometry geom = new Geometry("Sky", sphereMesh);
        geom.setQueueBucket(Bucket.Sky);
        geom.setCullHint(Spatial.CullHint.Never);
        
        skyMaterial = getDynamicSkyMaterial();
        geom.setMaterial(skyMaterial);
        return geom;
    }
    
    protected Material getDynamicSkyMaterial(){
        Material skyMat = new Material(assetManager, "MatDefs/dynamic_sky.j3md");
        skyMat.setTexture("glow_texture", assetManager.loadTexture("Textures/glow.png"));
        skyMat.setTexture("color_texture", assetManager.loadTexture("Textures/sky.png"));
        skyMat.setVector3("lightPosition", lightPosition);
        
        return skyMat;
    }
    
    public void updateLightPosition(Vector3f position){
        lightPosition = position;
        skyMaterial.setVector3("lightPosition", lightPosition);
        
        // make the sky follow the camera
        skyGeom.setLocalTranslation(viewPort.getCamera().getLocation());
    }
}
