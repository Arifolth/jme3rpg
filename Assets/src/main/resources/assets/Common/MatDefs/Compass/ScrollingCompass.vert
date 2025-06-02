attribute vec3 inPosition;
attribute vec2 inTexCoord;

uniform mat4 g_WorldViewProjectionMatrix;
uniform float m_ScrollOffset;

varying vec2 texCoord;

void main() {
    texCoord = inTexCoord + vec2(m_ScrollOffset, 0.0);
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}