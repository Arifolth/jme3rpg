uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;

void main() { 
    vec4 pos = vec4(inPosition, 1.0);
    texCoord = inTexCoord;
    gl_Position = g_WorldViewProjectionMatrix * pos;
}
