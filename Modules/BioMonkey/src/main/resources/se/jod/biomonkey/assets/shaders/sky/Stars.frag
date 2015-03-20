uniform sampler2D m_Texture;

varying vec4 color;

void main() {
    gl_FragColor = texture2D(m_Texture, gl_PointCoord.xy)*color;
}

