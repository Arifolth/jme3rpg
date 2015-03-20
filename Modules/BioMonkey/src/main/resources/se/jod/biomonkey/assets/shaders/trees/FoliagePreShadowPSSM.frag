varying vec2 texCoord;

uniform sampler2D m_ColorMap;
uniform float m_AlphaCutOff;


void main(){
    if (texture2D(m_ColorMap, texCoord).a <= m_AlphaCutOff){
        discard;
    }
   gl_FragColor = vec4(1.0);
}