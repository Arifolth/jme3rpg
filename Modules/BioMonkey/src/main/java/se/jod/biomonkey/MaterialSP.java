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
package se.jod.biomonkey;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.util.ArrayList;

/**
 * Material class for single pass lighting. Added some render state copying,
 * light culling etc.
 *
 * This class is part of Survivor's shader lib
 * (https://code.google.com/p/survivor-jme/), by
 *
 * @author Survivor
 */
public class MaterialSP extends Material {

    protected static boolean cullLights = false;
    protected ArrayList<Light> lightList = new ArrayList<Light>(4);

    public MaterialSP(MaterialDef def) {
        super(def);
    }

    public MaterialSP(AssetManager assetManager, String defName) {
        super(assetManager, defName);
    }

    public MaterialSP(Material mat) {
        super(mat.getMaterialDef());

        for (MatParam param : mat.getParams()) {
            if (param instanceof MatParamTexture) {
                this.setTextureParam(param.getName(), param.getVarType(), (Texture) param.getValue());
            } else {
                this.setParam(param.getName(), param.getVarType(), param.getValue());
            }
        }

        this.setTransparent(mat.isTransparent());
        this.setReceivesShadows(mat.isReceivesShadows());

        RenderState state = mat.getAdditionalRenderState();
        getAdditionalRenderState().setBlendMode(state.getBlendMode());
        getAdditionalRenderState().setWireframe(state.isWireframe());
        getAdditionalRenderState().setPointSprite(state.isPointSprite());
        getAdditionalRenderState().setAlphaFallOff(mat.getAdditionalRenderState().getAlphaFallOff());
        getAdditionalRenderState().setAlphaTest(mat.getAdditionalRenderState().isAlphaTest());
        getAdditionalRenderState().setFaceCullMode(mat.getAdditionalRenderState().getFaceCullMode());
        getAdditionalRenderState().setColorWrite(mat.getAdditionalRenderState().isColorWrite());
        getAdditionalRenderState().setDepthTest(mat.getAdditionalRenderState().isDepthTest());
        getAdditionalRenderState().setDepthWrite(mat.getAdditionalRenderState().isDepthWrite());

        getAdditionalRenderState().setPolyOffset(state.getPolyOffsetFactor(), mat.getAdditionalRenderState().getPolyOffsetUnits());

        getAdditionalRenderState().setStencil(state.isStencilTest(), state.getFrontStencilStencilFailOperation(),
                state.getFrontStencilDepthFailOperation(), state.getFrontStencilDepthPassOperation(),
                state.getBackStencilStencilFailOperation(), state.getBackStencilDepthFailOperation(),
                state.getBackStencilDepthPassOperation(), state.getFrontStencilFunction(), state.getBackStencilFunction());

    }

    public MaterialSP(String matName, AssetManager assetManager) {
        this(assetManager.loadMaterial(matName));
    }

    protected float calculateSpan(Geometry g) {
        BoundingVolume bv = g.getModelBound();
        if(bv instanceof BoundingBox){
        BoundingBox bb = (BoundingBox) bv;
        // Get the maximum span.
        float m = Math.max(bb.getXExtent(), bb.getYExtent());
        m = Math.max(m, bb.getZExtent());
        // Span is the largest possible span, so we don't have to calculate
        // it based on the direction of the light etc. (which would be
        // a lot of work).
        return m * 1.733f; // square root of 3.
        } else {
            // It's a sphere
            BoundingSphere bs = (BoundingSphere) bv;
            return bs.getRadius();
        }
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public MaterialSP() {
    }

    @Override
    protected void updateLightListUniforms(Shader shader, Geometry g, int numLights) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return;
        }

        LightList worldLightList = g.getWorldLightList();
        ColorRGBA ambLightColor = new ColorRGBA(0f, 0f, 0f, 1f);

        if (cullLights) {
            Vector3f center = g.getWorldTranslation().add(g.getModelBound().getCenter());
            float span = calculateSpan(g);
            for (int i = 0; i < worldLightList.size(); i++) {
                Light light = worldLightList.get(i);
                if (light instanceof AmbientLight) {
                    ambLightColor.addLocal(light.getColor());
                } else if (light instanceof DirectionalLight) {
                    lightList.add(light);
                } else if (light instanceof PointLight) {
                    PointLight pl = (PointLight) light;
                    Vector3f lightPos = pl.getPosition();
                    float radius = pl.getRadius();
                    float dist = lightPos.distance(center);
                    if (radius + span >= dist) {
                        lightList.add(light);
                    }
                } else if (light instanceof SpotLight) {
                    if (this.getParam("EnableSpotlights") != null && (Boolean) this.getParam("EnableSpotlights").getValue() == false) {
                        continue;
                    }
                    SpotLight sl = (SpotLight) light;
                    Vector3f lightPos = sl.getPosition();
                    float range = sl.getSpotRange();
                    Vector3f dir = center.subtract(lightPos);
                    float dist = dir.length();
                    // If the geometry is within distance of the spot light.
                    if (range + span >= dist) {
                        // If it's within "side distance" of the spot light.
                        Vector3f sDir = sl.getDirection();
                        float cosAngle = sDir.dot(dir);
                        if(cosAngle <= 0){
                            continue;
                        }
                        float cosOuter = sl.getPackedAngleCos() - (int)sl.getPackedAngleCos();
                        if (cosAngle > cosOuter) {
                            lightList.add(light);
                        }
                    }
                } // if spotlight
            } // for each light
        } else {
            for (int i = 0; i < worldLightList.size(); i++) {
                Light light = worldLightList.get(i);
                if (light instanceof AmbientLight) {
                    ambLightColor.addLocal(light.getColor());
                } else {
                    lightList.add(light);
                }
            }
        }

        numLights = lightList.size();
//        final int arraySize = Math.max(numLights, 4); // Intel GMA bug
//        this.getMaterialDef().addMaterialParam(VarType.Int, "NumLights", arraySize, null);
        this.setInt("NumLights", numLights);

        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        Uniform lightDir = shader.getUniform("g_LightDirection");

        lightColor.setVector4Length(numLights);
        lightPos.setVector4Length(numLights);
        lightDir.setVector4Length(numLights);

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");
        ambLightColor.a = 1.0f;
        ambientColor.setValue(VarType.Vector4, ambLightColor);

        for (int i = 0; i < numLights; i++) {
            Light l = lightList.get(i);
            ColorRGBA color = l.getColor();
            lightColor.setVector4InArray(color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    l.getType().getId(),
                    i);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, i);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, i);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();

                    lightPos.setVector4InArray(pos2.getX(), pos2.getY(), pos2.getZ(), invRange, i);
                    lightDir.setVector4InArray(dir2.getX(), dir2.getY(), dir2.getZ(), spotAngleCos, i);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }

        lightList.clear();
    }

    public static boolean isCullLights() {
        return cullLights;
    }

    public static void setCullLights(boolean cullLights) {
        MaterialSP.cullLights = cullLights;
    }
}
