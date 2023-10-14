
#ifdef TEXTURE
uniform sampler2D m_Texture;
varying vec2 texCoord;
#endif

#if defined(DISCARD_ALPHA)
uniform float m_AlphaDiscardThreshold;
#endif

uniform vec4 m_Color;

void main(void)
{
vec4 color = vec4(1.0);
color *= m_Color;

#if defined(DISCARD_ALPHA)
if(color.a < m_AlphaDiscardThreshold){
    discard;
}
#endif
#ifdef TEXTURE
vec4 texVal = texture2D(m_Texture, texCoord);
gl_FragColor = texVal * m_Color;
#else
gl_FragColor = m_Color;
#endif
}