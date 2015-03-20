
uniform float m_MoonPhase;
varying vec2 texCoord;

uniform sampler2D m_MoonTex;

void main() {
    
    // Output color
    gl_FragColor = texture2D(m_MoonTex, texCoord);
}

