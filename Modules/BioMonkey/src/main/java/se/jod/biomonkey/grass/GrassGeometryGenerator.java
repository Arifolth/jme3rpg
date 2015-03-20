/*
 * Copyright (c) 2011, Andreas Olofsson
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
package se.jod.biomonkey.grass;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import java.nio.Buffer;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.RectBounds;
import se.jod.biomonkey.datagrids.DataProvider;
import se.jod.biomonkey.grass.GrassLayer.MeshType;
import se.jod.biomonkey.random.FastRandom;
import se.jod.biomonkey.terrain.TerrainLoader;

/**
 * This class contains a few methods for generating grass meshes.
 *
 * @author Andreas
 */
public class GrassGeometryGenerator {

    protected TerrainLoader terrain;
    protected FastRandom rand;

    public GrassGeometryGenerator(TerrainLoader terrain) {
        this.terrain = terrain;
        rand = new FastRandom();
    }

    /**
     * This method creates a grass geometry. This is this method you call from
     * the grassloader.
     *
     * @param layer The grasslayer.
     * @param block The grassblock.
     * @param page The grass page.
     * @param dataProvider The data provider used for getting planting data.
     * @return A batched grass geometry.
     */
    public Geometry createGrassGeometry(GrassLayer layer,
            GrassBlock block,
            GrassPage page,
            DataProvider dataProvider) {
        // Use layer seed to make sure same numbers are produced every time.
        rand.reSeed(EcoManager.getInstance().getRandomTable().lookup(page, block, layer.getID()));

        //Each "grass data point" consists of coords (x,y,z), scale and rotation-angle.
        float[] grassData = dataProvider.getData(page, block, layer);
        int grassCount = grassData.length / 5;

        //No need running this if there's no grass data.
        if (grassCount != 0) {
            Mesh grassMesh = new Mesh();
            MeshType meshType = layer.getMeshType();
            
            if (meshType == MeshType.QUADS) {
                grassMesh = generateGrass_QUADS(layer, block, grassData, grassCount);
            } else if (meshType == MeshType.CROSSQUADS) {
                grassMesh = generateGrass_CROSSQUADS(layer, block, grassData, grassCount);
            }

            grassMesh.setStatic();
            grassMesh.updateCounts();
            Geometry geom = new Geometry("Grass" + layer.getID());
            geom.setMesh(grassMesh);
            geom.setMaterial(layer.getMaterial());

            return geom;
        } else {
            return null;
        }


    }

    /**
     * Method for creating a static quad mesh.
     *
     * @param layer The grass-layer.
     * @param block The grassblock.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry
     * method.
     *
     * @return A static quad mesh.
     */
    protected Mesh generateGrass_QUADS(GrassLayer layer,
            GrassBlock block,
            float[] grassData,
            int grassCount) {
        //The grass mesh
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);

        // ***************** Setting up the mesh buffers. *****************

        //Each grass has four positions, each vertice is 3 floats
        float[] positions = new float[grassCount * 12];
        float[] normals = new float[grassCount * 12];
        //Each grass has got 4 texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount * 8];

