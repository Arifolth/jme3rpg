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

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D m_ColorMap;
uniform float m_Metallic;
uniform float m_Roughness;
uniform vec4 m_Specular;

varying vec2 texCoord;

void main() {
    // Sample the color from the scrolling texture
    vec4 baseColor = texture2D(m_ColorMap, texCoord);

    // For simplicity, we output the base color directly.
    // You can extend this shader to add lighting, reflections, or other PBR effects.

    // Apply a simple tint to simulate brass metallic look
    vec3 brassTint = vec3(1.0, 0.85, 0.5);
    vec3 finalColor = baseColor.rgb * brassTint;

    gl_FragColor = vec4(finalColor, baseColor.a);
}