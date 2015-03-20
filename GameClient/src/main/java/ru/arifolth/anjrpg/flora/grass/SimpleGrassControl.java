/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.arifolth.anjrpg.flora.grass;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 17.04.13
 * Time: 0:17
 * To change this template use File | Settings | File Templates.
 */

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import jme3tools.optimize.GeometryBatchFactory;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a grass control that can be added to any terrain or any spatial/node that has
 * a terrain object attached to it.
 *
 * This is a simple implementation of a grass control and does not use paging
 * or anything too fancy <img src='http://jmonkeyengine.org/wp-includes/images/smilies/chimpanzee-tongue.gif' alt=':p' class='wp-smiley' />
 *
 * Grass is generated only where there is the first texture layer displayed on the terrain
 * The first layer is the grass layer (only been tested for up to 3 layers)
 *
 * Values and materials should be modified to suite your needs
 *
 */
public class SimpleGrassControl extends AbstractControl {

    TerrainQuad terrain;
    AssetManager assetManager;
    Material faceMat;
    Quad faceShape;
    Node grassLayer = new Node();
    float scale;

    /*
     * Should be greater than 1
     * GrassPatches will be scaled randomly by a factor between 1/patchScaleVariation and patchScaleVariation
     */
    float patchScaleVariation = 2f;
    /*
     * The width of the grass Quads
     */
    float patchWidth = 20;
    /*
     * The height of the Grassquads
     */
    float patchHeight = 15;
    /*
     * Increment for the uniform grass planting algorithm
     * The lower this value the more dense the grass
     * Making this a very low value may cause memory issues
     */
    float inc = 14;
    String texturePath;

