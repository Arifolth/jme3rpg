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

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture2D;

//Inspired by https://www.shadertoy.com/view/DdGyRd
//
//Why there is no "Time" material parameter anymore:
//jME3 binds MaterialParameters to GLSL uniforms named m_<Name> and
//WorldParameters to g_<Name> (see MatParam.getPrefixedName() and
//Material.updateShaderMaterialParameter()). The old filter pushed
//material.setFloat("Time", ...) every frame, but the shader declared an
//UNPREFIXED "uniform float Time;", which is never written by the engine -
//it stayed 0.0 forever and the grain stuck to the screen.
//The shader now reads the engine-managed g_Time world parameter
//(WorldParameters { Time } in FilmGrain.j3md), which jME3 refreshes
//automatically every frame, so no per-frame Java updates are needed.
//
//Tone preservation note: the shader adds zero-mean grain and nothing else.
//The old "film look" chain (desaturation, brightness -0.1, contrast 0.88,
//gamma 1/1.43) has been removed - it lifted dark scenes by ~+20/255 gray
//and doubled the visible noise, which read as a milky veil over the picture.
//
//Flicker note (removes the "horrible screen flicker" of the previous build):
//that build re-rolled the whole grain pattern on every rendered GPU frame
//(millisecond-resolution seed) with a peak amplitude of +/-0.12. Two
//consequences: (1) the amplitude itself was a full-screen strobe, and
//(2) on an uncapped frame rate the monitor scanout stitched several different
//noise frames into a single refresh, which read as a hard flicker front
//crawling from top to bottom. Fixes: the seed now advances at a fixed
//GrainRate (Hz) so the pattern re-rolls IN PLACE - with the default 60 a tear
//line shows identical noise on both of its sides and becomes invisible - and
//the default strength is lowered to a Lords of the Fallen-like 0.05.
//
//Coverage note (v4 - fixes "noise only on the grass"): the filter sits before
//GammaCorrection, so it receives tone-mapped but still LINEAR colors. The old
//weight curve was calibrated for perceptual values: on linear input everything
//darker than linear 0.03 (roughly sRGB 48/255) got zero grain and bright areas
//swallowed the fixed offset - only linear midtones (the lit grass) showed
//noise. The shader now encodes to perceptual space, grains there, and decodes
//back; the pow round trip is exact, so tone/color/sharpness still pass 1:1.
//Also adds GrainSize (grain cell size in pixels; 1 = the current 1-px grain).
public class FilmGrainFilter extends Filter {

    /**
     * Peak grain amplitude: pixels are shifted by up to +/-strength around
     * the original color. 0.03 = gentle, 0.05 = Lords of the Fallen-like
     * default, 0.08 = sharp and aggressive, 0.12+ = full-screen strobe (the
     * flickering value of the previous build).
     */
    public static final float DEFAULT_STRENGTH = 0.072f;

    /**
     * 0.0 = pure monochrome film grain (the Lords of the Fallen look),
     * 1.0 = full RGB confetti. 0.5 keeps the filmic feel with a visible
     * colored sparkle, which is the shipped default.
     */
    public static final float DEFAULT_CONFETTI = 0.6f;

    /**
     * How many times per second the whole grain pattern re-rolls (Hz).
     * 60 matches a 60 Hz monitor refresh and behaves like white noise on an
     * old TV; 30 halves the flicker energy while the grains still jump like
     * mad. Values above the monitor refresh rate only burn GPU time, values
     * below ~24 start to look like stepping frames. Must be > 0 (the shader
     * clamps internally to >= 1).
     */
    public static final float DEFAULT_GRAIN_RATE = 60.0f;

    /**
     * Grain cell size in pixels. 1.0 = the classic one-pixel grain (each
     * pixel gets its own random value); 2.0 = 2x2 blocks share one value
     * (chunkier, calmer flicker); 3.0+ = retro/film-like coarse grain.
     * Must be >= 1 (the shader clamps internally).
     */
    public static final float DEFAULT_GRAIN_SIZE = 1.0f;

    private SimpleApplication application;
    private float strength = DEFAULT_STRENGTH;
    private float confetti = DEFAULT_CONFETTI;
    private float grainRate = DEFAULT_GRAIN_RATE;
    private float grainSize = DEFAULT_GRAIN_SIZE;

    public FilmGrainFilter(SimpleApplication application) {
        super("FilmGrainFilter");
        this.application = application;
    }

    @Override
    protected void initFilter(AssetManager assetManager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(assetManager, "Common/MatDefs/Post/FilmGrain.j3md");
        Texture2D tex = processor.getFilterTexture();
        material.setTexture("Texture", tex);
        material.setInt("NumSamples", tex.getImage().getMultiSamples());
        material.setFloat("Strength", strength);
        material.setFloat("Confetti", confetti);
        material.setFloat("GrainRate", grainRate);
        material.setFloat("GrainSize", grainSize);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    public float getStrength() {
        return strength;
    }

    /**
     * Grain intensity: pixels are shifted by up to +/-(strength) around the
     * original color. 0 disables the grain, 0.05 matches the Lords of the
     * Fallen grain, 0.08 is the sharp/aggressive end, 0.12+ strobes. Safe to
     * call at any time (also before the filter is initialized).
     */
    public void setStrength(float strength) {
        this.strength = strength;
        if (material != null) {
            material.setFloat("Strength", strength);
        }
    }

    public float getConfetti() {
        return confetti;
    }

    /**
     * 0.0 = monochrome grain, 1.0 = full RGB confetti, 0.5 = filmic mix.
     * Safe to call at any time (also before the filter is initialized).
     */
    public void setConfetti(float confetti) {
        this.confetti = confetti;
        if (material != null) {
            material.setFloat("Confetti", confetti);
        }
    }

    public float getGrainRate() {
        return grainRate;
    }

    /**
     * Pattern re-roll rate in Hz (see {@link #DEFAULT_GRAIN_RATE}). Controls
     * the flicker/jump trade-off: 60 = old-TV chaos on a 60 Hz monitor,
     * 30 = calmer but still jumping. Safe to call at any time (also before
     * the filter is initialized).
     */
    public void setGrainRate(float grainRate) {
        this.grainRate = grainRate;
        if (material != null) {
            material.setFloat("GrainRate", grainRate);
        }
    }

    public float getGrainSize() {
        return grainSize;
    }

    /**
     * Grain cell size in pixels (see {@link #DEFAULT_GRAIN_SIZE}). 1 = the
     * current one-pixel grain, 2 = chunkier 2x2 blocks that also flicker
     * calmer. Safe to call at any time (also before the filter is
     * initialized).
     */
    public void setGrainSize(float grainSize) {
        this.grainSize = grainSize;
        if (material != null) {
            material.setFloat("GrainSize", grainSize);
        }
    }
}
