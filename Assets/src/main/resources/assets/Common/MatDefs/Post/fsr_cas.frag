/*
This file is part of the FSR 1 (RCAS) integration for ANJRpg.

This work is a derivative of AMD FidelityFX SDK FSR 1 shader code:
ffx_fsr1.h (FSR_RCAS_F, 32-bit float path) from
https://github.com/GPUOpen-Effects/FidelityFX-FSR,
which is licensed under the MIT License (see below).

Port notes (deviations from the AMD reference):
 - The AMD reference computes the sharpness constant on the CPU
   (FsrRcasCon: con.x = exp2(-sharpness), where its "sharpness" input is a
   stop count, 0.0 := maximum sharpness). This port instead takes a UI
   range uniform m_Sharpness in [0, 1], where 0.0 := no sharpening (a pure
   pass-through: lobe 0) and 1.0 := the reference maximum strength
   (FSR_RCAS_LIMIT). The lobe is scaled linearly by the uniform, which
   keeps the on/off end of the FSRCASFilter.setSharpness range exact and
   makes intermediate values a plain percentage of the reference maximum.
 - The reference fetches the 5 taps with integer-coordinate imageLoad
   calls (FsrRcasLoadF). This port fetches the same texels with plain
   texture() lookups at exact texel centers (GLSL 3.3, like fsr_easu.frag);
   the Clamp wrap mode installed by FSRCASFilter.initFilter makes the
   out-of-image border taps return the edge texels.
 - ARcpF1 (exact reciprocal in the reference) is guarded against a zero
   divisor (rcp(0) := 0), so an all-black or all-white neighborhood cannot
   produce NaN. Unlike fsr_easu.frag's rcp the guard is x != 0 rather than
   x > 0, because the hitMax limiter divides by the NEGATIVE value
   4*mn4 - 4 whenever the ring minimum is below 1.0 - and that sign is
   essential to the lobe.
 - FsrRcasInputF (optional per-tap input transform) is the identity, as
   in AMD's own sample: the filter-chain texture already carries the
   working-space colors the FSR 1 chain operates on.
 - The optional FSR_RCAS_DENOISE noise-rejection path is not compiled in:
   AMD recommends applying film grain AFTER RCAS instead (which this game
   does - FilmGrainFilter follows the FSR pair in the chain), so the
   noise-dependent lobe damping is unnecessary.
 - The core RCAS math - luma, ring min/max, the hitMin/hitMax limiter
   construction, the clamped negative lobe and the resolve - is ported
   verbatim from the reference.

Modifications and adaptations for jMonkeyEngine 3 compatibility by Alexander Nilov, 2026.
Licensed under the GNU GPLv3 License.

The GPLv3 license applies to modifications and additions made by Alexander Nilov.
The original code remains under the MIT License.
*/