        // Slim the mesh down a little.
        Format form;
        if (grassCount * 4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount * 4 > 255) {
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }

        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount * 6);
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);

        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();

        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();

        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position,texcoord, angle and color iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;

        RectBounds bounds = block.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;

        float maxSlope = layer.getMaxTerrainSlope();

        float xMin = Float.POSITIVE_INFINITY, xMax = Float.NEGATIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY, yMax = Float.NEGATIVE_INFINITY;
        float zMin = Float.POSITIVE_INFINITY, zMax = Float.NEGATIVE_INFINITY;

        //Generating quads
        for (int i = 0; i < grassCount; i++) {
            //Position values
            float x = grassData[gIt++];
            gIt++;
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];

            float halfScaleX = (minWidth + size * (maxWidth - minWidth)) * 0.5f;
            float scaleY = minHeight + size * (maxHeight - minHeight);

            float xAng = (float) (Math.cos(angle));
            float zAng = (float) (Math.sin(angle));

            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;

            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;

            float y1 = terrain.getTerrainHeight(x1, z1);
            float y2 = terrain.getTerrainHeight(x2, z2);

            Vector3f norm = terrain.getTerrainNormal(x, z);

            //Shift to block center.
            x -= cX;
            z -= cZ;

            // Check the angle between y1 and y2. If too steep, collapse the quad.
            float tanDYDX = FastMath.abs((y2 - y1) / (x2 - x1));

            float y1h = y1;
            float y2h = y2;

            // Used to set all texture coordinates to 0 in case the mesh
            // is collapsed.
            float tC = 0;

            //If angle is within bounds, generate a proper quad.
            if (tanDYDX < maxSlope) {
                y1h = y1 + scaleY;
                y2h = y2 + scaleY;
                tC = 1;
            }

            // Used to mirror texture coordinates (to create more variety).

            float tCx = (rand.nextBoolean() == true) ? 1.0f : 0f;
            float tCxneg = 1.0f - tCx;

            // Bounding box
            if (x < xMin) {
                xMin = x;
            }
            if (x > xMax) {
                xMax = x;
            }

            float ym = (y1 < y2) ? y1 : y2;
            float yM = (y1h > y2h) ? y1h : y2h;
            if (ym < yMin) {
                yMin = ym;
            }
            if (yM > yMax) {
                yMax = yM;
            }
            if (z < zMin) {
                zMin = z;
            }
            if (z > zMax) {
                zMax = z;
            }

            // ******************** Adding vertices ********************** 

            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;
            texCoords[tIt++] = tCx;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;
            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1;
            positions[pIt++] = z1 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;
            texCoords[tIt++] = tCx;
            texCoords[tIt++] = 0;                               //uv

            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;
            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = 0;                               //uv

        }

        //************************ Indices **************************

        int iIt = 0;

        int offset;
        IndexBuffer iB = mesh.getIndexBuffer();
        for (int i = 0; i < grassCount; i++) {
            offset = i * 4;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);

            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
        }


        // ******************** Finalizing the mesh ***********************

        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.Normal, 2, normals);

        BoundingBox box = new BoundingBox();
        Vector3f bC = new Vector3f();
        bC.x = (xMax + xMin) * 0.5f;
        bC.y = (yMax + maxHeight + yMin) * 0.5f;
        bC.z = (zMax + zMin) * 0.5f;
        box.setCenter(bC);
        box.setXExtent(xMax - bC.x + maxWidth * 0.5f);
        box.setYExtent(yMax + maxHeight - bC.y);
        box.setZExtent(zMax - bC.z + maxWidth * 0.5f);

        mesh.setBound(box);
        return mesh;
    }

    /**
     * Method for creating a static cross-quad mesh.
     *
     * @param layer The grass-layer.
     * @param block The grassblock.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry
     * method.
     * @return A static cross-quad mesh.
     */
    protected Mesh generateGrass_CROSSQUADS(GrassLayer layer,
            GrassBlock block,
            float[] grassData,
            int grassCount) {
        //The grass mesh
        Mesh mesh = new Mesh();

        mesh.setMode(Mesh.Mode.Triangles);

        // ***************** Setting up the mesh buffers. *****************

        //Each grass has eight positions, each position is 3 floats.
        float[] positions = new float[grassCount * 24];
        //Each grass has got eight texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount * 16];
        //This is the normal of the terrain underneath, to blend when shading.
        float[] normals = new float[grassCount * 24];

        //Slim the mesh down a little.
        Format form;
        if (grassCount * 4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount * 4 > 255) {
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }

        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount * 12);
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);

        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();

        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();

        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position, texcoord and angle iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;

        RectBounds bounds = block.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;

        float maxSlope = layer.getMaxTerrainSlope();

        float xMin = Float.POSITIVE_INFINITY, xMax = Float.NEGATIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY, yMax = Float.NEGATIVE_INFINITY;
        float zMin = Float.POSITIVE_INFINITY, zMax = Float.NEGATIVE_INFINITY;


        //Generating quads
        for (int i = 0; i < grassCount; i++) {
            //Position values
            float x = grassData[gIt++];
            gIt++;
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];

            float halfScaleX = (minWidth + size * (maxWidth - minWidth)) * 0.5f;
            float scaleY = minHeight + size * (maxHeight - minHeight);

            float xAng = (float) (Math.cos(angle));
            float zAng = (float) (Math.sin(angle));

            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;

            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;
            float x3 = x + zTrans, z3 = z - xTrans;
            float x4 = x - zTrans, z4 = z + xTrans;

            float y1 = terrain.getTerrainHeight(x1, z1);
            float y2 = terrain.getTerrainHeight(x2, z2);
            float y3 = terrain.getTerrainHeight(x3, z3);
            float y4 = terrain.getTerrainHeight(x4, z4);

            // TODO correct this when patched.
