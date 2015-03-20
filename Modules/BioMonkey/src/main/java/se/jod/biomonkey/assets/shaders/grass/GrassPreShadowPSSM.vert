
attribute vec3 inPosition;
attribute vec2 inTexCoord;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;

varying vec3 texCoord;

//#if !defined(HQ_FADING)
//uniform float m_FadeEnd;
//uniform float m_FadeRange;
//#endif

#ifdef SWAYING
uniform float g_Time;
uniform vec2 m_Wind;
uniform vec3 m_SwayData;
#endif

void main(){
    vec4 pos = vec4(inPosition,1.0);

    #ifdef SWAYING
    float angle = (g_Time + pos.x*m_SwayData.y) * m_SwayData.x;
    pos.xz += 0.2*m_SwayData.z*m_Wind*inTexCoord.y*sin(angle);
    #endif

    // Fading
    //vec4 wvPos = g_WorldViewMatrix*pos;
    //float dist = wvPos.z;
    
    //#if !defined(HQ_FADING)
    //float fadeVal = clamp((m_FadeEnd - dist)/m_FadeRange,0.0,1.0);
    //pos.y *= fadeVal;
    //#else
    //texCoord.z = dist;
    //#endif
    
    gl_Position = g_WorldViewProjectionMatrix * pos;
    texCoord.xy = inTexCoord;
}