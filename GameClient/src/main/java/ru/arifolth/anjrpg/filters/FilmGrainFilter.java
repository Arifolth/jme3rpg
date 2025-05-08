/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture2D;

//Inspired by https://www.shadertoy.com/view/3sGGRz
public class FilmGrainFilter extends Filter {
    private SimpleApplication application;
    private ViewPort viewPort;

    public FilmGrainFilter(SimpleApplication application) {
        super("FilmGrainFilter");
        this.application = application;
    }

    @Override
    protected void initFilter(AssetManager assetManager, RenderManager renderManager, ViewPort vp, int w, int h) {
        this.viewPort = vp;

        material = new Material(assetManager, "Common/MatDefs/Post/FilmGrain.j3md");
        Texture2D tex = processor.getFilterTexture();
        material.setTexture("Texture", tex);
        material.setInt("NumSamples", tex.getImage().getMultiSamples());
        material.setFloat("Time", application.getTimer().getTimeInSeconds());
//        material.setVector2("Resolution", new Vector2f(((ANJRpg)application).getSettings().getWidth(), ((ANJRpg)application).getSettings().getHeight()));
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    protected void preFrame(float tpf) {
        if(material != null)
            material.setFloat("Time", application.getTimer().getTimeInSeconds());
    }
}
