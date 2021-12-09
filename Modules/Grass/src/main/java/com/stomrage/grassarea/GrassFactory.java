/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ImageRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The GrassFactory is a Singleton and is in charge of the whole GrassBlade
 * creation process
 *
 * @author Stomrage
 * @version 0.1
 */
public class GrassFactory {

    private static GrassFactory instance = null;
    //The TerrainQuad is used to get the height of the terrain
    private TerrainQuad terrain;
    //The array of image raster for each density map
    private ArrayList<ImageRaster> densityTextures = new ArrayList<ImageRaster>();
    //This array is here to help the save of the density map (contains the path to the density texture)
    private ArrayList<String> densityTexturesPath = new ArrayList<String>();
    private AssetManager assetManager;
    private int terrainSize = 0;
    //The number of density map
    private int layerNumberMax = 0;
    private float grassDist = 0;
    //This hash map contains all the layer of the GrassArea. Every DensityMap+ColorChannel is a HashKey value in this HashMap
    private HashMap<HashKey, Layer> layerHash = new HashMap<HashKey, Layer>();
    //The perlin noise
    private PerlinNoise perlinNoise;
    private Texture dissolveTexture;
    private Texture colorTexture;

    /**
     * Get access to the singleton
     *
     * @return The instance
     */
    public static GrassFactory getInstance() {
        if (instance == null) {
            instance = new GrassFactory();
        }
        return instance;
    }

    /**
     * The GrassFactory constructor is empty
     */
    public GrassFactory() {
    }

    /**
     * Create the perlin noise for the terrain
     *
     * @param size The size of the terrain
     * @param interpolate The interpolation value for the perlin noise algorithm
     */
    public void createPerlinNoise(int size, int interpolate) {
        this.perlinNoise = new PerlinNoise(size, interpolate);
    }

    /**
     * The asset manager need to be set before creating the GrassArea material
     *
     * @param assetManager The asset manager
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Set the terrain for the GrassFactory
     *
     * @param terrain The terrain
     */
    public void setTerrain(TerrainQuad terrain) {
        this.terrain = terrain;
        this.terrainSize = terrain.getTerrainSize();
    }

    /**
     * Set the grass draw distance
     *
     * @param grassDist The grass distance
     */
    public void setGrassDistance(float grassDist) {
        this.grassDist = grassDist;
    }

    /**
     * Set the color texture atlas for the GrassFactory
     * @param color The color map texture atlas
     */
    public void setColorTexture(Texture color) {
        this.colorTexture = color;
    }

    /**
     * Set the dissolve texture for the GrassFactory
     * @param dissolve The dissolve texture
     */
    public void setDissolveTexture(Texture dissolve) {
        this.dissolveTexture = dissolve;
    }

    /**
     * Exatcly the same as
     * @see GrassArea But this time the ColorChannel and DensityMap integer
     * value is used and we check for Exception
     * @param minX The start index of the texture tile to reach
     * @param texSize The size of the texture to reach
     * @param density The density of the layer, this number will be multiplied
     * by the density map value to give the number of grass to add a this point
     * @param index Which ColorChannel to use for this layer
     * @param densityMap Which DensityMap to use for this layer
     * @param minSize The minimum size of the grass for this layer
     * @param maxSize The maximum size of the grass for this layer
     * @throws Exception
     */
    public void addLayer(float minX, float texSize, float density, int index, int densityMap, float minSize, float maxSize) throws Exception {
        if (densityMap >= layerNumberMax) {
            throw new Exception("The number of possible densityMap : " + layerNumberMax + " add more density map");
        }
        if (layerHash.get(new HashKey(densityMap, index)) != null) {
            throw new Exception("A key for density map : " + densityMap + " and color index : " + index + " already exist !");
        }
        layerHash.put(new HashKey(densityMap, index), new Layer(density, minSize, maxSize, minX, texSize, index));
    }

    /**
     * In this method we create an ImageRaster and add it to the DensityTextures
     * array list as well as saving the textures path
     *
     * @param densityTexture The density map
     */
    public void addDensityMap(Texture densityTexture) {
        this.densityTexturesPath.add(densityTexture.getName());
        this.densityTextures.add(ImageRaster.create(densityTexture.getImage()));
        this.layerNumberMax++;
    }

