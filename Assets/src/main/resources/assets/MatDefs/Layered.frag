uniform vec4 m_Color;
varying vec2 texCoord;
uniform sampler2D m_GrassTexture;
uniform sampler2D m_GrassMask;
uniform sampler3D m_ThreeDTex;
uniform float m_MaskScale;
uniform float m_GrassTexScale;
uniform float m_Layer;
uniform float m_GrassDistance;
varying float distFromCam;

#ifdef ALPHAMAP
varying float alphaVal;
#endif

varying float lightIntensity;

void main(){
    if (distFromCam > m_GrassDistance) {discard;}

#ifdef ALPHAMAP
    if (alphaVal == 0.0) {discard;}
#endif
    vec4 color;
    float GrassMask;

#ifdef THREEDTEX
    float maskAlpha = (texture3D(m_ThreeDTex, vec3(texCoord * m_MaskScale,m_Layer)).a);
#else
    float maskAlpha = (texture2D( m_GrassMask, texCoord * m_MaskScale).a);
#endif
    if (maskAlpha == 0.0) discard;
    color = texture2D( m_GrassTexture, texCoord * vec2(m_GrassTexScale,m_GrassTexScale));

    //LIGHTING
    //gl_FragColor = color * lightIntensity;

#ifdef ALPHAMAP
    maskAlpha = min(alphaVal, maskAlpha);
#endif
    
    gl_FragColor = color;
    gl_FragColor.a = maskAlpha;

    //gl_FragColor = color;
    //gl_FragColor = FinalColour;
}