//            float xN = FastMath.clamp(x,-252,252);
//            float zN = FastMath.clamp(z,-252,252);

            Vector3f norm = terrain.getTerrainNormal(x, z);

            //Normalize to block center (not world center).
            x -= cX;
            z -= cZ;

            // Check the angles. If too steep, collapse the quad.
            float tanDYDX1 = FastMath.abs((y2 - y1) / (x2 - x1));
            float tanDYDX2 = FastMath.abs((y4 - y3) / (x4 - x3));

            float y1h = y1;
            float y2h = y2;
            float y3h = y3;
            float y4h = y4;

            float tC = 0;

            //If angles are within bounds, generate a crossquad.
            if (tanDYDX1 < maxSlope && tanDYDX2 < maxSlope) {
                y1h = y1 + scaleY;
                y2h = y2 + scaleY;
                y3h = y3 + scaleY;
                y4h = y4 + scaleY;
                tC = 1;
            }

            float tCx = (rand.nextBoolean() == true) ? 1.0f : 0f;
            float tCxneg = 1.0f - tCx;

            // Bounding box

            if (x < xMin) {
                xMin = x;
            }
            if (x > xMax) {
                xMax = x;
            }

            float ym1 = (y1 < y2) ? y1 : y2;
            float ym2 = (y3 < y4) ? y3 : y4;
            float ym = (ym1 < ym2) ? ym1 : ym2;

            float yM1 = (y1h > y2h) ? y1h : y2h;
            float yM2 = (y3h > y4h) ? y3h : y4h;
            float yM = (yM1 > yM2) ? yM1 : yM2;

            if (ym < yMin) {
                yMin = ym;
            }
            if (yM > yMax) {
                yMax = yM;
            }
            if (z < zMin) {
                zMin = z;
            }
            if (z > zMax) {
                zMax = z;
            }

            //************Generate the first quad**************

            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCx;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1;
            positions[pIt++] = z1 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCx;
            texCoords[tIt++] = 0;                               //uv

            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = 0;                               //uv

            //************Generate the second quad**************

            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3h;
            positions[pIt++] = z3 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCx;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4h;
            positions[pIt++] = z4 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = tC;                              //uv

            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3;
            positions[pIt++] = z3 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCx;
            texCoords[tIt++] = 0;                               //uv

            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4;
            positions[pIt++] = z4 - cZ;

            normals[nIt++] = norm.x;                            //normal
            normals[nIt++] = norm.y;
            normals[nIt++] = norm.z;

            texCoords[tIt++] = tCxneg;
            texCoords[tIt++] = 0;                               //uv

        }

        //Indices
        int iIt = 0;

        int offset;
        IndexBuffer iB = mesh.getIndexBuffer();
        for (int i = 0; i < grassCount; i++) {
            offset = i * 8;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);

            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);

            iB.put(iIt++, 4 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 5 + offset);

            iB.put(iIt++, 5 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 7 + offset);
        }

        //********************* Finalizing the mesh ***********************

        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.Normal, 3, normals);

        BoundingBox box = new BoundingBox();
        Vector3f bC = new Vector3f();
        bC.x = (xMax + xMin) * 0.5f;
        bC.y = (yMax + maxHeight + yMin) * 0.5f;
        bC.z = (zMax + zMin) * 0.5f;
        box.setCenter(bC);
        box.setXExtent(xMax - bC.x + maxWidth * 0.5f);
        box.setYExtent(yMax + maxHeight - bC.y);
        box.setZExtent(zMax - bC.z + maxWidth * 0.5f);

        mesh.setBound(box);
        return mesh;
    }
    
}//GrassGeometryGenerator
