uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform vec3 g_CameraPosition;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec3 inTangent;
attribute vec2 inTexCoord;

uniform float m_BSRadius;

varying vec3 normal;
varying vec3 tangent;
varying vec3 bitangent;
varying vec2 texCoord;
varying float depth;

void main() {
    texCoord = inTexCoord.xy;
    vec4 pos = vec4(inPosition,1.0);
    //World pos is (0,0,0) so local = world pos.
    vec2 circlePos = normalize(g_CameraPosition.xz)*vec2(m_BSRadius);
    float dist = distance(pos.xz,circlePos);
    float diam = 2.0*m_BSRadius;
    depth = (diam - dist)/diam;
    
    //You never know..
    vec4 tempNorm = vec4(normalize(inNormal),0.0);
    vec4 tempTang = vec4(normalize(inTangent),0.0);

    normal = (g_WorldMatrix*tempNorm).xyz;
    tangent = (g_WorldMatrix*tempTang).xyz;
    bitangent = cross(normal,tangent);

    gl_Position = g_WorldViewProjectionMatrix*pos;
}
