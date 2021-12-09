/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represent the whole GrassArea grid and is in charge of the whole
 * mesh creation process and is an interface between the GrassHolder (Contains
 * all the information on the grass) and GrassFactory (contains all the
 * information to generate the grass)
 *
 * @author Stomrage
 * @version 0.1
 *
 */
public class GrassArea extends Geometry implements GrassObject {

    private boolean autoUpdate = false;

    public int getSize() {
        return totalSize;
    }

    public int getLocX() {
        return -totalSize / 2;
    }

    public int getLocZ() {
        return -totalSize / 2;
    }

    public void updateAt(Vector2f pointA, Vector2f pointB) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r1 = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                Rectangle r2 = new Rectangle((int) pointA.x, (int) pointA.y, (int) (pointB.x - pointA.x), (int) (pointB.y - pointA.y));
                if (r1.intersects(r2)) {
                    go.updateAt(pointA, pointB);
                }
            }
        }
    }

    public void generateAt(Vector2f pointA, Vector2f pointB) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r1 = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                Rectangle r2 = new Rectangle((int) pointA.x, (int) pointA.y, (int) (pointB.x - pointA.x), (int) (pointB.y - pointA.y));
                if (r1.intersects(r2)) {
                    go.generateAt(pointA, pointB);
                }
            }
        }
    }

    public void updateAt(Vector2f pointA) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                if (r.contains(pointA.x, pointA.y)) {
                    go.updateAt(pointA);
                }
            }
        }
    }

    public void generateAt(Vector2f pointA) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GrassObject go = grassPatchs[i][j];
                Rectangle r = new Rectangle(go.getLocX(), go.getLocZ(), go.getSize() * 2, go.getSize() * 2);
                if (r.contains(pointA.x, pointA.y)) {
                    go.generateAt(pointA);
                }
            }
        }
    }

    /**
     * This function is use to adjust the density map at the given point with an
     * array of float value which will influence the density map Warning : The
     * array must be a 2D array with the same size float[size][size]
     *
     * @param densityMap The density map to change
     * @param colorChannel The color channel to modify
     * @param height An array of float (+ or -) of the density to add
     * @param size The size of the 2D array
     * @param at The location of modification
     */
    public void adjustDensity(DensityMap densityMap, ColorChannel colorChannel, float[][] height, int size, Vector2f at) {
        GrassFactory.getInstance().adjustHeight(densityMap.ordinal(), colorChannel.ordinal(), height, size, at, true);
        if (autoUpdate) {
            this.generateAt(at, at.add(new Vector2f(size, size)));
        }
    }

    /**
     * Same as @see adjustDensity but this time the height is set to the 2d array in parameter
     * @param densityMap The density map to change
     * @param colorChannel The color channel to modify
     * @param height An array of float (+ or -) of the density to set
     * @param size The size of the 2D array
     * @param at The location of modification
     */
    public void setDensity(DensityMap densityMap, ColorChannel colorChannel, float[][] height, int size, Vector2f at) {
        GrassFactory.getInstance().adjustHeight(densityMap.ordinal(), colorChannel.ordinal(), height, size, at, false);
        if (autoUpdate) {
            this.generateAt(at, at.add(new Vector2f(size, size)));
        }
    }

    /**
     * Set the density of the given layer
     * @param densityMap The density map to change
     * @param colorChannel The color channel to change
     * @param density The next density of this layer
     */
    public void setLayerDensity(DensityMap densityMap, ColorChannel colorChannel, float density) {
        GrassFactory.getInstance().setLayerDensity(densityMap.ordinal(), colorChannel.ordinal(), density);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * Change the texture atlas parameter of the given layer
     * @param densityMap The density map to change
     * @param colorChannel The color channel to change
     * @param minX The start index of the texture tile to reach
     * @param size The size of the texture to reach
     */
    public void setLayerTexture(DensityMap densityMap, ColorChannel colorChannel, float minX, float size) {
        GrassFactory.getInstance().setLayerTexture(densityMap.ordinal(), colorChannel.ordinal(), minX, size);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * Set the minimum and maximum grass size for the given layer
     * @param densityMap The density map to change
     * @param colorChannel The color channel to change
     * @param minSize Minimum size of the Grass
     * @param maxSize Maximum size of the Grass
     */
    public void setLayerSize(DensityMap densityMap, ColorChannel colorChannel, float minSize, float maxSize) {
        GrassFactory.getInstance().setLayerSize(densityMap.ordinal(), colorChannel.ordinal(), minSize, maxSize);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * This function is use to set the auto update parameter of the GrassArea
     * @param autoUpdate Set auto update
     */
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    //The density map texture to use for the current layer
    public enum DensityMap {

        DENSITY_MAP_1, DENSITY_MAP_2, DENSITY_MAP_3, DENSITY_MAP_4
    };

    //The color channel to use for the current layer
    public enum ColorChannel {

        RED_CHANNEL, GREEN_CHANNEL, BLUE_CHANNEL, ALPHA_CHANNEL
    };
    //Contains all the 4 grass objects linked to GrassArea
    private GrassObject[][] grassPatchs;
    /**
     * @see GrassObject
     */
    private Vector3f location;
    //The total size of the GrassArea (which is in fact the size of the TerrainQuad)
    private int totalSize;
    //The total number of division to generate the whole GrassArea to the GrassHolder
    private int division = 0;

    /**
     * The GrassArea empty constructor needed by a Savable object
     *
     * @see Savable
     */
    public GrassArea() {
    }

    /**
     * The main constructor to generate a GrassArea over the TerrainQuad
     * geometry.
     *
     * @param terrain The TerrainQuad to use to generate the GrassArea
     * @param holderSize The minimum size of the GrassArea grid splitting (Size
     * of the
     * @see GrassHolder)
     * @param assetManager The assetManager is needed by the
     * @see GrassFactory to generate the grass material
     * @param grassDist The draw distance of the grass
     *
     */
    public GrassArea(TerrainQuad terrain, int holderSize, AssetManager assetManager, float grassDist) throws Exception {
        super("grass_area");
        this.grassPatchs = new GrassObject[2][2];
        division = calculateDivision(terrain.getTerrainSize() - 1, holderSize);
        mesh = new Mesh();
        this.totalSize = terrain.getTerrainSize();
        if (!FastMath.isPowerOfTwo(totalSize - 1)) {
            //In practics it can't happend since the TerrainQuad already check for it
            throw new Exception("The terrain size must be a power of 2");
        }
        if (!FastMath.isPowerOfTwo(holderSize)) {
            throw new Exception("The holderSize must be a power of 2");
        }
        //Check for dumb holder size value
        if (holderSize >= (totalSize - 1)) {
            throw new Exception("The holder size cannot be superior to the terrain size");
        }
        if (grassDist <= 0) {
            throw new Exception("The grass distance cannot be <= to 0");
        }
        //The GrassArea as well as the TerrainQuad is located in 0,0,0
        location = new Vector3f(0, 0, 0);

        GrassFactory.getInstance().setGrassDistance(grassDist);
        GrassFactory.getInstance().setTerrain(terrain);
        GrassFactory.getInstance().setAssetManager(assetManager);
        //Generate the perlin noise with a size of totalSize and a turbulence of totalSize/100 (This value will be changed)
        GrassFactory.getInstance().createPerlinNoise(totalSize, totalSize / 100);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                //We generate the 2 by 2 GrassPatch first subdivision
                grassPatchs[x][z] = new GrassPatch(totalSize / 2 * x - (totalSize / 2), totalSize / 2 * z - (totalSize / 2), totalSize / 2, holderSize);
            }
        }
        this.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    /**
     * This function calculate the number of subdivions needed to reach the
     * GrassHolder
     *
     * @param terrainSize The size of the terrain-1
     * @param holderSize The size of the GrassHolder
     * @return The number of divions
     */
    private int calculateDivision(int terrainSize, int holderSize) {
        int i = terrainSize;
        int div = 0;
        while (i != holderSize) {
            i = i / 2;
            div++;
        }
        return div;
    }

    /**
     * Add a density map to the GrassArea. A density map is here to change the
     * density of the grass given by the rgba of the texture value, Each added
     * density map give you 4 more layer to add
     *
     * @param densityMap The density map Texture
     */
    public void addDensityMap(Texture densityMap) {
        GrassFactory.getInstance().addDensityMap(densityMap);
    }

    /**
     * Add a layer to the GrassArea. A layer is linked to a DensityMap and a
     * ColorChannel, you can't add the same ColorChannel to a single density map
     * 2 times.
     *
     * @param minX The start index of the texture tile to reach
     * @param texSize The size of the texture to reach
     * @param density The density of the layer, this number will be multiplied
     * by the density map value to give the number of grass to add a this point
     * @param index Which ColorChannel to use for this layer
     * @param densityMap Which DensityMap to use for this layer
     * @param minSize The minimum size of the grass for this layer
     * @param maxSize The maximum size of the grass for this layer
     */
    public void addLayer(float minX, float texSize, float density, ColorChannel index, DensityMap densityMap, float minSize, float maxSize) throws Exception {
        GrassFactory.getInstance().addLayer(minX, texSize, density, index.ordinal(), densityMap.ordinal(), minSize, maxSize);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * An accessor to the number of subdivisions
     *
     * @return The number of subdivions
     */
    public int getDivision() {
        return division;
    }

    /**
     * @see GrassObject
     * @return the Location of this GrassObject
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * This texture is an atlas for the GrassArea, the atlas need to be X tiled
     * if you want it to work _ _ |_|_|... Basicly it contains all the variation
     * for your grass area
     *
     * @param color The tiled color map texture
     */
    public void setColorTexture(Texture color) {
        GrassFactory.getInstance().setColorTexture(color);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * Define the dissolve texture for the GrassArea, this dissolve texture will
     * be used to make the texture dissolve in the far away
     *
     * @param dissolve The dissolve texture
     */
    public void setDissolveTexture(Texture dissolve) {
        GrassFactory.getInstance().setDissolveTexture(dissolve);
        if (autoUpdate) {
            this.generate();
        }
    }

    /**
     * Generate the whole GrassArea tree
     */
    public void generate() {
        this.setMaterial(GrassFactory.getInstance().createMaterial());
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                grassPatchs[x][z].generate();
            }
        }
    }

    /**
     * This method is here to create the whole GrassArea mesh. The
     * GrassAreaControl check for close Grass and give it to this method
     *
     * @param grassBlades The list of grass blade to draw
     */
    public void createMesh(List<GrassBlade> grassBlades) {
        int triCount = grassBlades.size();
        //The position buffer
        FloatBuffer pb = BufferUtils.createVector3Buffer(triCount * 4);
        //The normal buffer
        FloatBuffer nb = BufferUtils.createVector3Buffer(triCount * 4);
        //The texCoord1 buffer
        FloatBuffer tb1 = BufferUtils.createVector2Buffer(triCount * 4);
        //The texCoord2 buffer, it will hold the perlin noise color information
        //and the size of the GrassQuad as well as the vertice y alignement
        FloatBuffer tb2 = BufferUtils.createVector2Buffer(triCount * 4);
        //The texCoord3 buffer, we write this buffer to get the atlas texture
        //of the layer
        FloatBuffer tb3 = BufferUtils.createVector2Buffer(triCount * 4);
        //The index buffer
        IntBuffer ib = BufferUtils.createIntBuffer(triCount * 6);

        //For each grass blade we increase this variable to create the index buffer
        int indexBufferHelper = 0;
        for (GrassBlade grassB : grassBlades) {
            Vector3f p1 = grassB.position;
            float size = grassB.size;
            float color = grassB.color;
            float minX = grassB.minX;
            float texSize = grassB.texSize;

            pb.put(p1.x - size / 2).put(p1.y + 0).put(p1.z + 0);
            pb.put(p1.x + size / 2).put(p1.y + 0).put(p1.z + 0);
            pb.put(p1.x - size / 2).put(p1.y + size).put(p1.z + 0);
            pb.put(p1.x + size / 2).put(p1.y + size).put(p1.z + 0);

            tb1.put(0).put(0);
            tb1.put(1).put(0);
            tb1.put(0).put(1);
            tb1.put(1).put(1);

            tb2.put(color).put(size);
            tb2.put(color).put(-size);
            tb2.put(color).put(size);
            tb2.put(color).put(-size);

            tb3.put(minX).put(texSize);
            tb3.put(minX).put(texSize);
            tb3.put(minX).put(texSize);
            tb3.put(minX).put(texSize);

            nb.put(0).put(0).put(1);
            nb.put(0).put(0).put(1);
            nb.put(0).put(0).put(1);
            nb.put(0).put(0).put(1);

            ib.put(2 + indexBufferHelper * 4).put(0 + indexBufferHelper * 4).put(1 + indexBufferHelper * 4).put(1 + indexBufferHelper * 4).put(3 + indexBufferHelper * 4).put(2 + indexBufferHelper * 4);
            indexBufferHelper++;
        }
        if (!mesh.getBufferList().isEmpty()) {
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.Position).getData());
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.Normal).getData());
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.TexCoord).getData());
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.TexCoord2).getData());
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.TexCoord3).getData());
            BufferUtils.destroyDirectBuffer(mesh.getBuffer(VertexBuffer.Type.Index).getData());
        }

        mesh.setBuffer(VertexBuffer.Type.Position, 3, pb);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, nb);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tb1);
        mesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, tb2);
        mesh.setBuffer(VertexBuffer.Type.TexCoord3, 2, tb3);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, ib);
        //Update the model bound. (Needed ?)
        this.updateModelBound();
    }

    /**
     * This method give access to the GrassObjects held by the GrassArea
     *
     * @return The GrassObjects 2D array
     */
    GrassObject[][] getPatch() {
        return grassPatchs;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        location = (Vector3f) capsule.readSavable("location", new Vector3f());
        totalSize = capsule.readInt("totalSize", 0);
        division = capsule.readInt("division", 0);
        grassPatchs = GrassUtils.toArray2D(capsule.readSavableArrayList("grassPatchs", new ArrayList<GrassObject>()), 2);
        readGrassFactory(im);
    }

    public void readGrassFactory(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        float grassDist = capsule.readFloat("grassDist", 0);
        int layerNumberMax = capsule.readInt("layerNumberMax", 0);
        ArrayList<SavableString> densitySavable = capsule.readSavableArrayList("densityTextures", new ArrayList<SavableString>());
        HashMap<HashKey, Layer> layerHash = (HashMap<HashKey, Layer>) capsule.readSavableMap("layerHash", new HashMap<HashKey, Layer>());
        PerlinNoise perlinNoise = (PerlinNoise) capsule.readSavable("perlinNoise", new PerlinNoise());
        TerrainQuad terrain = (TerrainQuad) capsule.readSavable("terrain", new TerrainQuad());

        GrassFactory.getInstance().setAssetManager(im.getAssetManager());
        GrassFactory.getInstance().setGrassDistance(grassDist);
        GrassFactory.getInstance().setLayerNumberMax(layerNumberMax);
        ArrayList<String> densityTextures = new ArrayList<String>();
        for (SavableString sSave : densitySavable) {
            densityTextures.add(sSave.string);
        }
        GrassFactory.getInstance().setDensityTextures(densityTextures);
        GrassFactory.getInstance().setLayerHash(layerHash);
        GrassFactory.getInstance().setPerlinNoise(perlinNoise);
        GrassFactory.getInstance().setTerrain(terrain);
    }

    public void writeGrassFactory(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        float grassDist = GrassFactory.getInstance().getGrassDist();
        int layerNumberMax = GrassFactory.getInstance().getLayerNumberMax();
        ArrayList<String> densityTextures = GrassFactory.getInstance().getDensityTextures();
        HashMap<HashKey, Layer> layerHash = GrassFactory.getInstance().getLayerHash();
        PerlinNoise perlinNoise = GrassFactory.getInstance().getPerlinNoise();
        TerrainQuad terrain = GrassFactory.getInstance().getTerrain();

        capsule.write(grassDist, "grassDist", 0);
        capsule.write(layerNumberMax, "layerNumberMax", 0);
        ArrayList<SavableString> densitySavable = new ArrayList<SavableString>();
        for (String s : densityTextures) {
            densitySavable.add(new SavableString(s));
        }
        capsule.writeSavableArrayList(densitySavable, "densityTextures", new ArrayList<SavableString>());
        capsule.write(perlinNoise, "perlinNoise", new PerlinNoise());
        capsule.write(terrain, "terrain", new TerrainQuad());
        capsule.writeSavableMap(layerHash, "layerHash", new HashMap<HashKey, Layer>());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(location, "location", new Vector3f());
        capsule.write(totalSize, "totalSize", 0);
        capsule.write(division, "division", 0);
        capsule.writeSavableArrayList((ArrayList<GrassObject>) GrassUtils.toArrayList(grassPatchs, 2), "grassPatchs", new ArrayList<GrassObject>());
        writeGrassFactory(ex);
    }
}
