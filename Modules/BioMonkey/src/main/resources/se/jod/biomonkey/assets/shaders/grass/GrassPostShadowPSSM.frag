#import "Common/ShaderLib/PssmShadows.glsllib"

varying float shadowPosition;
varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

uniform sampler2D m_DiffuseMap;
uniform float m_AlphaDiscardThreshold;
uniform sampler2D m_AlphaNoiseMap;

uniform float m_FadeEnd;
uniform float m_FadeRange;

varying vec3 texCoord;

void main(){   
    float noise = texture2D(m_AlphaNoiseMap, texCoord.st).r;
    float fadeVal = clamp((m_FadeEnd - texCoord.z)/m_FadeRange,0.0,1.0);
    if(fadeVal < noise){
        discard;
    }
    float alpha = texture2D(m_DiffuseMap,texCoord.st).a;
    if(alpha <= m_AlphaDiscardThreshold){
        discard;
    }

   float shadow = 0.0;
    if(shadowPosition < m_Splits.x){
        shadow = GETSHADOW(m_ShadowMap0, projCoord0);
    }else if( shadowPosition <  m_Splits.y){
        shadowBorderScale = 0.5;
        shadow = GETSHADOW(m_ShadowMap1, projCoord1);
    }else if( shadowPosition <  m_Splits.z){
        shadowBorderScale = 0.25;
        shadow = GETSHADOW(m_ShadowMap2, projCoord2);
    }else if( shadowPosition <  m_Splits.w){
        shadowBorderScale = 0.125;
        shadow = GETSHADOW(m_ShadowMap3, projCoord3);
    }
    
    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);

  gl_FragColor = vec4(shadow, shadow, shadow, 1.0);

}