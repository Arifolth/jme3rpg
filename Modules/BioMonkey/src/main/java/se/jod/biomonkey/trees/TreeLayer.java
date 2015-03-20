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
package se.jod.biomonkey.trees;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import se.jod.biomonkey.BoundingCylinder;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.MaterialSP;
import se.jod.biomonkey.image.AlphaMipmapGenerator;
import se.jod.biomonkey.paging.DetailLevel;
import se.jod.biomonkey.paging.GeometryLayer;
import se.jod.biomonkey.paging.GeometryPageLoader;
import se.jod.biomonkey.trees.impostors.ImpostorTextureGenerator;

/**
 * Class for storing different tree types.
 *
 * @author Andreas
 */
public class TreeLayer extends GeometryLayer {

    protected String impostorTextureDir = "Trees/Impostors/";
    protected static final Logger log = Logger.getLogger(TreeLayer.class.getName());
    protected Node model;
    protected Spatial collisionModel;
    protected BoundingCylinder boundingCylinder;
    protected float minimumScale = 0.8f;
    protected float maximumScale = 1.2f;
    protected Texture impostorTexture;
    protected MaterialSP impostorMaterial;

    public TreeLayer(Spatial model) {
        super();
        setModel(model);
    }

    public TreeLayer() {
        super();
    }

