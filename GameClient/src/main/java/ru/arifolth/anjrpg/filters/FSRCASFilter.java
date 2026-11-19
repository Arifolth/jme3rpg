/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.math.Vector2f;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/*
* FSR 1 RCAS (Reality Contrast Adaptive Sharpening) stage, ported with
* fsr_cas.frag from AMD's MIT-licensed FSR 1 reference (ffx_fsr1.h,
* FsrRcasF, 32-bit float path) - the same source as the EASU port in
* FSREASUFilter. Runs IMMEDIATELY after the EASU upscaler in the filter
* chain (FSR 1 pipeline requirement) and sharpens the upscaled image at
* the display resolution.
* */
public class FSRCASFilter extends Filter {
    /**
     * Sharpness strength: 0.0 = exact pass-through (RCAS off), 1.0 = the
     * FSR 1 reference maximum (the FSR_RCAS_LIMIT-clamped negative lobe).
     * Scales the reference lobe linearly; see fsr_cas.frag's port notes
     * for how this maps onto AMD's FsrRcasCon constant.
     */
    private float sharpness = 0.8f;

    /**
     * Display resolution RCAS operates at. When FSR upscaling is active the
     * FPP pass size (w/h of initFilter) is the INTERNAL resolution, while
     * RCAS runs on the upscaled image - so FilterManager injects the display
     * size here. 0 = fall back to the viewport camera (native rendering).
     */
    private int displayWidth, displayHeight;

    private final DisplayRenderPass displayRenderPass = new DisplayRenderPass("FSR CAS display pass");

    public FSRCASFilter() {
        super("FSR CAS Filter");
    }

    /**
     * The display resolution (the size of the image RCAS sharpens). Must be
     * called by the FilterManager when FSR upscaling is enabled, because at
     * add-time the viewport camera may already be resized to the internal
     * resolution by the FPP.
     */
    public void setDisplayResolution(int width, int height) {
        this.displayWidth = width;
        this.displayHeight = height;
        if (material != null) {
            material.setVector2("Resolution", new Vector2f(Math.max(width, 1), Math.max(height, 1)));
        }
    }

    @Override
    protected void initFilter(AssetManager assetManager,
                              RenderManager renderManager,
                              ViewPort vp,
                              int w, int h) {
        material = new Material(assetManager, "Common/MatDefs/Post/FSRCASFilter.j3md");

        Texture2D tex = processor.getFilterTexture();
        // Ensure proper HDR texture handling
        tex.setWrap(Texture.WrapMode.Clamp);
        tex.setMagFilter(Texture.MagFilter.Bilinear);
        tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);

        material.setTexture("Texture", tex);
        material.setInt("NumSamples", tex.getImage().getMultiSamples());

        // RCAS runs on the UPSCALED image, so its sampling step (Resolution)
        // is the display resolution - not the FPP pass size, which is the
        // internal resolution when upscaling is active. Fall back to the
        // viewport camera (correct at native resolution).
        int width = displayWidth > 0 ? displayWidth : vp.getCamera().getWidth();
        int height = displayHeight > 0 ? displayHeight : vp.getCamera().getHeight();

        material.setVector2("Resolution", new Vector2f(width, height));

        material.setFloat("Sharpness", sharpness);

        // When RCAS runs after the FSREASUFilter (FSR 1 pipeline: EASU then
        // RCAS immediately after), its input and output are display-res
        // while the FPP sizes its default pass at the internal resolution.
        // Swap in the display-resolution pass in that case.
        if (displayRenderPass.isActive(w, h, width, height, processor.getDefaultPassTextureFormat())) {
            getRenderFrameBuffer().dispose();
            getRenderedTexture().getImage().dispose();
            setRenderFrameBuffer(displayRenderPass.getFrameBuffer());
            setRenderedTexture(displayRenderPass.getTexture());
        }
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    public void setSharpness(float sharpness) {
        this.sharpness = sharpness;

        if (material != null) {
            material.setFloat("Sharpness", sharpness);
        }
    }

    public float getSharpness() {
        return sharpness;
    }

    @Override
    protected void cleanUpFilter(com.jme3.renderer.Renderer r) {
        // The base Filter.cleanup has already disposed the pair installed in
        // the default pass (the display-res pair when upscaling was active).
        displayRenderPass.release();
    }
}
