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

uniform sampler2D Texture;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;

// Random functions from old shader
float rand(float co) {
    return fract(sin(co * 91.3458) * 47453.5453);
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float rand(vec3 co) {
    return rand(co.xy + rand(co.z));
}

// Compute luminance for desaturation
float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

uniform float brightness; // around 0.0 means no change, negative to darken, positive to brighten
uniform float contrast;   // 1.0 means no change, <1 to reduce contrast, >1 to increase contrast

vec3 adjustBrightnessContrast(vec3 color, float brightness, float contrast) {
    // Shift color by brightness
    color += brightness;

    // Apply contrast around 0.5 mid-point
    color = (color - 0.5) * contrast + 0.5;

    return clamp(color, 0.0, 1.0);
}

void main() {
    // Normalized pixel coordinates (uv)
    vec2 uv = texCoord; // assuming texCoord in [0,1]

    // Noise strength (adjust for desired grain amount)
    float strength = 0.102;

    // Generate animated noise vector from time and uv
    vec4 noise = vec4(
        rand(vec3(Time * 3.0, uv.x, uv.y)),
        rand(vec3(Time * 1.0, uv.y, uv.x)),
        rand(vec3(Time * 0.3, cos(uv.x), uv.y)),
        1.0
    );

    // Sample scene texture
    vec4 color = texture(Texture, uv);

    // Convert noise to grain effect centered around zero and scaled
    vec3 grainEffect = vec3((noise.r - 0.5) * strength,
        (noise.g - 0.5) * strength,
        (noise.b - 0.5) * strength);

    // Add grain effect to original color (additive)
    vec3 noisyColor = color.rgb + grainEffect;

    // Clamp color to valid range
    noisyColor = clamp(noisyColor, 0.0, 1.0);

    // Slight desaturation for film look
    float lum = luminance(noisyColor);
    vec3 desatColor = mix(noisyColor, vec3(lum), 0.28);

    // Apply tone mapping (Reinhard)
    vec3 toneMapped = desatColor / (desatColor + vec3(1.0));

    // Adjust brightness and contrast
    float brightness = -0.1; // negative to darken slightly
    float contrast = 0.88;    // less than 1 to reduce contrast a bit
    vec3 finalColor = adjustBrightnessContrast(desatColor, brightness, contrast);

    float gamma = 1.43;
    finalColor = pow(finalColor, vec3(1.0 / gamma));

    fragColor = vec4(finalColor, color.a);
}