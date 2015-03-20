#import "se/jod/biomonkey/assets/shaders/lib/Fog.glsllib"
#ifndef NUM_LIGHTS
  #define NUM_LIGHTS 1
#endif
varying vec3 texCoord;

#ifdef LIGHTING
varying vec3 diffuseLight;
varying vec3 ambientLight;

  #ifdef NORMALMAP
    uniform sampler2D m_NormalMap;
  #endif

#endif

uniform sampler2D m_DiffuseMap;

#ifdef HQ_FADING
uniform sampler2D m_AlphaNoiseMap;
uniform float m_FadeEnd;
uniform float m_FadeRange;
#endif


#if defined(FOG)
varying float vdist;
varying vec3 dir;
#endif

uniform float m_AlphaDiscardThreshold;

const vec3 UnitZ = vec3(0.0,0.0,1.0);

void main() {

    #if defined(HQ_FADING)
    float noise = texture2D(m_AlphaNoiseMap, texCoord.xy).r;
    float fadeVal = clamp((m_FadeEnd - texCoord.z)/m_FadeRange,0.0,1.0);
    if(fadeVal < noise){
        discard;
    }
    #endif

    vec4 outColor = texture2D(m_DiffuseMap, texCoord.xy);

    if(outColor.a < m_AlphaDiscardThreshold){
        discard;
    }
    #ifdef LIGHTING
    vec3 light = diffuseLight;
      #ifdef NORMALMAP
      vec3 tNorm = (2.0*texture2D(m_NormalMap,texCoord.st).rgb - 1.0);
      light *= dot(tNorm,UnitZ);
      #endif
      outColor.rgb *= (ambientLight*vec3(0.2,0.2,0.2) + diffuseLight);
    #endif
    gl_FragColor = outColor;
    #ifdef FOG
    gl_FragColor.rgb = applyFog(gl_FragColor.rgb,vdist,normalize(dir));
    #endif
}

