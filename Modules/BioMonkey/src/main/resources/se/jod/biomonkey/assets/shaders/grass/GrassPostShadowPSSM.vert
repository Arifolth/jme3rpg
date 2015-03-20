uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

uniform vec3 g_CameraPosition;

#ifdef SWAYING
uniform float g_Time;
uniform vec2 m_Wind;
uniform vec3 m_SwayData;
#endif

varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

varying float shadowPosition;

varying vec3 texCoord;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


void main(){
    // get the vertex in world space
    vec4 pos = vec4(inPosition.xyz,1.0);
    
    #ifdef SWAYING
    float angle = (g_Time + pos.x*m_SwayData.y) * m_SwayData.x;
    pos.xz += 0.2*m_Wind*inTexCoord.y*sin(angle);
    #endif

    vec4 worldPos = g_WorldMatrix * pos;

    vec2 CtoP = worldPos.xz - g_CameraPosition.xz;
    float dist = length(CtoP);
    texCoord.z = dist;
    
    texCoord.xy = inTexCoord;
    gl_Position = g_WorldViewProjectionMatrix * pos;
    shadowPosition = gl_Position.z;
    // populate the light view matrices array and convert vertex to light viewProj space
    projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;
}