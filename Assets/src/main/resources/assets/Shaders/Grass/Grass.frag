varying vec3 texCoord;

#ifdef VERTEX_COLORS
varying vec4 color;
#endif

#ifdef VERTEX_LIGHTING
varying vec3 diffuseLight;
varying vec3 ambientLight;
#endif

uniform sampler2D m_ColorMap;

#ifdef FADE_ENABLED
uniform sampler2D m_AlphaNoiseMap;
#endif


void main() {
    #ifdef FADE_ENABLED
    if(texCoord.z < texture2D(m_AlphaNoiseMap, texCoord.xy).r){
        discard;
    }
    #endif

    vec4 outColor = texture2D(m_ColorMap, texCoord.xy);
    
    if(outColor.a < 0.3){
        discard;
    }

    #ifdef VERTEX_LIGHTING    
    outColor.rgb *= (diffuseLight + ambientLight);
    #endif

    #ifdef VERTEX_COLORS
    outColor.rgb*= color.rgb;
    #endif

    gl_FragColor = outColor;
}

