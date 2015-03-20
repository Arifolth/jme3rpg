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
package se.jod.biomonkey.trees;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import se.jod.biomonkey.BoundingCylinder;
import se.jod.biomonkey.MaterialSP;
import se.jod.biomonkey.datagrids.DataProvider;
import se.jod.biomonkey.random.FastRandom;
import se.jod.biomonkey.terrain.TerrainLoader;

/**
 * Class used to generate batched tree meshes, collision shapes and impostors.
 * 
 * @author Andreas
 */
public class TreeGeometryGenerator {

    protected TerrainLoader terrain;
    protected FastRandom rand;

    public TreeGeometryGenerator(TerrainLoader terrain) {
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
     * @param densityMap The densitymap (or null).
     * @return A batched grass geometry.
     */
    public void createBatchedGeometry(ArrayList<TreeLayer> layers,
            TreeBlock block,
            TreePage page,
            DataProvider dataProvider,
            CompoundCollisionShape ccs,
            Node geomNode,
            Node impNode) {

        boolean physics = false;

        if (ccs != null) {
            physics = true;
        }
        
        for (int i = 0; i < layers.size(); i++) {
            // Prepare a node for geometry batches.
            TreeLayer layer = layers.get(i);
            Node model = layer.getModel();
            Spatial lCMod = layer.getCollisionModel();

            // Get planting data.
            float[] data = dataProvider.getData(page, block, layer);
            if (data.length == 0) {
                continue;
            }

            // Correct the data for this layer.
            for (int h = 0; h < data.length / 5; h++) {

                // set location.y to terrain height.
                data[h * 5 + 1] = terrain.getTerrainHeight(data[h * 5], data[h * 5 + 2]);
                // Set xz-location relative to the block center.
                data[h * 5] -= block.getBounds().getCenter().x;
                data[h * 5 + 2] -= block.getBounds().getCenter().z;
                // set scale in the proper range.
                data[h * 5 + 3] = layer.getMinimumScale() + data[h * 5 + 3] * (layer.getMaximumScale() - layer.getMinimumScale());
            }
            // Create a batched geometry.
            batch(model, data, geomNode,layer);

            // Create an impostor mesh if needed.
            if (layer.getImpostorMaterial() != null) {
                createImpostorGeom(data, model, impNode, layer.getImpostorMaterial());
            }
            // If no collision shape is present, or physics is disabled,
            // don't generate a collision shape. Otherwise do.
            if (lCMod == null || !physics) {
                continue;
            }

            int it = 0;
            for (int h = 0; h < data.length / 5; h++) {

                float x = data[it++];
                float y = data[it++];
                float z = data[it++];
                float scale = data[it++];
                float rot = data[it++];

                Spatial cMod = lCMod.clone(false);
                cMod.setLocalScale(scale);
                cMod.setLocalRotation(new Quaternion().fromAngleNormalAxis(rot, Vector3f.UNIT_Y));
                CollisionShape cs = CollisionShapeFactory.createMeshShape(cMod);
                ccs.addChildShape(cs, new Vector3f(x, y, z));
            }
        }
    }

    protected void generatePTN(Mesh inMesh, Mesh outMesh, float[] data, int vertCount, Spatial model, BoundingCylinder cylinder) {

        boolean foliage = false;
        if(cylinder != null){
            foliage = true;
        }
        
        FloatBuffer inPos = (FloatBuffer) inMesh.getBuffer(Type.Position).getData();
        FloatBuffer outPos = (FloatBuffer) outMesh.getBuffer(Type.Position).getData();

        outPos.clear();

        boolean usesNormals = false;
        boolean usesTangents = false;

        VertexBuffer inNormVB = inMesh.getBuffer(Type.Normal);
        VertexBuffer outNormVB = outMesh.getBuffer(Type.Normal);

        FloatBuffer inNorm = null;
        FloatBuffer outNorm = null;

        if (inNormVB != null) {
            usesNormals = true;
            inNorm = (FloatBuffer) inNormVB.getData();
            outNorm = (FloatBuffer) outNormVB.getData();
            outNorm.clear();
        }

        VertexBuffer inTanVB = inMesh.getBuffer(Type.Tangent);
        VertexBuffer outTanVB = outMesh.getBuffer(Type.Tangent);

        FloatBuffer inTan = null;
        FloatBuffer outTan = null;

        int tanComps = 0;

        if (inTanVB != null) {
            usesTangents = true;
            tanComps = inTanVB.getNumComponents();
            inTan = (FloatBuffer) inTanVB.getData();
            outTan = (FloatBuffer) outTanVB.getData();
            outTan.clear();
        }

        // Sphere
        FloatBuffer spBuf = (FloatBuffer) outMesh.getBuffer(Type.TexCoord3).getData();
        spBuf.clear();

        FloatBuffer cBuf = null;

        if (foliage) {
            cBuf = (FloatBuffer) outMesh.getBuffer(Type.TexCoord4).getData();
            cBuf.clear();
        }

        // Storage objects for planting data
        Vector3f basePos = new Vector3f();
        Quaternion baseRot = new Quaternion();
        float baseScale;

        // Storage objects for each vert.
        Vector3f pos = new Vector3f();
        // Position relative to bounding sphere of the model.
        Vector3f spherePos = new Vector3f();
        Vector3f cylPos = new Vector3f();
        Vector3f norm = new Vector3f();
        Vector3f tan = new Vector3f();

        BoundingSphere sphere = (BoundingSphere) model.getWorldBound();
        // Bounding sphere related data. Used to normalize positions.
        Vector3f center = sphere.getCenter();
        float radius = sphere.getRadius();

        // Bounding cylinder data, used to normalize positions as well.
        Vector3f cCenter = null;
        float cRadius = 0;
        float cYExtent = 0;
        if (foliage) {
            cCenter = cylinder.getCenter();
            cRadius = cylinder.getRadius();
            cYExtent = cylinder.getyExtent();
        }
        for (int obj = 0; obj < data.length / 5; obj++) {
            inPos.clear();
            if (usesNormals) {
                inNorm.clear();
            }
            if (usesTangents) {
                inTan.clear();
            }

            basePos.set(data[obj * 5], data[obj * 5 + 1], data[obj * 5 + 2]);
            baseScale = data[obj * 5 + 3];
            baseRot.fromAngleNormalAxis(data[obj * 5 + 4], Vector3f.UNIT_Y);

            float scaledRadius = radius * baseScale;
            float scaledCRadius = cRadius * baseScale;
            float scaledYExtent = cYExtent * baseScale;

            for (int i = 0; i < vertCount; i++) {
                // Positions
                pos.x = inPos.get();
                pos.y = inPos.get();
                pos.z = inPos.get();

                //scale
                pos.multLocal(baseScale);
                //rotate
                baseRot.mult(pos, pos);

                // Before translating, add the positions relative to
                // bounding sphere center and normalized by its size.
                spherePos.x = (pos.x - center.x * baseScale) / scaledRadius;
                spherePos.y = (pos.y - center.y * baseScale) / scaledRadius;
                spherePos.z = (pos.z - center.z * baseScale) / scaledRadius;

                spBuf.put(spherePos.x);
                spBuf.put(spherePos.y);
                spBuf.put(spherePos.z);

                if (foliage) {
                    // Same thing with bounding cylinder in case of foliage.
                    cylPos.x = (pos.x - cCenter.x * baseScale) / scaledCRadius;
                    cylPos.z = (pos.z - cCenter.z * baseScale) / scaledCRadius;
                    cylPos.y = (pos.y - cCenter.y * baseScale) / scaledYExtent;
                    
                    cBuf.put(cylPos.x);
                    cBuf.put(cylPos.y);
                    cBuf.put(cylPos.z);
                }

                // now translate the regular position.
                pos.addLocal(basePos.x, basePos.y, basePos.z);

                outPos.put(pos.x);
                outPos.put(pos.y);
                outPos.put(pos.z);

                if (usesNormals) {
                    norm.x = inNorm.get();
                    norm.y = inNorm.get();
                    norm.z = inNorm.get();
                    //rotate
                    baseRot.mult(norm, norm);

                    outNorm.put(norm.x);
                    outNorm.put(norm.y);
                    outNorm.put(norm.z);
                }

                if (usesTangents) {
                    if (tanComps == 3) {
                        tan.x = inTan.get();
                        tan.y = inTan.get();
                        tan.z = inTan.get();
                        //rotate
                        baseRot.mult(tan, tan);

                        outTan.put(tan.x);
                        outTan.put(tan.y);
                        outTan.put(tan.z);
                    } else if (tanComps == 4) {
                        tan.x = inTan.get();
                        tan.y = inTan.get();
                        tan.z = inTan.get();

                        float w = inTan.get();
                        //rotate
                        baseRot.mult(tan, tan);

                        outTan.put(tan.x);
                        outTan.put(tan.y);
                        outTan.put(tan.z);
                        outTan.put(w);
                    }

                } // if tangents
            } // inner for
        } // outer for
    }

    /**
     * Merges all geometries in the collection into the output mesh. Creates a
     * new material using the TextureAtlas.
     *
     * @param geometries
     * @param outMesh
     */
    protected Geometry batchGeometry(Spatial model, Geometry geom, TreeLayer layer, float[] plantingData) {

        int[] compsForBuf = new int[VertexBuffer.Type.values().length];
        Format[] formatForBuf = new Format[compsForBuf.length];

        int objectCount = plantingData.length / 5;
        int vertCount = geom.getVertexCount();
        int triCount = geom.getTriangleCount();
        int totalVerts = vertCount * objectCount;
        int totalTris = triCount * objectCount;

        for (VertexBuffer vb : geom.getMesh().getBufferList().getArray()) {
            compsForBuf[vb.getBufferType().ordinal()] = vb.getNumComponents();
            formatForBuf[vb.getBufferType().ordinal()] = vb.getFormat();
        }

        compsForBuf[Type.Index.ordinal()] = 3;

        // New mesh.
        Mesh outMesh = new Mesh();
        outMesh.setMode(Mode.Triangles);

        if (totalVerts >= 65536) {
            // make sure we create an UnsignedInt buffer so
            // we can fit all of the meshes
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedInt;
        } else {
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedShort;
        }

        // generate output buffers based on retrieved info
        for (int i = 0; i < compsForBuf.length; i++) {
            if (compsForBuf[i] == 0) {
                continue;
            }

            Buffer data;
            if (i == Type.Index.ordinal()) {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalTris);
            } else {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalVerts);
            }

            VertexBuffer vb = new VertexBuffer(Type.values()[i]);
            vb.setupData(Usage.Static, compsForBuf[i], formatForBuf[i], data);
            outMesh.setBuffer(vb);
        }

