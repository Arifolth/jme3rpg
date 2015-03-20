uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform vec3 g_CameraPosition;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

#ifdef VERTEX_COLORS
attribute vec4 inColor;
varying vec4 color;
#endif

#ifdef FADE_ENABLED
uniform float m_FadeEnd;
uniform float m_FadeRange;
#endif

#ifdef SWAYING
uniform float g_Time;
uniform vec2 m_Wind;
uniform vec3 m_SwayData;
#endif

#ifdef VERTEX_LIGHTING

uniform vec4 g_LightPosition[4];
uniform vec4 g_LightColor[4];
uniform vec4 g_LightDirection[4];

uniform vec4 g_AmbientLightColor;

varying vec3 diffuseLight;
varying vec3 ambientLight;

  #ifdef SELF_SHADOWING
  attribute vec2 inTexCoord2;
  #endif

#endif

varying vec3 texCoord;

void main() {
    texCoord = vec3(inTexCoord,1.0);
    vec4 pos = vec4(inPosition,1.0);

    #ifdef VERTEX_COLORS
    color = inColor;
    #endif

    vec4 worldPos = g_WorldMatrix*pos;
    
    #ifdef VERTEX_LIGHTING
    diffuseLight = vec3(0.0,0.0,0.0);
      #ifdef SELF_SHADOWING
      vec2 quadNorm = inTexCoord2;
      //Tangent directed towards texcoord x = 1.
      vec2 quadTan = vec2(inTexCoord2.y,-inTexCoord2.x);
      #endif
    for(int i = 0; i < NUM_LIGHTS; i++){
    vec3 diffLight = vec3(0.0,0.0,0.0);
        float posLight = step(0.5, g_LightColor[i].w);
        vec3 lightVec = g_LightPosition[i].xyz * sign(posLight - 0.5) - (worldPos.xyz * posLight);
        float lDist = length(lightVec);

        float att = clamp(1.0 - g_LightPosition[i].w * lDist * posLight, 0.0, 1.0);
        lightVec = lightVec / vec3(lDist);
        //Spotlights
        float spotFallOff = 1.0;
        if(g_LightDirection[i].w != 0.0){
            vec3 spotdir = normalize(g_LightDirection[i].xyz);
            float curAngleCos = dot(-lightVec, spotdir);    
            float innerAngleCos = floor(g_LightDirection[i].w) * 0.001;
            float outerAngleCos = fract(g_LightDirection[i].w);
            float innerMinusOuter = innerAngleCos - outerAngleCos;
            spotFallOff = clamp((curAngleCos - outerAngleCos) / innerMinusOuter, 0.0, 1.0);
        }
        diffLight = g_LightColor[i].rgb*(att*spotFallOff);

        #ifdef SELF_SHADOWING
        //Find the angle between the quad and the light to find the level of self shadowing.
        vec2 lightXZ = normalize(lightVec.xz);
        //The strength of the shadow depends on the lights angle of inclination.
        float str = 0.5 + 0.5*abs(dot(lightXZ,quadNorm));
        //How this vert is affected depends on whether the light comes in from the
        //"left" or the "right". Light coming in from the right does not
        //shadow the right side of the quad.
        float dir = sign(dot(lightXZ,quadTan));
        //Depending on these vars, we apply self shadowing to this vert.
        float sFact = 1.0;
        if(dir > 0.0 && inTexCoord.x > 0.5){
            sFact *= str;
        }
        else if (dir < 0.0 && inTexCoord.x < 0.5){
            sFact *= str;
        }
        sFact -= step(-inTexCoord.y, -0.5)*0.5*(1.0 - abs(lightVec.y));
        //Modify this lights contribution by the self shadowing factor.
        diffLight *= sFact;
        #endif 
        diffuseLight += diffLight;
    }
    ambientLight = g_AmbientLightColor.rgb;
    #endif

    #if defined(FADE_ENABLED) || defined(SWAYING)
    float dist = distance(g_CameraPosition.xz, worldPos.xz);
    #endif

    #ifdef FADE_ENABLED
    texCoord.z = clamp((m_FadeEnd - dist)/(m_FadeRange),0.0,1.0);
    #endif

    #ifdef SWAYING
    float angle = (g_Time + pos.x*m_SwayData.y) * m_SwayData.x;
    pos.xz += 0.1*m_Wind*inTexCoord.y*sin(angle);
    #endif

    gl_Position = g_WorldViewProjectionMatrix * pos;
}
