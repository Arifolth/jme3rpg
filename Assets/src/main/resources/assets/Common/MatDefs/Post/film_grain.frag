/*
This work is a derivative of the shader originally created by willeves07 on shadertoy: https://www.shadertoy.com/view/DdGyRd,
Modifications and adaptations for jMonkeyEngine 3 compatibility by Alexander Nilov, 2025.
Licensed under the GNU GPLv3 License.
*/

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

/*
 * jME3 uniform naming rules (this is what made the port's noise static):
 *   MaterialParameters of the .j3md bind to GLSL uniforms named  m_<Name>
 *   WorldParameters    of the .j3md bind to GLSL uniforms named  g_<Name>
 * There is no fallback: an unprefixed "uniform float Time;" is NEVER written
 * by the engine, so an early port kept Time frozen at 0.0 and the grain stuck
 * to the screen. Engine filters (e.g. Common/MatDefs/Post/Fog.frag) all use
 * the m_ prefix for the same reason.
 */

uniform sampler2D m_Texture; // MaterialParameters { Texture2D Texture }
uniform float m_Strength;    // MaterialParameters { Float Strength }  - peak grain amplitude, 0 = off
uniform float m_Confetti;    // MaterialParameters { Float Confetti }  - 0 = monochrome film grain, 1 = full RGB confetti
uniform float m_GrainRate;   // MaterialParameters { Float GrainRate } - pattern re-roll rate in Hz (how many times per second the whole grain re-rolls)
uniform float m_GrainSize;   // MaterialParameters { Float GrainSize } - grain cell size in pixels (1 = one pixel per grain, 2 = chunky 2x2 blocks, ...)

// WorldParameters { Time } in FilmGrain.j3md: jME3 feeds this every frame with
// timer.getTimeInSeconds() (Material.render -> RenderManager.updateUniformBindings),
// even for post-filter materials rendered by FilterPostProcessor.
uniform float g_Time;

in vec2 texCoord;
out vec4 fragColor;

// ------------------------------------------------------------------
// PCG3D hash (Jarzynski & Olano, "Hash Functions for GPU Rendering").
//   - every pixel gets an independent value, so the grain is exactly one
//     pixel wide; no clusters, no stripes, at any resolution;
//   - integer math: no sin() precision decay over long play sessions,
//     identical results on every GPU/driver.
// ------------------------------------------------------------------
uvec3 pcg3d(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    v ^= v >> 16u;
    v = v * 1664525u + 1013904223u;
    v ^= v >> 16u;
    return v;
}

vec3 grainNoise3(vec2 pixel, float seedFrame) {
    uvec3 h = pcg3d(uvec3(uint(pixel.x), uint(pixel.y), uint(seedFrame)));
    return vec3(h) * (1.0 / 4294967295.0);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// Perceptual gamma used ONLY for the encode -> grain -> decode round trip in
// main(). 2.2 approximates the display's sRGB response; it does NOT have to
// match the downstream GammaCorrectionFilter (whose fade values 0.6..0.84 are
// a stylistic lift): pow(pow(x, a), 1/a) is an exact round trip, so the base
// image passes through 1:1 - 2.2 only decides in WHICH space the noise is
// distributed, namely the eye-uniform one.
const float PERCEPTUAL_GAMMA = 2.2;

void main() {
    // Sample scene texture
    vec4 color = texture(m_Texture, texCoord);

    // COVERAGE FIX ("grain only on the grass"): this filter sits BEFORE
    // GammaCorrection in the chain, so m_Texture holds tone-mapped but still
    // LINEAR values. Weighting the grain directly on those was wrong:
    //   - linear 0.03 covers everything darker than ~sRGB 48/255, so trees,
    //     dark mountains and shadows got ZERO grain (black-floor fade);
    //   - on bright skies a fixed linear offset vanishes after gamma
    //     encoding (highlight fade + perceptually tiny relative shift);
    //   - only linear midtones - the lit grass - kept the grain.
    // Encode to perceptual space first, then grain, then decode: the weight
    // thresholds below finally mean what they say, and the grain becomes
    // uniform across the whole screen.
    vec3 perceptual = pow(max(color.rgb, vec3(0.0)), vec3(1.0 / PERCEPTUAL_GAMMA));

    // The whole pattern re-rolls m_GrainRate times per second instead of once
    // per rendered GPU frame. Rationale (this removed the "horrible screen
    // flicker"): with an uncapped frame rate the old millisecond-resolution
    // seed produced a brand-new pattern for EVERY rendered frame, and the
    // monitor scanout then stitched several different noise frames into a
    // single refresh - visible as a hard flicker front crawling from top to
    // bottom. Holding the pattern for 1/m_GrainRate seconds makes a tear line
    // show the SAME noise on both of its sides (invisible), while the grain
    // still jumps chaotically like white noise on an old TV. The seed has no
    // spatial time term, so the pattern always re-rolls IN PLACE - it can
    // never scroll, crawl or drift. mod(..., 65536.0) keeps the uint cast
    // small and exact even after many hours of play.
    float rate = max(m_GrainRate, 1.0);
    float seedFrame = mod(floor(g_Time * rate), 65536.0);

    // Grain cells: m_GrainSize = 1 gives the classic one-pixel grain; larger
    // values group pixels into size x size blocks sharing one noise value -
    // chunkier grain that also flickers calmer (fewer independent samples
    // per screen area). gl_FragCoord is in framebuffer pixels, so cell size
    // stays exact at any resolution.
    vec2 cell = floor(gl_FragCoord.xy / max(m_GrainSize, 1.0));
    vec3 noise = grainNoise3(cell, seedFrame);

    // Monochrome film grain with optional RGB confetti on top. All channels
    // share the same seed, so nothing crawls: the whole pattern re-rolls at
    // once. m_Confetti = 0 reproduces the pure monochrome Lords of the Fallen
    // look, 1.0 gives full RGB confetti.
    vec3 grain = mix(vec3(noise.r), noise, m_Confetti);

    // Film response, now in PERCEPTUAL units: grain reads strongest in
    // shadows and midtones, fades out of blown-out highlights (no sparkling
    // sky) and keeps the deepest blacks clean (no gray noise floor on dark
    // scenes).
    float lum = luminance(perceptual);
    float weight = (1.0 - smoothstep(0.35, 0.95, lum) * 0.70)
                 * smoothstep(0.0, 0.03, lum);

    // Zero-mean additive grain: the average tone, contrast, color and
    // sharpness of the scene pass through this filter 1:1. No desaturation,
    // no brightness/contrast reduction, no gamma tricks - those were the
    // "milky picture" culprits of an earlier iteration.
    vec3 grainEffect = (grain - 0.5) * 2.0 * m_Strength * weight;

    // Decode perceptual -> linear for the GammaCorrection filter downstream.
    fragColor = vec4(pow(max(perceptual + grainEffect, vec3(0.0)), vec3(PERCEPTUAL_GAMMA)), color.a);
}