    /**
     * This method create a grass a list of grass a the given location. This
     * method is used inside the GrassHolder during the generation process.
     * During the process we'll take a look at the density map value and the
     * density of the layer and create a list of grass blade randomly generate
     * for this position.
     *
     * @param minX The minimum X size of this GrassHolder (to avoid border
     * effect)
     * @param minZ The minimum Z size of this GrassHolder (to avoid border
     * effect)
     * @param maxX The maximum X size of this GrassHolder (to avoid border
     * effect)
     * @param maxZ The maximum Z size of this GrassHolder (to avoid border
     * effect)
     * @param x The x position of the grass
     * @param z The z position of the grass
     * @return A list of GrassBlade at this given location
     */
    List<GrassBlade> createGrass(int minX, int minZ, int maxX, int maxZ, int x, int z) {
        List<GrassBlade> grassBlades = new ArrayList<GrassBlade>();
        for (int i = 0; i < layerNumberMax; i++) {
            for (int j = 0; j < 4; j++) {
                HashKey k = new HashKey(i, j);
                if (layerHash.get(k) != null) {
                    switch (j) {
                        case 0: {
                            float textureDensity = densityTextures.get(i).getPixel(x + terrainSize / 2, z + terrainSize / 2).r;
                            float layerDensity = layerHash.get(k).density;
                            grassBlades.addAll(createBlades(textureDensity, layerDensity, minX, maxX, minZ, maxZ, x, z, k));
                            break;
                        }
                        case 1: {
                            float textureDensity = densityTextures.get(i).getPixel(x + terrainSize / 2, z + terrainSize / 2).g;
                            float layerDensity = layerHash.get(k).density;
                            grassBlades.addAll(createBlades(textureDensity, layerDensity, minX, maxX, minZ, maxZ, x, z, k));
                            break;
                        }
                        case 2: {
                            float textureDensity = densityTextures.get(i).getPixel(x + terrainSize / 2, z + terrainSize / 2).b;
                            float layerDensity = layerHash.get(k).density;
                            grassBlades.addAll(createBlades(textureDensity, layerDensity, minX, maxX, minZ, maxZ, x, z, k));
                            break;
                        }
                        case 3: {
                            float textureDensity = densityTextures.get(i).getPixel(x + terrainSize / 2, z + terrainSize / 2).a;
                            float layerDensity = layerHash.get(k).density;
                            grassBlades.addAll(createBlades(textureDensity, layerDensity, minX, maxX, minZ, maxZ, x, z, k));
                            break;
                        }
                    }
                }
            }
        }
        return grassBlades;
    }

    /**
     * In this method we'll create a certain amount of grass based of the
     * Layer.density and DensityMap.value during the loop a grass is generated
     * randomly is the density map value is > to a randomly generate float
     *
     * @param textureDensity The texture density at this point
     * @param layerDensity The layer global density
     * @param minX
     * @see createGrass
     * @param maxX
     * @see createGrass
     * @param minZ
     * @see createGrass
     * @param maxZ
     * @see createGrass
     * @param x
     * @see createGrass
     * @param z
     * @see createGrass
     * @param k The key for the layerindex+densitymap
     * @return A list of grass
     */
    List<GrassBlade> createBlades(float textureDensity, float layerDensity, int minX, int maxX, int minZ, int maxZ, int x, int z, HashKey k) {
        List<GrassBlade> grassBlades = new ArrayList<GrassBlade>();
        for (int i = 0; i < layerDensity; i++) {
            if (textureDensity > FastMath.nextRandomFloat()) {
                float finalX = FastMath.clamp(FastMath.nextRandomFloat() + x, minX, maxX);
                float finalZ = FastMath.clamp(FastMath.nextRandomFloat() + z, minZ, maxZ);
                grassBlades.add(createGrassBlade(finalX, finalZ, k));
            }
        }
        return grassBlades;
    }

    /**
     * In the method we simply create a GrassBlade object at the given position
     * This time the position is in float !
     *
     * @param x The x position
     * @param z The z position
     * @param k The layerIndex+densityMap
     * @return A grass blade
     */
    GrassBlade createGrassBlade(float x, float z, HashKey k) {
        Layer layer = layerHash.get(k);
        float perlinValue = perlinNoise.getPerlinNoise()[(int) x + terrainSize / 2][(int) z + terrainSize / 2];
        float positionY = terrain.getHeight(new Vector2f(x, z));
        float grassSize = layer.minSize + (FastMath.nextRandomFloat() * (layer.maxSize - layer.minSize));
        return new GrassBlade(new Vector3f(x, positionY, z), perlinValue, grassSize, layer.minX, layer.size);
    }

    /**
     * An accessor to the GrassDist value
     *
     * @return The Grass distance
     */
    float getGrassDist() {
        return grassDist;
    }

    /**
     * Create the material for the GrassArea Geometry (Called by
     * GrassArea.generate())
     *
     * @return The GrassArea material
     */
    public Material createMaterial() {
        Material grassMat = new Material(assetManager, "MatDefs/Grass/grass.j3md");
        grassMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        grassMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        grassMat.getAdditionalRenderState().setDepthTest(true);
        grassMat.getAdditionalRenderState().setDepthWrite(true);
        grassMat.setTexture("ColorMap", colorTexture);
        grassMat.setTexture("Dissolve", dissolveTexture);
        grassMat.setParam("GrassDist", VarType.Float, grassDist * 2);
        return grassMat;
    }

    /**
     * An accesor to the layerNumberMax value
     *
     * @return The number of densiy map
     */
    public int getLayerNumberMax() {
        return layerNumberMax;
    }

    /**
     * An accessor to the density textures value
     *
     * @return The density map textures path
     */
    public ArrayList<String> getDensityTextures() {
        return densityTexturesPath;
    }

