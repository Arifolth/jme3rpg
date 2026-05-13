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
* Inspired by https://www.shadertoy.com/view/ftsXzM
* */
public class FSRCASFilter extends Filter {
    private float sharpness = 0.8f; // Range 0.0 (soft) to 1.0  (sharp)

    public FSRCASFilter() {
        super("FSR CAS Filter");
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

        int width = vp.getCamera().getWidth();
        int height = vp.getCamera().getHeight();

        material.setVector2("Resolution", new Vector2f(width, height));

        material.setFloat("Sharpness", sharpness);
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
}
