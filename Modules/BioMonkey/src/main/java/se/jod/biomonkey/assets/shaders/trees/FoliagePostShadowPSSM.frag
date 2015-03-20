#import "Common/ShaderLib/PssmShadows.glsllib"

varying float shadowPosition;
varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

uniform sampler2D m_ColorMap;
uniform float m_AlphaCutOff;
varying vec2 texCoord;

void main(){   

    float alpha = texture2D(m_ColorMap,texCoord).a;
    if(alpha<=m_AlphaCutOff){
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
