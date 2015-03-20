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
varying vec3 vRayleighColor;
varying vec3 vMieColor;
varying vec3 vDirection;

// Phase function
uniform float m_G;
uniform float m_G2;

// Global time variable
uniform float g_Time;

// Direction of the light.
uniform vec3 m_LightDir;

// Exposure.
uniform float m_Exposure;

// Cloud stuff
uniform sampler2D m_CloudTexture;
uniform sampler2D m_CloudNormal;
uniform vec4 m_SunColor;
uniform vec4 m_AmbientColor;

varying vec3 norm;

void main()
{
    // Calculate the base sky color.
    vec3 skyColor = vec3(0.0);
    if(m_LightDir.y >= -0.3){
    float cosA = dot(m_LightDir, normalize(vDirection));
    float cosA2 = cosA*cosA;
    
    float rayleighPhase = 0.75 * (1.0 + 0.5*cosA2);
    
    float miePhase = 1.5 * ((1.0 - m_G2) / (2.0 + m_G2)) * 
                        (1.0 + cosA2) / pow(1.0 + m_G2 - 2.0 * m_G * cosA, 1.5);
                        
    skyColor = rayleighPhase * vRayleighColor + miePhase * vMieColor;
    
    #if !defined(HDR_ENABLED)
    skyColor = vec3(1.0 - exp(-m_Exposure*skyColor));
    #endif
    }
    // ******************************** clouds *********************
    
    // The base texture coordinate offset.
    vec2 offset = g_Time*0.005*vec2(1,1);
    // Get the fragment position on the projected plane.
    vec3 n = norm;
    vec3 cloudPos = n * 0.2 / n.y;
    
    vec2 tcVar = vec2(cloudPos.x,cloudPos.z);
    float initDens = texture2D(m_CloudTexture,tcVar + offset).r;

    // Calculate cloud density between sun and fragment (approx).
    float density = 0.25*initDens; // Initial value.
    vec3 dirToLight = normalize(m_LightDir - cloudPos);
    vec2 dirToLightTc = normalize(m_LightDir.xz - cloudPos.xz);
    //vec2 dirToLightTc = normalize(vec2(dirToLight.x,dirToLight.z));
    for(int i = 1; i < 4; i++){
        // Ray-trace along dirToLight.
        tcVar += 0.01*dirToLightTc; // 0.01
        float d = texture2D(m_CloudTexture,tcVar + offset).r;
        density += 0.25*d;
    }
    // A value used for cloud transparency.
    float cloudFactor = (1.0 - (1.0 - initDens)*(1.0 - initDens))*clamp(exp(20.0*(n.y - 0.2)),0.0,1.0);
    //float cloudFactor = initDens*clamp(exp(20.0*(n.y - 0.2)),0.0,1.0);

    //vec3 cloudNorm = -(2.0*texture2D(m_CloudNormal, vec2(cloudPos.x,cloudPos.z) + offset).rbg-1.0);
    //cloudNorm.x = -cloudNorm.x;
    //float colMod = 0.15*(1.0 - clamp(dot(-dirToLight,cloudNorm),0.0,1.0)) + 0.85;
    vec3 cloudColor = (m_AmbientColor.rgb*0.4 + m_SunColor.rgb)*(0.4*(1.0 - density) + 0.6);//*colMod*0.9;
    
    float blend = 1.0;
    if(m_LightDir.y <= -0.1){
        // From 1.0 at -0.1 to 0 at -0.2.
        blend = clamp(1.0 + (m_LightDir.y + 0.1)*10.0,0.0,1.0);
    }
    
    skyColor = mix(skyColor, cloudColor, cloudFactor);
    gl_FragColor = vec4(skyColor,max(blend,cloudFactor));
}