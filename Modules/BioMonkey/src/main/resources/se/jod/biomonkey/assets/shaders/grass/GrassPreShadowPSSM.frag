varying vec3 texCoord;

//#ifdef HQ_FADING
//uniform sampler2D m_AlphaNoiseMap;
//uniform float m_FadeEnd;
//uniform float m_FadeRange;
//#endif

uniform sampler2D m_DiffuseMap;
uniform float m_AlphaDiscardThreshold;

void main(){

    //#if defined(HQ_FADING)
    //float noise = texture2D(m_AlphaNoiseMap, texCoord.st).r;
    //float fadeVal = clamp((m_FadeEnd - texCoord.z)/m_FadeRange,0.0,1.0);
    //if(fadeVal < noise){
    //    discard;
    //}
    
    #endif

    if (texture2D(m_DiffuseMap, texCoord.st).a <= m_AlphaDiscardThreshold){
        discard;
    }
   gl_FragColor = vec4(1.0);
}