    // Prepare model for batching, collision physics and impostor rendering.
    protected void prepareModel() {

        // Set proper materials
        for (int i = 0; i < model.getChildren().size(); i++) {
            Geometry geom = (Geometry) model.getChild(i);
            Material oldMat = geom.getMaterial();
            if (!"Phong Lighting".equals(oldMat.getMaterialDef().getName())) {
                throw new RuntimeException("Model : " + model.getName() + " does not "
                        + "use the Lighting.j3md material definition.");
            }
            AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
            Material newMat;
            if (geom.<Boolean>getUserData("Foliage") != null) {
                newMat = new MaterialSP(assetManager, "se/jod/biomonkey/assets/matdefs/FoliageBase.j3md");

                // DiffuseMap
                if (oldMat.getTextureParam("DiffuseMap") != null) {
                    newMat.setTexture("DiffuseMap", oldMat.getTextureParam("DiffuseMap").getTextureValue());
                }
                // Normal map
                if (oldMat.getTextureParam("NormalMap") != null) {
                    newMat.setTexture("NormalMap", oldMat.getTextureParam("NormalMap").getTextureValue());
                    if (oldMat.getParam("LATC") != null) {
                        newMat.setBoolean("LATC", (Boolean) oldMat.getParam("LATC").getValue());
                    }
                }
                // SpecularMap
                if (oldMat.getTextureParam("SpecularMap") != null) {
                    newMat.setTexture("SpecularMap", oldMat.getTextureParam("SpecularMap").getTextureValue());
                }
                // AlphaMap
//                if (oldMat.getTextureParam("AlphaMap") != null) {
//                    newMat.setTexture("AlphaMap", oldMat.getTextureParam("AlphaMap").getTextureValue());
//                }
                // LightMap
//                if (oldMat.getTextureParam("LightMap") != null) {
//                    newMat.setTexture("LightMap", oldMat.getTextureParam("LightMap").getTextureValue());
//                    if (oldMat.getParam("SeparateTexCoord") != null) {
//                        newMat.setBoolean("SeparateTexCoord", (Boolean) oldMat.getParam("SeparateTexCoord").getValue());
//                    }
//                }

                // Material colors
                if (oldMat.getParam("UseMaterialColors") != null) {
                    if ((Boolean) oldMat.getParam("UseMaterialColors").getValue() == true) {
                        newMat.setBoolean("UseMaterialColors", true);
                        if (oldMat.getParam("Ambient") != null) {
                            newMat.setColor("Ambient", (ColorRGBA) oldMat.getParam("Ambient").getValue());
                        }
                        if (oldMat.getParam("Diffuse") != null) {
                            newMat.setColor("Diffuse", (ColorRGBA) oldMat.getParam("Diffuse").getValue());
                        }
                        if (oldMat.getParam("Specular") != null) {
                            newMat.setColor("Specular", (ColorRGBA) oldMat.getParam("Specular").getValue());
                        }
                    }
                }
                // Shininess
                if (oldMat.getParam("Shininess") != null) {
                    newMat.setFloat("Shininess", (Float) oldMat.getParam("Shininess").getValue());
                }
                // Vertex lighting.
                if (oldMat.getParam("VertexLighting") != null) {
                    newMat.setBoolean("VertexLighting", (Boolean) oldMat.getParam("VertexLighting").getValue());
                }

                // Alpha cut-off.
                newMat.setFloat("AlphaDiscardThreshold", (Float) oldMat.getParam("AlphaDiscardThreshold").getValue());

            } else {
                newMat = new MaterialSP(assetManager, "se/jod/biomonkey/assets/matdefs/TreeBase.j3md");

                // DiffuseMap
                if (oldMat.getTextureParam("DiffuseMap") != null) {
                    newMat.setTexture("DiffuseMap", oldMat.getTextureParam("DiffuseMap").getTextureValue());
                }
                // Normal map
                if (oldMat.getTextureParam("NormalMap") != null) {
                    newMat.setTexture("NormalMap", oldMat.getTextureParam("NormalMap").getTextureValue());
                    if (oldMat.getParam("VTangent") != null) {
                        newMat.setBoolean("LATC", (Boolean) oldMat.getParam("LATC").getValue());
                    }
                    if (oldMat.getParam("LATC") != null) {
                        newMat.setBoolean("LATC", (Boolean) oldMat.getParam("LATC").getValue());
                    }
                    if (oldMat.getParam("PackedNormalParallax") != null) {
                        newMat.setBoolean("PackedNormalParallax", (Boolean) oldMat.getParam("PackedNormalParallax").getValue());
                    }
                }
                // SpecularMap
//                if (oldMat.getTextureParam("SpecularMap") != null) {
//                    newMat.setTexture("SpecularMap", oldMat.getTextureParam("SpecularMap").getTextureValue());
//                }
                // LightMap
                if (oldMat.getTextureParam("LightMap") != null) {
                    newMat.setTexture("LightMap", oldMat.getTextureParam("LightMap").getTextureValue());
                    if (oldMat.getParam("SeparateTexCoord") != null) {
                        newMat.setBoolean("SeparateTexCoord", (Boolean) oldMat.getParam("SeparateTexCoord").getValue());
                    }
                }

                // ParallaxMap
                if (oldMat.getTextureParam("ParallaxMap") != null) {
                    newMat.setTexture("ParallaxMap", oldMat.getTextureParam("ParallaxMap").getTextureValue());
                    if (oldMat.getParam("ParallaxHeight") != null) {
                        newMat.setFloat("ParallaxHeight", (Float) oldMat.getParam("ParallaxHeight").getValue());
                    }
                }

                // Material colors
                if (oldMat.getParam("UseMaterialColors") != null) {
                    if ((Boolean) oldMat.getParam("UseMaterialColors").getValue() == true) {
                        newMat.setBoolean("UseMaterialColors", true);
                        if (oldMat.getParam("Ambient") != null) {
                            newMat.setColor("Ambient", (ColorRGBA) oldMat.getParam("Ambient").getValue());
                        }
                        if (oldMat.getParam("Diffuse") != null) {
                            newMat.setColor("Diffuse", (ColorRGBA) oldMat.getParam("Diffuse").getValue());
                        }
                        if (oldMat.getParam("Specular") != null) {
                            newMat.setColor("Specular", (ColorRGBA) oldMat.getParam("Specular").getValue());
                        }
                    }
                }
                // Shininess
                if (oldMat.getParam("Shininess") != null) {
                    newMat.setFloat("Shininess", (Float) oldMat.getParam("Shininess").getValue());
                }
                // Vertex lighting.
                if (oldMat.getParam("VertexLighting") != null) {
                    newMat.setBoolean("VertexLighting", (Boolean) oldMat.getParam("VertexLighting").getValue());
                }
            }
            
            BlendMode bMode = oldMat.getAdditionalRenderState().getBlendMode();
            FaceCullMode fcMode = oldMat.getAdditionalRenderState().getFaceCullMode();
            if (bMode != null) {
                newMat.getAdditionalRenderState().setBlendMode(bMode);
            }
            if (fcMode != null) {
                newMat.getAdditionalRenderState().setFaceCullMode(fcMode);
            }
            EcoManager.getInstance().getAtmosphereManager().getFogManager().addMaterial(newMat);
            geom.setMaterial(newMat);
            if (pageLoader != null) {
                DetailLevel dl = this.getPageLoader().getPagingManager().getDetailLevels().get(0);
                float fadeEnd = dl.getFarTransDist();
                float fadeRange = fadeEnd - dl.getFarDist();
                newMat.setFloat("FadeEnd", fadeEnd);
                newMat.setFloat("FadeRange", fadeRange);
                newMat.setBoolean("Fading", false);
            }
        }

        BoundingCylinder cyl = new BoundingCylinder();
        // Add impostor data
        for (int i = 0; i < model.getChildren().size(); i++) {
            Geometry geom = (Geometry) model.getChild(i);
            cyl.merge(new BoundingCylinder(geom));
            geom.getMesh().setBound(new BoundingSphere());
            geom.updateModelBound();
        }
        model.updateGeometricState();
        this.boundingCylinder = cyl;
        if (pageLoader != null) {
            setupImpostors();
        }
    }