        // Add a buffer for bounding-sphere relative position coordinates.
        Buffer data = BufferUtils.createFloatBuffer(totalVerts * 3);
        VertexBuffer vb = new VertexBuffer(Type.TexCoord3);
        vb.setupData(Usage.Static, 3, VertexBuffer.Format.Float, data);
        outMesh.setBuffer(vb);

        // Generate position, normal and tangent for the new mesh.
        boolean foliage = false;
        // TODO Put this back in.
//        if (geom.getUserData("Foliage") != null) {
//            foliage = true;
//        }

        if (foliage) {
            // Add a buffer for bounding-sphere relative position coordinates.
            Buffer data2 = BufferUtils.createFloatBuffer(totalVerts * 3);
            VertexBuffer vb2 = new VertexBuffer(Type.TexCoord4);
            vb2.setupData(Usage.Static, 3, VertexBuffer.Format.Float, data2);
            outMesh.setBuffer(vb2);
        }

        Mesh inMesh = geom.getMesh();

        for (int bufType = 0; bufType < compsForBuf.length; bufType++) {
            VertexBuffer inBuf = inMesh.getBuffer(Type.values()[bufType]);
            VertexBuffer outBuf = outMesh.getBuffer(Type.values()[bufType]);

            if (inBuf == null || outBuf == null) {
                continue;
            }
            // Add indices with offset "vertCount" for each object.
            if (Type.Index.ordinal() == bufType) {

                IndexBuffer inIdx = inMesh.getIndicesAsList();
                IndexBuffer outIdx = outMesh.getIndexBuffer();

                for (int obj = 0; obj < objectCount; obj++) {
                    for (int tri = 0; tri < triCount; tri++) {
                        int gTriCount = triCount * obj;
                        int gVertCount = vertCount * obj;
                        int idx = inIdx.get(tri * 3 + 0) + gVertCount;
                        outIdx.put((gTriCount + tri) * 3 + 0, idx);
                        idx = inIdx.get(tri * 3 + 1) + gVertCount;
                        outIdx.put((gTriCount + tri) * 3 + 1, idx);
                        idx = inIdx.get(tri * 3 + 2) + gVertCount;
                        outIdx.put((gTriCount + tri) * 3 + 2, idx);
                    }
                }
                // If the buffer type is not pos, norm or tan.
            } else if (Type.Position.ordinal() != bufType
                    && Type.Normal.ordinal() != bufType
                    && Type.Tangent.ordinal() != bufType) {
                // Just clone the original buffer once per instance.
                for (int i = 0; i < objectCount; i++) {
                    inBuf.getNumElements();
                    inBuf.copyElements(0, outBuf, vertCount * i, vertCount);
                }
            }
        }
        BoundingCylinder bC = null;
        if(foliage){
            bC = layer.getBoundingCylinder();
        }
        generatePTN(inMesh, outMesh, plantingData, vertCount, model, bC);