    public SimpleGrassControl(AssetManager assetManager, String texturePath)
    {
        this.assetManager = assetManager;
        this.texturePath = texturePath;

        faceMat = new Material(assetManager,"MatDefs/grassBase.j3md");
        faceMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Off);
        faceMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        faceMat.setTransparent(true);
        faceMat.setTextureParam("ColorMap",VarType.Texture2D,assetManager.loadTexture(texturePath));
        faceMat.setBoolean("VertexLighting",false);
        faceMat.setInt("NumLights", 4);
        faceMat.setBoolean("VertexColors", false);
        faceMat.setBoolean("FadeEnabled", false);
        faceMat.setFloat("FadeEnd", 2000);
        faceMat.setFloat("FadeRange", 0);
        faceMat.setBoolean("FadeEnabled", true);
        faceMat.setBoolean("SelfShadowing", false);
        faceMat.setBoolean("Swaying",true);
        faceMat.setVector3("SwayData",new Vector3f(1.5f,1,5));
        faceMat.setVector2("Wind", new Vector2f(1,1));
    }

    public Control cloneForSpatial(Spatial spatial) {
        final SimpleGrassControl control = new SimpleGrassControl(assetManager, texturePath);

        control.setSpatial(spatial);
        return control;
    }

    @Override
    public void setSpatial(Spatial spatial)
    {
        super.setSpatial(spatial);
        Node spatNode = (Node)spatial;

        if(spatial instanceof Terrain)
        {
            terrain = (TerrainQuad)spatial;
        }
        else
        {
            for(Spatial currentSpatial : spatNode.getChildren())
            {
                if(currentSpatial instanceof Terrain)
                {
                    terrain=(TerrainQuad)currentSpatial;
                    break;
                }
            }
        }

        if(terrain==null||spatNode.getChildren().isEmpty())
        {
            Logger.getLogger(SimpleGrassControl.class.getName()).log(Level.SEVERE, "Could not find terrain object.", new Exception());
            System.exit(0);
        }

        scale = ((Spatial)terrain).getWorldScale().x;

        //Generate grass uniformly with random offset.
        float terrainWidth = scale*terrain.getTerrainSize(); // get width length of terrain(assuming its a square)
        Vector3f centre = (((Spatial)terrain).getWorldBound().getCenter()); // get the centr location of the terrain
        Vector2f grassPatchRandomOffset = new Vector2f().zero();
        Vector3f candidateGrassPatchLocation = new Vector3f();

        for(float x = centre.x - terrainWidth/2 + inc; x < centre.x + terrainWidth/2 - inc; x+=inc)
        {
            for(float z = centre.z - terrainWidth/2 + inc; z < centre.z + terrainWidth/2 - inc; z+=inc)
            {
                grassPatchRandomOffset.set(0, inc);
                grassPatchRandomOffset.multLocal(new Random().nextFloat()); // make the off set length a random distance smaller than the increment size
                grassPatchRandomOffset.rotateAroundOrigin((float)(((int)(Math.random()*359))*(Math.PI/180)), true); // rotate the offset by a random angle
                candidateGrassPatchLocation.set(x+grassPatchRandomOffset.x, terrain.getHeight(new Vector2f(x+grassPatchRandomOffset.x,z+grassPatchRandomOffset.y)), z+grassPatchRandomOffset.y);

                if(isGrassLayer(candidateGrassPatchLocation))
                {
                    createGrassPatch(candidateGrassPatchLocation);
                }

            }
        }

        grassLayer.scale(1/scale);
        GeometryBatchFactory.optimize(grassLayer);
        terrain.attachChild(grassLayer);

    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    private void createGrassPatch(Vector3f location)
    {

        Node grassPatch = new Node();
        float selectedSizeVariation = (float)(new Random().nextFloat()*(patchScaleVariation-(1/patchScaleVariation)))+(1/patchScaleVariation);
        faceShape = new Quad((patchWidth*selectedSizeVariation),patchHeight*selectedSizeVariation,false);
        Geometry face1 = new Geometry("face1",faceShape);
        face1.move(-(patchWidth*selectedSizeVariation)/2, 0, 0);
        grassPatch.attachChild(face1);

        Geometry face2 = new Geometry("face2",faceShape);
        face2.rotate(new Quaternion().fromAngleAxis(-FastMath.PI/2,   new Vector3f(0,1,0)));
        face2.move(0, 0, -(patchWidth*selectedSizeVariation)/2);
        grassPatch.attachChild(face2);

        grassPatch.setCullHint(Spatial.CullHint.Dynamic);
        grassPatch.setQueueBucket(RenderQueue.Bucket.Transparent);

        face1.setMaterial(faceMat);
        face2.setMaterial(faceMat);

        grassPatch.rotate(new Quaternion().fromAngleAxis( (((int)(Math.random()*359))+1) *(FastMath.PI/190),   new Vector3f(0,1,0)));
        grassPatch.setLocalTranslation(location);

        grassLayer.attachChild(grassPatch);
    }

    private boolean isGrassLayer(Vector3f pos)
    {
        MatParam matParam = terrain.getMaterial(null).getParam("AlphaMap_1");
        Texture tex = (Texture) matParam.getValue();
        Image image = tex.getImage();
        Vector2f uv = getPointPercentagePosition(terrain, pos);

        ByteBuffer buf = image.getData(0);
        int width = image.getWidth();
        int height = image.getHeight();

        int x = (int)(uv.x*width);
        int y = (int)(uv.y*height);

        int position = (y*width + x) * 4;
        ColorRGBA color = new ColorRGBA().set(ColorRGBA.Black);

        buf.position( position );
        color.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));

        if(color.r==1 && color.b==0 && color.g==0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private Vector2f getPointPercentagePosition(Terrain terrain, Vector3f worldLoc) {
        Vector2f uv = new Vector2f(worldLoc.x,-worldLoc.z);
        uv.subtractLocal(((Node)terrain).getWorldTranslation().x*scale, ((Node)terrain).getWorldTranslation().z*scale); // center it on 0,0
        float scaledSize = terrain.getTerrainSize()*scale;
        uv.addLocal(scaledSize/2, scaledSize/2); // shift the bottom left corner up to 0,0
        uv.divideLocal(scaledSize); // get the location as a percentage

        return uv;
    }

    private float byte2float(byte b){
        return ((float)(b & 0xFF)) / 255f;
    }

}
