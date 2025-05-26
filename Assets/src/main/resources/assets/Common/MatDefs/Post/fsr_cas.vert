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


in vec3 inPosition;
in vec2 inTexCoord;
out vec2 texCoord;

void main() {
    // Map vertex positions from [0,1] to [-1,1] (NDC)
    gl_Position = vec4(inPosition.xy * 2.0 - 1.0, 0.0, 1.0);
    texCoord = inTexCoord;
}