    /**
     * An accessor to the layer hash HashMap
     *
     * @return The layer hashmap
     */
    public HashMap<HashKey, Layer> getLayerHash() {
        return layerHash;
    }

    /**
     * An accessor to the perlin noise linked with the GrassFactory
     *
     * @return The perlin noise class
     */
    public PerlinNoise getPerlinNoise() {
        return perlinNoise;
    }

    /**
     * An accessor to the TerrainQuad
     *
     * @return The terrain
     */
    public TerrainQuad getTerrain() {
        return terrain;
    }

    /**
     * Method used by GrassArea in the read process
     *
     * @param layerNumberMax The number of density map
     */
    public void setLayerNumberMax(int layerNumberMax) {
        this.layerNumberMax = layerNumberMax;
    }

    /**
     * Here we give the GrassFactory the list of the density map textures path
     * and create the image raster
     *
     * @param densityTexturesPath
     */
    void setDensityTextures(ArrayList<String> densityTexturesPath) {
        this.densityTexturesPath = densityTexturesPath;
        for (String s : densityTexturesPath) {
            Texture t = assetManager.loadTexture(s);
            densityTextures.add(ImageRaster.create(t.getImage()));
        }
    }

    /**
     * Method used by GrassArea read to set the layer hash map value
     *
     * @param layerHash The layer hash value
     */
    void setLayerHash(HashMap<HashKey, Layer> layerHash) {
        this.layerHash = layerHash;
    }

    /**
     * Method used by GrassArea read process to set the perlin noise
     *
     * @param perlinNoise The perlin noise value
     */
    void setPerlinNoise(PerlinNoise perlinNoise) {
        this.perlinNoise = perlinNoise;
    }

    ArrayList<GrassBlade> updateHeight(ArrayList<GrassBlade> grassBlades) {
        for (GrassBlade blade : grassBlades) {
            blade.position.y = terrain.getHeight(new Vector2f(blade.position.x, blade.position.z));
        }
        return grassBlades;
    }

    /**
     * Adjust or Set the height for the HashKey value of the layerHashMap 
     * @param layer The density map index
     * @param color The color channel index
     * @param height The value of float to adjust the density
     * @param size The size of the 2D float array
     * @param at The location of the change (start at (0,0) of the 2D array)
     * @param adjust Set or Adjust the density value
     */
    void adjustHeight(int layer, int color, float[][] height, int size, Vector2f at, boolean adjust) {
        ImageRaster r = densityTextures.get(layer);
        Vector2f pAt = at.add(new Vector2f(terrainSize / 2, terrainSize / 2));
        for (int i = (int) pAt.x; i < pAt.x + size; i++) {
            for (int j = (int) pAt.y; j < pAt.y + size; j++) {
                if (i >= 0 && i < r.getHeight() && j >= 0 && j < r.getHeight()) {
                    ColorRGBA tempColor = r.getPixel(i, j);
                    switch (color) {
                        case 0: {
                            if (adjust) {
                                tempColor.set(tempColor.r + height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.g, tempColor.b, tempColor.a);
                            } else {
                                tempColor.set(height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.g, tempColor.b, tempColor.a);
                            }
                            break;
                        }
                        case 1: {
                            if (adjust) {
                                tempColor.set(tempColor.r, tempColor.g + height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.b, tempColor.a);
                            } else {
                                tempColor.set(tempColor.r, height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.b, tempColor.a);
                            }

                            break;
                        }
                        case 2: {
                            if (adjust) {
                                tempColor.set(tempColor.r, tempColor.g, tempColor.b + height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.a);
                            } else {
                                tempColor.set(tempColor.r, tempColor.g, height[(int) (i - pAt.x)][(int) (j - pAt.y)], tempColor.a);
                            }

                            break;
                        }
                        case 3: {
                            if (adjust) {
                                tempColor.set(tempColor.r, tempColor.g, tempColor.b, tempColor.a + height[(int) (i - pAt.x)][(int) (j - pAt.y)]);
                            } else {
                                tempColor.set(tempColor.r, tempColor.g, tempColor.b, height[(int) (i - pAt.x)][(int) (j - pAt.y)]);
                            }
                            break;
                        }
                    }
                    tempColor.clamp();
                    r.setPixel(i, j, tempColor);
                }
            }
        }
    }

    void setLayerDensity(int densityMap, int colorChannel, float density) {
        layerHash.get(new HashKey(densityMap, colorChannel)).density = density;
    }

    void setLayerTexture(int densityMap, int colorChannel, float minX, float size) {
        layerHash.get(new HashKey(densityMap, colorChannel)).minX = minX;
        layerHash.get(new HashKey(densityMap, colorChannel)).size = size;
    }

    void setLayerSize(int densityMap, int colorChannel, float minSize, float maxSize) {
        layerHash.get(new HashKey(densityMap, colorChannel)).minSize = minSize;
        layerHash.get(new HashKey(densityMap, colorChannel)).maxSize = maxSize;
    }
}
