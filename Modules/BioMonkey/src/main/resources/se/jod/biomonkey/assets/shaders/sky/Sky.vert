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
uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;

varying vec3 vRayleighColor;
varying vec3 vMieColor;
varying vec3 vDirection;

// Direction of the light.
uniform vec3 m_LightDir;

uniform vec3 m_CameraPos;
uniform vec3 m_InvWaveLength;
uniform float m_InnerRadius;


uniform float  m_KrESun; // Kr * ESun
uniform float  m_KmESun; // Km * ESun
uniform float  m_Kr4PI;  // Kr * 4 * PI
uniform float  m_Km4PI;  // Km * 4 * PI

uniform float m_Scale;               // 1 / (outerRadius - innerRadius)
uniform float m_ScaleOverScaleDepth; // Scale / ScaleDepth
uniform float m_ScaleDepth;

// Number of samples
uniform int   m_NumberOfSamples;
uniform float m_Samples;

varying vec3 norm;

// A function O'Neil use to scale attenuation values. This broham uses the same 
// attenuation for Mie and Rayleigh scattering, but it is weighted by a constant
// "m_ScaleDepth" representing the (relative) height where the average density
// of the atmosphere is found. At least this is how I get it. Works pretty well, 
// apparently.
//
// The constants are based on the ratio between outer/inner atmosphere radius, 
// so don't change that.
float scale(float cosA)
{
	float x = 1.0 - cosA;
	return m_ScaleDepth * exp(-0.00287 + x*(0.459 + x*(3.83 + x*(-6.80 + x*5.25))));
}

void main()
{
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition,1.0);
    //gl_Position.z = gl_Position.w;

    vec3 v3Pos = vec3(inPosition);
    float len = length(v3Pos); // Adding dome radius later..
    v3Pos = v3Pos / len;
    norm = v3Pos;

    if(m_LightDir.y >= -0.3){
    
    v3Pos.y += m_InnerRadius;
    
    
    vec3 v3Ray = v3Pos - m_CameraPos;
	
    float fFar = length(v3Ray);
    v3Ray = v3Ray / fFar;
    
    // Calculate the ray's starting position, then calculate its scattering offset
    vec3 v3Start = m_CameraPos;
    float fHeight = m_CameraPos.y;
    float fStartAngle = dot(v3Ray, v3Start) / fHeight;

    
    float fDepth = exp(m_ScaleOverScaleDepth * (m_InnerRadius - m_CameraPos.y));
    float fStartOffset = fDepth * scale(fStartAngle);

    // Init loop variables
    float fSampleLength = fFar / m_Samples;
    float fScaledLength = fSampleLength * m_Scale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;
	
    // Loop the ray
    vec3 color = vec3(0.0,0.0,0.0);

    for (int i = 0; i < m_NumberOfSamples; i++)
    {
    	float fHeight = length(v3SamplePoint);
    	float fDepth = exp(m_ScaleOverScaleDepth * (m_InnerRadius-fHeight));
        
        float fLightAngle = dot(m_LightDir, v3SamplePoint) / fHeight;
	float fCameraAngle = dot(v3Ray, v3SamplePoint) / fHeight;
		
	float fScatter = (fStartOffset + fDepth*(scale(fLightAngle) - scale(fCameraAngle)));
	vec3 v3Attenuate = exp(-fScatter * (m_InvWaveLength * m_Kr4PI + m_Km4PI));
		
	// Accumulate color
	v3Attenuate *= (fDepth * fScaledLength);
	color += v3Attenuate;
        
	// Next sample point
	v3SamplePoint += v3SampleRay;
    }

    // Outputs. Note mie and rayleigh scattering uses the same attenuation value.
    vRayleighColor = color * (m_InvWaveLength * m_KrESun); // Rayleigh scattering
    vMieColor      = color * m_KmESun; // Mie scattering
    vDirection     = m_CameraPos - v3Pos;
    }
}