    protected void setupImpostors() {
        int impostorDistance = 0;
        ArrayList<DetailLevel> detailLevels = this.pageLoader.getPagingManager().getDetailLevels();
        
        if (detailLevels.size() > 1) {
            impostorDistance = (int) detailLevels.get(0).getFarDist();
        }
        if (impostorDistance > 0) {

            AssetManager assetManager = EcoManager.getInstance().getApp().getAssetManager();
            String dirName = EcoManager.getInstance().getTextureFolder() + impostorTextureDir;
            String modelName = model.getName() + impostorDistance + ".png";
            String fullDir = dirName + modelName;
            File f = new File(fullDir);
            // TODO pass the texture value directly. Causes some weirdness tho. Find out why (mip probs?).
            if (!f.exists()) {
                Renderer renderer = EcoManager.getInstance().getApp().getRenderer();
                (new ImpostorTextureGenerator(renderer, assetManager)).generateImpostors(model, impostorDistance, this);
            }
            assetManager.registerLocator(dirName, FileLocator.class);
            
            impostorTexture = assetManager.loadTexture(modelName);
            
            
//            AlphaMipmapGenerator.generateMipMaps(impostorTexture.getImage(), 1.2f);
//            impostorTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            impostorMaterial = new MaterialSP(assetManager, "se/jod/biomonkey/assets/matdefs/TreeImpostorBase.j3md");
            Texture tex = assetManager.loadTexture("se/jod/biomonkey/assets/textures/noise.png");
            impostorMaterial.setTexture("AlphaNoiseMap", tex);
            impostorMaterial.setTexture("ImpostorTexture", impostorTexture);

            DetailLevel dl = this.getPageLoader().getPagingManager().getDetailLevels().get(0);
            float fadeEnd = dl.getFarDist();
            float fadeRange = dl.getFarTransDist() - fadeEnd;
            impostorMaterial.setFloat("FadeEnd", fadeEnd);
            impostorMaterial.setFloat("FadeRange", fadeRange);

            EcoManager.getInstance().getAtmosphereManager().getFogManager().addMaterial(impostorMaterial);
        }
    }

    public void setModel(Spatial model) {
        if (model instanceof Geometry) {
            this.model = new Node();
            this.model.attachChild(model);
        } else {
            this.model = (Node) model;
            // For blender importer - model is a node inside a node.
            if (this.model.getChildren().size() == 1 && this.model.getChild(0) instanceof Node) {
                this.model = (Node) this.model.getChild(0);
            }
        }
        if (this.model.getParent() != null) {
            this.model.removeFromParent();
        }
        prepareModel();
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    @Override
    public void setPageLoader(GeometryPageLoader pageLoader) {
        super.setPageLoader(pageLoader);
        for (int i = 0; i < model.getChildren().size(); i++) {
            Material material = ((Geometry) model.getChild(i)).getMaterial();
            DetailLevel dl = this.getPageLoader().getPagingManager().getDetailLevels().get(0);
            float fadeEnd = dl.getFarTransDist();
            float fadeRange = fadeEnd - dl.getFarDist();
            material.setFloat("FadeEnd", fadeEnd);
            material.setFloat("FadeRange", fadeRange);
        }
        setupImpostors();
    }

    public Node getModel() {
        return model;
    }

    public Spatial getCollisionModel() {
        return collisionModel;
    }

    public void setCollisionModel(Spatial collisionModel) {
        this.collisionModel = collisionModel;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public void setMaximumScale(float maxScale) {
        if (maximumScale < minimumScale) {
            throw new RuntimeException("Maximum scale needs to be larger then or equal to minimum scale.");
        }
        maximumScale = maxScale;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMaximumScale() {
        return maximumScale;
    }

    public void setMinimumScale(float minScale) {
        if (minimumScale > maximumScale) {
            throw new RuntimeException("Minimum scale needs to be smaller then or equal to maximum scale.");
        }
        minimumScale = minScale;
        if (pageLoader != null) {
            pageLoader.incrementPageVersion();
        }
    }

    public float getMinimumScale() {
        return minimumScale;
    }

    public MaterialSP getImpostorMaterial() {
        return impostorMaterial;
    }

    public String getImpostorTextureDir() {
        return impostorTextureDir;
    }

    public void setImpostorTextureDir(String impostorTextureDir) {
        this.impostorTextureDir = impostorTextureDir;
    }

    public BoundingCylinder getBoundingCylinder() {
        return boundingCylinder;
    }
}//TreeLayer
