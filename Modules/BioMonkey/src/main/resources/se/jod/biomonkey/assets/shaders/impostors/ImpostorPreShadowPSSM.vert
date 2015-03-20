//Based on PreShadow.vert
attribute vec4 inPosition;
attribute vec2 inTexCoord;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

varying vec2 texCoord;
attribute vec3 inTexCoord2;


void main(){
    vec4 pos = inPosition;

    vec4 worldPos = g_WorldMatrix*pos;
    
    gl_Position = g_WorldViewProjectionMatrix * pos;
    texCoord = inTexCoord;
}