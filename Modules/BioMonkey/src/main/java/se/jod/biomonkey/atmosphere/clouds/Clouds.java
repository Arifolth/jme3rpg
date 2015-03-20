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
package se.jod.biomonkey.atmosphere.clouds;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.atmosphere.sky.Sky;

/**
 * This class enables and manages clouds. Temporary.
 * 
 * @author Andreas
 */
public class Clouds {
        
    
    public Clouds(Sky sky){
        
        AssetManager am = EcoManager.getInstance().getApp().getAssetManager();
        
//        ImprovedNoise noise = new ImprovedNoise();
//        
//        noise.setScale(8);
//        FractalSum fractalSum = new FractalSum();
//        fractalSum.addBasis(noise);
//        fractalSum.setOctaves(8);
//        fractalSum.setFrequency(1.0f);
//        fractalSum.setAmplitude(0.5f);
//        fractalSum.setLacunarity(2.12f);
//        
//        int w = 2, h = 2;
//        ByteBuffer bb = BufferUtils.createByteBuffer(w*h*2);
//        Image noiseImg = new Image(Image.Format.Luminance16F,w,h,bb);
//        ImageRaster raster = new DefaultImageRaster(noiseImg,0);
//        for(int j = 0; j < h; j++){
//            for(int i = 0; i < w; i++){
//                float ii = i / (float)w;
//                float jj = j / (float)h;
//                double v1 = fractalSum.value(ii, jj, 0);
//                double v2 = fractalSum.value(ii - 1, jj, 0);
//                double v3 = fractalSum.value(ii - 1, jj - 1, 0);
//                double v4 = fractalSum.value(ii, jj - 1, 0);
//                double val = v1*(1.0 - ii)*(1.0 - jj) + v2*ii*(1.0 - jj) + v3*ii*jj + v4*(1.0 - ii)*jj;
//                
//                
//                val = val*0.5 + 0.5;
//                val = Math.pow(val, 0.25);
//                float v = FastMath.clamp((float) val,0.001f,1f);
//                raster.setPixel(i, j, new ColorRGBA(v,v,v,1.0f));
//            }
//        }
//        bb.rewind();
        
//        Texture cloudTex = new Texture2D(noiseImg);
        Texture cloudTex = am.loadTexture("se/jod/biomonkey/assets/textures/Clouds_L.png");
        cloudTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        cloudTex.setWrap(Texture.WrapMode.Repeat);
        
        sky.getSkyDome().getMaterial().setTexture("CloudTexture", cloudTex);
    }

}
