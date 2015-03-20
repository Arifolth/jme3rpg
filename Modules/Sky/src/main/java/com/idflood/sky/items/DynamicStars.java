package com.idflood.sky.items;

import com.idflood.sky.utils.SkyBillboardItem;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;

public class DynamicStars extends Node{
    private ViewPort viewPort = null;
    private AssetManager assetManager = null;
    
    private SkyBillboardItem[] stars;
    
    private int stars_count = 500;
    private Material mat;
    
    public DynamicStars(AssetManager assetManager, ViewPort viewPort, Float scaling){
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/star.png"));
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.setColor("Color", new ColorRGBA(1f,1f,1f, 0.4f));
        //mat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        stars = new SkyBillboardItem[stars_count];
        for(int i = 0; i < stars_count; i++){
            SkyBillboardItem item = new SkyBillboardItem("star_" + i, 80f + ((float) Math.random() * 2250f));  //star size
            stars[i] = item;
            
            item.setMaterial(mat);
            item.setLocalTranslation(getPointOnSphere().mult(15000));  //distance to stars
            item.removeBillboardController();
            item.lookAt(getRandomVector().mult(10), Vector3f.UNIT_Y);
            item.rotate(new Quaternion().fromAngles((float) Math.random() - 0.5f, (float) Math.random() - 0.5f, (float) Math.random() - 0.5f));
            attachChild(item);
        }
        
        setQueueBucket(Bucket.Sky);
        setCullHint(CullHint.Never);
    }
    
    public void update(Vector3f sunDir){
        mat.setColor("Color", new ColorRGBA(1f,1f,1f, (sunDir.y + 0.2f) * 0.5f));
    }
    
    protected Vector3f getRandomVector(){
        return new Vector3f(
                (float) Math.random() - 0.5f,
                (float) Math.random() - 0.5f,
                (float) Math.random() - 0.5f
        );
    }
    
    protected Vector3f getPointOnSphere(){
        Float x = (float) Math.random() - 0.5f;
        Float y = (float) Math.random() - 0.5f;
        Float z = (float) Math.random() - 0.5f;
        Float k = FastMath.sqrt(x * x + y * y + z * z);
        while(k < 0.2 || k > 0.3){
            x = (float) Math.random() - 0.5f;
            y = (float) Math.random() - 0.5f;
            z = (float) Math.random() - 0.5f;
            k = FastMath.sqrt(x * x + y * y + z * z);
        }
        
        return new Vector3f(
                x / k,
                y / k,
                z / k
        );
    }
}
