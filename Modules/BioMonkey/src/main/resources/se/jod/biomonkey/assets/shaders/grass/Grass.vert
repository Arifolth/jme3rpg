#ifndef NUM_LIGHTS
  #define NUM_LIGHTS 1
#endif

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ViewMatrix;

uniform vec3 g_CameraPosition;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;

#if !defined(HQ_FADING)
uniform float m_FadeEnd;
uniform float m_FadeRange;
#endif

#ifdef SWAYING
uniform float g_Time;
uniform vec2 m_Wind;
uniform vec3 m_SwayData;
#endif

#ifdef LIGHTING

uniform vec4 g_LightPosition[NUM_LIGHTS];
uniform vec4 g_LightColor[NUM_LIGHTS];
uniform vec4 g_LightDirection[NUM_LIGHTS];

uniform vec4 g_AmbientLightColor;

varying vec3 diffuseLight;
varying vec3 ambientLight;

#endif

#if defined(FOG)
varying float vdist;
varying vec3 dir;
#endif

varying vec3 texCoord;

void main() {
    texCoord = vec3(inTexCoord,1.0);
    vec4 pos = vec4(inPosition,1.0);

    #ifdef SWAYING
    float angle = (g_Time + pos.x*m_SwayData.y) * m_SwayData.x;
    pos.xz += 0.2*m_Wind*inTexCoord.y*sin(angle);
    #endif

    vec4 worldPos = g_WorldMatrix*pos;
    
    #ifdef LIGHTING
    diffuseLight = vec3(0.0,0.0,0.0);
    for(int i = 0; i < NUM_LIGHTS; i++){
        vec3 diffLight = vec3(0.0,0.0,0.0);
        float posLight = step(0.5, g_LightColor[i].w);
        vec3 lightVec = g_LightPosition[i].xyz * sign(posLight - 0.5) - (worldPos.xyz * posLight);
        float lDist = length(lightVec);

        float att = clamp(1.0 - g_LightPosition[i].w * lDist * posLight, 0.0, 1.0);
        lightVec = lightVec / vec3(lDist);
        //Spotlights
        float spotFallOff = 1.0;
        #ifdef ENABLE_SPOTLIGHTS
        if(g_LightDirection[i].w != 0.0){
            vec3 spotdir = normalize(g_LightDirection[i].xyz);
            float curAngleCos = dot(-lightVec, spotdir);    
            float innerAngleCos = floor(g_LightDirection[i].w) * 0.001;
            float outerAngleCos = fract(g_LightDirection[i].w);
            float innerMinusOuter = innerAngleCos - outerAngleCos;
            spotFallOff = clamp((curAngleCos - outerAngleCos) / innerMinusOuter, 0.0, 1.0);
        }
        #endif
        vec3 wNorm = vec3(g_WorldMatrix*vec4(inNormal,0.0));
        float NdotL = max(0.0,dot(lightVec,wNorm));
        diffLight = g_LightColor[i].rgb*(att*spotFallOff);//*NdotL;
        diffuseLight += diffLight;
    }
    ambientLight = vec3(0.2, 0.2, 0.2)*g_AmbientLightColor.rgb;
    #endif

    // Fading
    vec2 CtoP = worldPos.xz - g_CameraPosition.xz;
    float dist = length(CtoP);
    //vec4 wvPos = g_ViewMatrix*worldPos;
    //float dist = -wvPos.z;
    
    #if !defined(HQ_FADING)
    float fadeVal = clamp((m_FadeEnd - dist)/m_FadeRange,0.0,1.0);
    pos.y *= fadeVal;
    #else
    texCoord.z = dist;
    #endif

    #if defined(FOG)
    vec3 ray = worldPos.xyz - g_CameraPosition;
    vdist = length(ray);
    dir = ray/vdist; // cam -> vert
    #endif

    gl_Position = g_WorldViewProjectionMatrix * pos;
}
