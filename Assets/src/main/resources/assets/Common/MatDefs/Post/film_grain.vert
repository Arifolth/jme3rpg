/*
This work is a derivative of the shader originally created by terchapone on shadertoy: https://www.shadertoy.com/view/3sGGRz,
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

in vec3 inPosition;
in vec2 inTexCoord;
out vec2 texCoord;

void main() {
    // Map vertex positions from [0,1] to [-1,1] (NDC)
    gl_Position = vec4(inPosition.xy * 2.0 - 1.0, 0.0, 1.0);
    texCoord = inTexCoord;
}