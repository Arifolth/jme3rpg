/*
This work is a derivative of the shader originally created by goingdigital on ShaderToy:
https://www.shadertoy.com/view/ftsXzM,
which itself is based on the original AMD shader (FidelityFX SDK https://gpuopen.com/fidelityfx-cas/)
licensed under the MIT License (see below).

Modifications and adaptations for jMonkeyEngine 3 compatibility by Alexander Nilov, 2025.
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
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

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


in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Texture;
uniform vec2 Resolution;
uniform float Sharpness;

precision highp float;

const float FSR_RCAS_LIMIT = 0.25 - (1.0/16.0);
const float MAX_LUMINANCE = 4.0;

vec3 CASPass(vec2 uv) {
    vec2 pixelSize = 1.0 / Resolution;

    vec3 c  = texture(Texture, uv).rgb;
    vec3 b  = texture(Texture, uv + vec2( 0.0, -pixelSize.y)).rgb;
    vec3 d  = texture(Texture, uv + vec2(-pixelSize.x,  0.0)).rgb;
    vec3 f  = texture(Texture, uv + vec2( pixelSize.x,  0.0)).rgb;
    vec3 h  = texture(Texture, uv + vec2( 0.0,  pixelSize.y)).rgb;

    // Tonemapping
    vec3 tm = vec3(1.0) - exp(-c * 0.5);
    vec3 tm_b = vec3(1.0) - exp(-b * 0.5);
    vec3 tm_d = vec3(1.0) - exp(-d * 0.5);
    vec3 tm_f = vec3(1.0) - exp(-f * 0.5);
    vec3 tm_h = vec3(1.0) - exp(-h * 0.5);

    // Luma
    float bL = dot(tm_b, vec3(0.2126, 0.7152, 0.0722));
    float dL = dot(tm_d, vec3(0.2126, 0.7152, 0.0722));
    float eL = dot(tm,   vec3(0.2126, 0.7152, 0.0722));
    float fL = dot(tm_f, vec3(0.2126, 0.7152, 0.0722));
    float hL = dot(tm_h, vec3(0.2126, 0.7152, 0.0722));

    float minL = min(min(min(dL, eL), min(fL, bL)), hL);
    float maxL = max(max(max(dL, eL), max(fL, bL)), hL);

    // Adaptive luminance
    float avgLuminance = (bL + dL + eL + fL + hL) * 0.2;
    float maxAdaptedL = min(maxL, avgLuminance * 2.0);
    maxL = mix(maxL, maxAdaptedL, smoothstep(1.0, 3.0, avgLuminance));

    // Sharpness
    float con = 1.0 / (Sharpness * sqrt(2.0) * (0.5 + 1.0/(avgLuminance + 1.0)) + 2.0);
    float range = max(maxL - minL, 0.0001);
    float scale = con / range;

    // Weights
    float wb = clamp((bL - eL) * scale + 0.5, 0.0, 1.0);
    float wd = clamp((dL - eL) * scale + 0.5, 0.0, 1.0);
    float wf = clamp((fL - eL) * scale + 0.5, 0.0, 1.0);
    float wh = clamp((hL - eL) * scale + 0.5, 0.0, 1.0);

    // Weight limiting
    float limit = FSR_RCAS_LIMIT * (1.0 - smoothstep(1.0, MAX_LUMINANCE, avgLuminance));
    float totalWeight = wb + wd + wf + wh;
    if (totalWeight > limit) {
        float ratio = limit / totalWeight;
        wb *= ratio;
        wd *= ratio;
        wf *= ratio;
        wh *= ratio;
    }

    // Blend
    float weightSum = wb + wd + wf + wh + 1.0;
    vec3 result = (wb*b + wd*d + c + wf*f + wh*h) / weightSum;

    return mix(c, result, smoothstep(2.0, MAX_LUMINANCE, avgLuminance));
}

void main() {
    vec2 uv = clamp(texCoord, vec2(0.001), vec2(0.999));
    vec3 color = CASPass(uv);
    fragColor = vec4(color, 1.0);
}