        outMesh.updateBound();
        outMesh.updateCounts();
        outMesh.setStatic();
        Geometry newGeom = new Geometry("BatchGeom", outMesh);
        newGeom.setMaterial(geom.getMaterial());

        return newGeom;
    }

    protected void batch(Node model, float[] data, Node batchNode, TreeLayer layer) {

        for (int i = 0; i < model.getChildren().size(); i++) {
            Geometry batch = batchGeometry(model, (Geometry) model.getChild(i), layer, data);
            batchNode.attachChild(batch);
        }
    }

    protected void createImpostorGeom(float[] data, Node model, Node impNode, MaterialSP mat) {
        //TODO remove this
        if (data.length == 0) {
            return;
        }

        Mesh mesh = new Mesh();

        mesh.setMode(Mesh.Mode.Triangles);
        mesh.setDynamic();

        int objectCount = data.length / 5;

        //Position float array.
        float[] positions = new float[12 * objectCount];
        float[] texCoords = new float[8 * objectCount];
        float[] texCoords2 = new float[12 * objectCount];
        int[] indices = new int[6 * objectCount];

        // Default values.
        BoundingSphere treeSphere = (BoundingSphere) model.getWorldBound();

        float rad = treeSphere.getRadius();
        Vector3f center = treeSphere.getCenter();
        float maxRad = 0;

        // Iterators
        int pIt = 0;
        int tIt = 0;
        int t2It = 0;
        int iIt = 0;

        Vector3f scaledCenter = new Vector3f();

        for (int i = 0; i < objectCount; i++) {
            float x = data[i * 5];
            float y = data[i * 5 + 1];
            float z = data[i * 5 + 2];
            float scale = data[i * 5 + 3];
            float rot = data[i * 5 + 4];
            //Position values

            float modRad = rad * scale;
            scaledCenter.set(center.mult(scale));

            if (modRad > maxRad) { // For boundingbox.
                maxRad = modRad;
            }
            // Left to right
            float x0 = -modRad;
            float x1 = modRad;
            // Bottom to top
            float y0 = 0;
            float y1 = 2f * modRad;

            // ******************** Adding vertices ********************** 

            //All four verts in a quad is at the same position.
            for (int j = 0; j < 4; j++) {
                positions[pIt++] = x;
                //Models are designed with center at their bottom.
                positions[pIt++] = y + scaledCenter.y - modRad;
                positions[pIt++] = z;
            }

            //Texcoords for each vert + the position relative to the quad center
            //stored in texCoord2.
            texCoords[tIt++] = 0f;
            texCoords[tIt++] = 0f;
            texCoords2[t2It++] = x0;
            texCoords2[t2It++] = y0;
            texCoords2[t2It++] = rot;

            texCoords[tIt++] = 1f;
            texCoords[tIt++] = 0f;
            texCoords2[t2It++] = x1;
            texCoords2[t2It++] = y0;
            texCoords2[t2It++] = rot;

            texCoords[tIt++] = 0f;
            texCoords[tIt++] = 1f;
            texCoords2[t2It++] = x0;
            texCoords2[t2It++] = y1;
            texCoords2[t2It++] = rot;

            texCoords[tIt++] = 1f;
            texCoords[tIt++] = 1f;
            texCoords2[t2It++] = x1;
            texCoords2[t2It++] = y1;
            texCoords2[t2It++] = rot;

            //Indices
            int offset = i * 4;
            //First triangle
            indices[iIt++] = 0 + offset;
            indices[iIt++] = 1 + offset;
            indices[iIt++] = 2 + offset;
            //Second triangle
            indices[iIt++] = 1 + offset;
            indices[iIt++] = 3 + offset;
            indices[iIt++] = 2 + offset;
        }

        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoords);
        mesh.setBuffer(VertexBuffer.Type.TexCoord2, 3, texCoords2);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, indices);

        // The buffers are now finished.
        BoundingBox box = new BoundingBox();
        mesh.setBound(box);
        mesh.updateBound();
        box.setYExtent(box.getYExtent() + maxRad * 2);
        box.setXExtent(box.getXExtent() + maxRad);
        box.setZExtent(box.getZExtent() + maxRad);
        mesh.updateCounts();
        mesh.setStatic();

        Geometry geom = new Geometry("ImpostorGeom");
        geom.setMesh(mesh);
        geom.setMaterial(mat);
        geom.setQueueBucket(Bucket.Opaque);
        impNode.attachChild(geom);
    }
}
