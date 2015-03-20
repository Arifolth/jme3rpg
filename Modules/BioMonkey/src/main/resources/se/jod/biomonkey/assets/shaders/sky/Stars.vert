uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec4 inColor;
attribute float inSize;

varying vec4 color;

void main() {
    vec4 pos = vec4(inPosition, 1.0);
    gl_PointSize = inSize;
    color = inColor;
    gl_Position = g_WorldViewProjectionMatrix * pos;
}
