#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 m_FogColor;

varying vec2 texCoord;

void main() {
    // Simple fog with some texture variation
    vec2 pos = texCoord * 16.0;
    float noise = sin(pos.x * 2.0) * cos(pos.y * 1.8) * 0.1 + 0.9;

    vec4 fogColor = m_FogColor;
    fogColor.a *= noise;

    gl_FragColor = fogColor;
}
