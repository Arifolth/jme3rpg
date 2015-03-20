
uniform sampler2D m_DiffuseMap;
uniform float m_AlphaDiscardThreshold;

varying vec2 texCoord;

void main() {
    vec4 color = texture2D(m_DiffuseMap, texCoord);
    if(color.a < m_AlphaDiscardThreshold){
        discard;
    }
    gl_FragColor = color;
}

