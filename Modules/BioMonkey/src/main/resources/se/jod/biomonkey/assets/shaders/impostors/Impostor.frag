#import "se/jod/biomonkey/assets/shaders/lib/Fog.glsllib"
#ifndef NUM_LIGHTS
    #define NUM_LIGHTS 1
#endif

uniform vec4 g_LightPosition[NUM_LIGHTS];
uniform vec4 g_LightColor[NUM_LIGHTS];
uniform vec4 g_LightDirection[NUM_LIGHTS];
uniform vec4 g_AmbientLightColor;

varying vec4 texCoords;
varying vec2 noiseTexCoords;
varying vec4 worldPos;

uniform float m_FadeEnd;
uniform float m_FadeRange;
varying float fadeVal;

varying vec3 normal;
varying float angleFract;

uniform float m_AlphaDiscardThreshold;

uniform sampler2D m_ImpostorTexture;
uniform sampler2D m_AlphaNoiseMap;

#ifdef FOG
varying float vdist;
varying vec3 dir;
#endif

const vec3 unitY = vec3(0.0,1.0,0.0);

void main() {

    float noise = texture2D(m_AlphaNoiseMap, noiseTexCoords.xy).r;
        // Reversed because fading starts at a higher value then it ends.
        float fv = clamp((fadeVal - m_FadeEnd)/m_FadeRange,0.0,1.0);
        if(fv < 1.0 - noise){
            discard;
        }

    float interp = step(0.8,angleFract)*(angleFract - 0.8)*5.0;
    
    vec4 outColor = texture2D(m_ImpostorTexture, texCoords.xy);
    vec4 outColor2 = texture2D(m_ImpostorTexture, texCoords.zw);
    gl_FragColor = mix(outColor,outColor2,interp*noise);

    if(gl_FragColor.a < m_AlphaDiscardThreshold){
        discard;
    }

    vec3 normMapNorm = texture2D(m_ImpostorTexture, texCoords.xy + vec2(0.0,0.5)).rgb;
    vec3 normMapNorm2 = texture2D(m_ImpostorTexture, texCoords.zw + vec2(0.0,0.5)).rgb;
    
    normMapNorm = mix(normMapNorm,normMapNorm2,interp);
    
    //unpack
    normMapNorm = (normMapNorm - vec3(0.5))*vec3(2.0);
    
    vec3 norm = normalize(normal);
    vec3 tang = vec3(normal.z,0.0,-normal.x);
    mat3 tbnMat = mat3(tang,unitY,norm);

    vec3 finalNorm = tbnMat*normMapNorm;

    vec3 light = vec3(0.0,0.0,0.0);
    for(int i = 0; i < NUM_LIGHTS; i++){
        
        vec3 diffLight = vec3(0.0,0.0,0.0);
        float posLight = step(0.5, g_LightColor[i].w);
        vec3 lightVec = g_LightPosition[i].xyz * sign(posLight - 0.5) - (worldPos.xyz * posLight);
        float lDist = length(lightVec);

        float att = clamp(1.0 - g_LightPosition[i].w * lDist * posLight, 0.0, 1.0);
        lightVec = lightVec / vec3(lDist);
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
        float NdotL = max(0.0,dot(lightVec,finalNorm));
        diffLight = g_LightColor[i].rgb*(att*spotFallOff)*NdotL;
        light += diffLight;
    }
    light += g_AmbientLightColor.rgb*vec3(0.2,0.2,0.2);
    gl_FragColor.xyz *= light;

    #ifdef FOG
    gl_FragColor.rgb = applyFog(gl_FragColor.rgb,vdist,normalize(dir));
    #endif

    //gl_FragColor = vec4(fv,fv,fv,1.0);
}