/*
This file is part of the FidelityFX SDK.

Copyright (C) 2024 Advanced Micro Devices, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
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


in vec2 texCoord;
out vec4 fragColor;

// jME3 uniform naming rules (see film_grain.frag for the in-repo
// documentation): MaterialParameters of the .j3md bind to GLSL uniforms
// named m_<Name>; unprefixed uniforms are never written by the engine.
//   MaterialParameters { Texture2D Texture; Vector2 Resolution; Float Sharpness }
uniform sampler2D m_Texture;   // the image being sharpened (the EASU output)
uniform vec2 m_Resolution;     // the resolution of m_Texture (display res while upscaling)
uniform float m_Sharpness;     // 0.0 := pass-through, 1.0 := FSR 1 reference maximum

precision highp float;

// This is set at the limit of providing unnatural results for sharpening.
const float FSR_RCAS_LIMIT = 0.25 - (1.0 / 16.0);

// Exact reciprocal guarded against a zero divisor. NOTE: unlike
// fsr_easu.frag's rcp (which only sees non-negative distances and can
// guard with x > 0), RCAS also divides by NEGATIVE values: the hitMax
// limiter's denominator 4*mn4 + peakC.y = 4*mn4 - 4 is negative for any
// ring minimum below 1.0, and the sign of that division is essential to
// the lobe - so the guard must be x != 0, not x > 0 (a zero denominator,
// which AMD's exact ARcpF1 would turn into inf/NaN, maps to 0 here; see
// the port notes in the file header).
float rcp(float x) {
    return x != 0.0 ? 1.0 / x : 0.0;
}

// Fetch one texel by its integer texel coordinate, at the exact texel
// center so that the sampler filtering mode is irrelevant. The Clamp wrap
// installed by FSRCASFilter.initFilter returns the edge texels for the
// out-of-image taps of the border pixels.
vec3 fetchTap(vec2 invSize, vec2 t) {
    return texture(m_Texture, (t + vec2(0.5)) * invSize).rgb;
}

// Guarded per-channel reciprocal for the limiter divisions (see rcp()).
vec3 rcp4(vec3 x) {
    return vec3(rcp(x.r), rcp(x.g), rcp(x.b));
}

//------------------------------------------------------------------------------------------------------------------------------
// RCAS: Reality Contrast Adaptive Sharpening.
// Ported from AMD ffx_fsr1.h, FsrRcasF (32-bit float path). Uses a minimal
// 3x3 pixel neighborhood:
//    b
//  d e f
//    h
// The neighbor "lobe" weight is NEGATIVE (neighbors subtract), which is
// what makes RCAS sharpen: local contrast around 'e' is amplified while
// flat regions stay untouched (the lobe is capped by FSR_RCAS_LIMIT and
// by the per-channel min/max limiters to avoid halos and ringing).
void FsrRcasF(
 out vec3 pix,
 vec2 ip) { // Integer pixel position in the RCAS input/output (1:1 pass).
  vec2 invSize = 1.0 / m_Resolution;

//------------------------------------------------------------------------------------------------------------------------------
  // Algorithm uses minimal 3x3 pixel neighborhood.
  //    b
  //  d e f
  //    h
  vec2 sp = floor(ip);
  vec3 b = fetchTap(invSize, sp + vec2( 0.0, -1.0));
  vec3 d = fetchTap(invSize, sp + vec2(-1.0,  0.0));
  vec3 e = fetchTap(invSize, sp);
  vec3 f = fetchTap(invSize, sp + vec2( 1.0,  0.0));
  vec3 h = fetchTap(invSize, sp + vec2( 0.0,  1.0));
//------------------------------------------------------------------------------------------------------------------------------
  // Luma times 2 (approximate, in 2 FMA/MAD - as the reference). Consumed
  // only by the reference's optional FSR_RCAS_DENOISE noise-rejection path,
  // which is not compiled in (see the port notes); kept for parity with
  // ffx_fsr1.h so that path can be enabled later without re-deriving them
  // (the GLSL compiler eliminates the dead code).
  float bL = b.b * 0.5 + (b.r * 0.5 + b.g);
  float dL = d.b * 0.5 + (d.r * 0.5 + d.g);
  float eL = e.b * 0.5 + (e.r * 0.5 + e.g);
  float fL = f.b * 0.5 + (f.r * 0.5 + f.g);
  float hL = h.b * 0.5 + (h.r * 0.5 + h.g);
//------------------------------------------------------------------------------------------------------------------------------
  // Min and max of ring (per channel).
  vec3 mn4 = min(min(b, d), min(f, h));
  vec3 mx4 = max(max(b, d), max(f, h));
//------------------------------------------------------------------------------------------------------------------------------
  // Immediate constants for peak range.
  const vec2 peakC = vec2(1.0, -1.0 * 4.0);
  // Limiters, these need to be high precision RCPs.
  vec3 hitMin = min(mn4, e) * rcp4(4.0 * mx4);
  vec3 hitMax = (peakC.x - max(mx4, e)) * rcp4(4.0 * mn4 + peakC.y);
  vec3 lobeV = max(-hitMin, hitMax);
  // The lobe is clamped to [-FSR_RCAS_LIMIT, 0] and scaled by the sharpness
  // strength (the reference scales it by the FsrRcasCon constant instead;
  // see the port notes in the file header).
  float lobe = max(-FSR_RCAS_LIMIT, min(max(max(lobeV.r, lobeV.g), lobeV.b), 0.0))
           * clamp(m_Sharpness, 0.0, 1.0);
//------------------------------------------------------------------------------------------------------------------------------
  // Resolve, which needs the medium precision rcp approximation to avoid
  // visible tonality changes (the reference's APrxMedRcpF1; exact here).
  float rcpL = rcp(4.0 * lobe + 1.0);
  pix = (lobe * (b + d + h + f) + e) * rcpL;
}

void main() {
    // Integer pixel position derived from the interpolated full-screen quad
    // texcoord (pixel-center aligned, matching the reference's
    // gl_FragCoord-based integer position but orientation-agnostic) - the
    // same convention as fsr_easu.frag.
    vec2 ip = texCoord * m_Resolution;
    vec3 color;
    FsrRcasF(color, ip);
    fragColor = vec4(color, 1.0);
}
