varying vec2 texCoord;

uniform sampler2D m_DiffuseMap;
uniform float m_AlphaDiscardThreshold;

void main(){
    if (texture2D(m_DiffuseMap, texCoord.st).a <= m_AlphaDiscardThreshold){
        discard;
    }
   gl_FragColor = vec4(1.0);
}