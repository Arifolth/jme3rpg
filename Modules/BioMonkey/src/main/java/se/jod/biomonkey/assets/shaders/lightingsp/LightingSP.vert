#ifndef NUM_LIGHTS
  #define NUM_LIGHTS 1
#endif

attribute vec3 inPosition;
attribute vec3 inNormal;

#if defined(DIFFUSEMAP) || defined(NORMALMAP) || defined(SPECULARMAP) || defined(ALPHAMAP)
  attribute vec2 inTexCoord;
  varying vec2 v_TexCoord;
#endif

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_WorldMatrix;
uniform mat4 g_ViewMatrix;
uniform mat3 g_NormalMatrix;

#ifdef VERTEX_LIGHTING

  uniform vec4 g_LightPosition[NUM_LIGHTS];
  uniform vec4 g_LightColor[NUM_LIGHTS];
  uniform vec4 g_AmbientLightColor;

  #ifdef MATERIAL_COLORS
    uniform vec4 m_Ambient;
    uniform vec4 m_Diffuse;
    uniform vec4 m_Specular;
  #endif
  uniform float m_Shininess;

  void calculateVertexColor(const in vec3 N, const in vec3 L, const in vec3 E, const in vec4 lightColor, inout vec4 vertexColor)
  {
    // calculate Diffuse Term:
    vec4 Idiff = lightColor * max(dot(N, L), 0.0);
    #ifdef MATERIAL_COLORS
      Idiff *= m_Diffuse;
    #endif

    // calculate Specular Term:
    vec3 R = normalize(-reflect(L, N));
    vec4 Ispec = lightColor * pow(max(dot(R, E), 0.0), m_Shininess);
    #ifdef MATERIAL_COLORS
      Ispec *= m_Specular;
    #endif

    vertexColor += Idiff + Ispec;
  }

  void doPerVertexLighting(const in vec4 position)
  {
    vec3 V; // view vector
    vec3 N; // normal vector
    vec3 E; // eye vector
    vec3 L; // light vector

    V = normalize(vec3(g_WorldViewMatrix * position)); // object space -> view space
    E = -V;
    N = g_NormalMatrix * inNormal; // object space -> view space
    gl_FrontColor = m_Ambient * g_AmbientLightColor;

    for (int i = 0; i < NUM_LIGHTS; i++)
    {
      vec4 lightPosition = g_LightPosition[i];
      vec4 lightColor = g_LightColor[i];
      vec4 lightVector;

      // positional or directional light?
      float isPosLight = step(0.5, lightColor.w);
      lightVector = vec4(lightPosition.xyz * sign(isPosLight - 0.5) - V * isPosLight,
        clamp(lightColor.w, 0.0, 1.0));
      
      lightVector = g_ViewMatrix * lightVector; // world space -> view space
      L = vec3(lightVector);

      calculateVertexColor(N, L, E, lightColor, gl_FrontColor);
    }
  }

#else // per fragment lighting

  varying vec3 v_Position;
  varying vec3 v_View;
  varying vec3 v_Normal;

  #if defined(NORMALMAP)
    attribute vec4 inTangent;
    varying vec3 v_Tangent;
    varying vec3 v_Bitangent;
  #endif

#endif

void main(void)
{
  vec4 position = vec4(inPosition, 1.0);

  #ifdef VERTEX_LIGHTING
    doPerVertexLighting(position);
  #else
    v_Position = vec3(g_WorldMatrix * position); // object space -> world space
    v_View = normalize(vec3(g_WorldViewMatrix * position)); // object space -> view space
    v_Normal = normalize(g_NormalMatrix * inNormal); // object space -> view space

    #if defined(NORMALMAP)      
      v_Tangent = normalize(g_NormalMatrix * inTangent.xyz); // object space -> view space
      v_Bitangent = cross(v_Normal, v_Tangent) * -inTangent.w;

      // view space -> tangent space
      v_View = v_View * mat3(v_Tangent, v_Bitangent, v_Normal);
    #endif
  #endif

  #if defined(DIFFUSEMAP) || defined(NORMALMAP) || defined(SPECULARMAP) || defined(ALPHAMAP)
    v_TexCoord = inTexCoord;
  #endif

  gl_Position = g_WorldViewProjectionMatrix * position; // object space -> projection space
}
