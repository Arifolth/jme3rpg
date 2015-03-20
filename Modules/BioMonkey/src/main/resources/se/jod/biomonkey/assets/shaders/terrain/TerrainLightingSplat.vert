uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;
uniform mat4 g_ViewMatrix;
uniform mat4 g_WorldMatrix;

#ifdef FOG
uniform vec3 g_CameraPosition;
varying float vdist;
varying vec3 dir;
#endif

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;


varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vViewDir;

#if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
attribute vec4 inTangent;
varying vec3 vTangent;
varying vec3 vBiTangent;
#endif

#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif

void main(){
    vec4 pos = vec4(inPosition, 1.0);
    gl_Position = g_WorldViewProjectionMatrix * pos;
    texCoord = inTexCoord;

    vPosition = (g_WorldViewMatrix * pos).xyz;
    vNormal  = normalize(g_NormalMatrix * inNormal);
    vViewDir = normalize(-vPosition);

    //--------------------------
    // specific to normal maps:
    //--------------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
      vTangent = normalize(g_NormalMatrix * inTangent.xyz);
      vBiTangent = cross(vNormal, vTangent) * -inTangent.w;
    #endif
       
    #ifdef FOG
    vec4 wPos = g_WorldMatrix*pos;
    vec3 ray = wPos.xyz - g_CameraPosition;
    vdist = length(ray);
    dir = ray/vdist;
    #endif

    #ifdef TRI_PLANAR_MAPPING
      wVertex = vec4(inPosition,0.0);
      wNormal = inNormal;
    #endif

}