package ru.arifolth.anjrpg.grass;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;

import java.util.List;

/**
 *
 * @author Brian Babcock
 */
public class LayeredMaterial extends Material {

   protected int numLayers;             //Number of layers.  More layers are more realistic but slower
   protected int maxLayers;
   protected Texture layerTex;          //Texture that colors the layers
   protected List<Texture> layerMasks;  //List of masks to use for the layers
   protected Material baseMaterial;     //Material to use for the first layer
   protected int grassDistance;

   /**
    * For custom layer materials.  Only use with .j3md materials meant for layering!
    * 
    * @param am AssetManager
    * @param asset Path to the custom material definition
    * @param numLayers Number of layers
    * @param height Height to the top layer
    */
   public LayeredMaterial(AssetManager am, String asset, int numLayers) {
      super(am, asset);
      this.numLayers = numLayers;
      this.maxLayers = numLayers;
      grassDistance = 20;
      setFloat("GrassDistance", 20.0f);
   }

   /**
    * Standard constructor using the standard layered j3md material definition.
    * 
    * @param am AssetManager
    * @param numLayers Number of layers
    * @param height Height to the top layer
    */
   public LayeredMaterial(AssetManager am, int numLayers) {
      this(am, "MatDefs/Layered.j3md", numLayers);
   }

   public void setBaseMaterial(Material baseMaterial) {
      this.baseMaterial = baseMaterial;
   }

   public void setNumLayers(int numLayers) {
      this.numLayers = numLayers;
   }

   public int getNumLayers() {
      return numLayers;
   }

   public void setLayerMasks(List<Texture> layerMasks) {
      this.layerMasks = layerMasks;
   }

   public List<Texture> getLayerMasks() {
      return layerMasks;
   }

   public void addLayerMask(Texture layerMask) {
      layerMasks.add(layerMask);
   }

   public void setGrassDistance(int dist) {
      grassDistance = dist;
      setFloat("GrassDistance", dist);
   }

   public float getGrassDistance() {
      return grassDistance;
   }
   
   public void updateTime(float totalTime) {
      setFloat("Time", totalTime);
   }

   /**
    * Override the standard render to render all the layers
    */
   @Override
   public void render(Geometry geom, RenderManager rm) {

      //First, render the base material
      if (baseMaterial != null) {
         baseMaterial.render(geom, rm);
      }
      float distFromCamera = geom.getWorldBound().distanceToEdge(rm.getCurrentCamera().getLocation());
      if (distFromCamera < grassDistance) {

         //Then render the layers
         //Ignore it if it's still sitting in availableGeos, it hasn't been assigned to a patch this time around
         if (getActiveTechnique() != null) {
            //**Render the grass layers**

            //Make sure Layer is set to zero
            //g.getMaterial().setFloat("Layer", 0);

            //Depending on how many different layer maps were generated,
            //we move on to the next layer map after rendering the right
            //number of layers
            int curDiffLayer = 0;
            int layerChangeInterval = (int) FastMath.ceil(numLayers / (float) layerMasks.size());

            int numRenderedLayers = numLayers;

            //System.out.println("Drawing grass mesh centered at " + geom.getWorldTranslation()); //DEBUG
            //Main layer render loop
            for (int i = 1; i <= numRenderedLayers; i++) {
               float layer = i / (float) numRenderedLayers;

               //Move to the next layer map if necessary
               if (i % layerChangeInterval == 0) {
                  if (!layerMasks.isEmpty()) {
                     setTexture("GrassMask", layerMasks.get(curDiffLayer));
                     curDiffLayer++;
                  }
               }

               //Set the layer
               setFloat("Layer", layer);

               //And finally, pass to the superclass to render the layer
               super.render(geom, rm);
            }
            setFloat("Layer", 0);
         } else {
            super.render(geom, rm);
         }
      }
   }

   /**
    * Adjusts the number of layers, if possible.  Fewer layers means less detail.
    * 
    * @param less if true, lowers the number, otherwise raises the number.
    */
   public void adjustLayers(boolean less) {
      if (less && numLayers > 10) {
         numLayers--;
      } else if (!less && numLayers < maxLayers) {
         numLayers++;
      }
   }
}
