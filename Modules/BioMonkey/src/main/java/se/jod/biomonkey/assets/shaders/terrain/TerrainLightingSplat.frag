#import "se/jod/biomonkey/assets/shaders/lib/Fog.glsllib"
#ifndef NUM_LIGHTS
  #define NUM_LIGHTS 1
#endif

uniform mat4 g_ViewMatrix;

uniform vec4 g_LightPosition[NUM_LIGHTS];
uniform vec4 g_LightColor[NUM_LIGHTS];
uniform vec4 g_AmbientLightColor;

#ifdef ENABLE_SPOTLIGHTS
uniform vec4 g_LightDirection[NUM_LIGHTS];
#endif

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vViewDir;

#if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
varying vec3 vTangent;
varying vec3 vBiTangent;
#endif

#ifdef AMBIENT_COLOR
uniform vec4 m_Ambient;
#endif

#ifdef DIFFUSE_COLOR
uniform vec4 m_Diffuse;
#endif

#ifdef FOG
varying float vdist;
varying vec3 dir;
#endif

#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif
#ifdef DIFFUSEMAP_1
  uniform sampler2D m_DiffuseMap_1;
#endif
#ifdef DIFFUSEMAP_2
  uniform sampler2D m_DiffuseMap_2;
#endif
#ifdef DIFFUSEMAP_3
  uniform sampler2D m_DiffuseMap_3;
#endif
#ifdef DIFFUSEMAP_4
  uniform sampler2D m_DiffuseMap_4;
#endif
#ifdef DIFFUSEMAP_5
  uniform sampler2D m_DiffuseMap_5;
#endif
#ifdef DIFFUSEMAP_6
  uniform sampler2D m_DiffuseMap_6;
#endif
#ifdef DIFFUSEMAP_7
  uniform sampler2D m_DiffuseMap_7;
#endif
#ifdef DIFFUSEMAP_8
  uniform sampler2D m_DiffuseMap_8;
#endif
#ifdef DIFFUSEMAP_9
  uniform sampler2D m_DiffuseMap_9;
#endif
#ifdef DIFFUSEMAP_10
  uniform sampler2D m_DiffuseMap_10;
#endif
#ifdef DIFFUSEMAP_11
  uniform sampler2D m_DiffuseMap_11;
#endif


#ifdef DIFFUSEMAP_0_SCALE
  uniform float m_DiffuseMap_0_scale;
#endif
#ifdef DIFFUSEMAP_1_SCALE
  uniform float m_DiffuseMap_1_scale;
#endif
#ifdef DIFFUSEMAP_2_SCALE
  uniform float m_DiffuseMap_2_scale;
#endif
#ifdef DIFFUSEMAP_3_SCALE
  uniform float m_DiffuseMap_3_scale;
#endif
#ifdef DIFFUSEMAP_4_SCALE
  uniform float m_DiffuseMap_4_scale;
#endif
#ifdef DIFFUSEMAP_5_SCALE
  uniform float m_DiffuseMap_5_scale;
#endif
#ifdef DIFFUSEMAP_6_SCALE
  uniform float m_DiffuseMap_6_scale;
#endif
#ifdef DIFFUSEMAP_7_SCALE
  uniform float m_DiffuseMap_7_scale;
#endif
#ifdef DIFFUSEMAP_8_SCALE
  uniform float m_DiffuseMap_8_scale;
#endif
#ifdef DIFFUSEMAP_9_SCALE
  uniform float m_DiffuseMap_9_scale;
#endif
#ifdef DIFFUSEMAP_10_SCALE
  uniform float m_DiffuseMap_10_scale;
#endif
#ifdef DIFFUSEMAP_11_SCALE
  uniform float m_DiffuseMap_11_scale;
#endif


#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif

#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;
#endif
#ifdef NORMALMAP_1
  uniform sampler2D m_NormalMap_1;
#endif
#ifdef NORMALMAP_2
  uniform sampler2D m_NormalMap_2;
#endif
#ifdef NORMALMAP_3
  uniform sampler2D m_NormalMap_3;
#endif
#ifdef NORMALMAP_4
  uniform sampler2D m_NormalMap_4;
#endif
#ifdef NORMALMAP_5
  uniform sampler2D m_NormalMap_5;
#endif
#ifdef NORMALMAP_6
  uniform sampler2D m_NormalMap_6;
#endif
#ifdef NORMALMAP_7
  uniform sampler2D m_NormalMap_7;
#endif
#ifdef NORMALMAP_8
  uniform sampler2D m_NormalMap_8;
#endif
#ifdef NORMALMAP_9
  uniform sampler2D m_NormalMap_9;
#endif
#ifdef NORMALMAP_10
  uniform sampler2D m_NormalMap_10;
#endif
#ifdef NORMALMAP_11
  uniform sampler2D m_NormalMap_11;
#endif


#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif

const vec3 AmbientMult = vec3(0.2,0.2,0.2);
const vec3 DiffuseMult = vec3(1.0,1.0,1.0);

void calculateLightVector(const in vec4 lightPosition, const in vec4 lightColor, 
    out vec3 lightVector, out float attenuation)
  {
    // positional or directional light?
    if (lightColor.w == 0.0)
    {
      lightVector = -lightPosition.xyz;
      attenuation = 1.0;
    }
    else
    {
      lightVector = lightPosition.xyz - vPosition;
      float dist = length(lightVector);
      lightVector /= vec3(dist);
      attenuation = clamp(1.0 - lightPosition.w * dist, 0.0, 1.0);
    }

    float spotFallOff = 1.0;
      #ifdef ENABLE_SPOTLIGHTS
        if(g_LightDirection[i].w != 0.0){
              vec3 L = normalize(lightVec[i].xyz);
              vec3 spotdir = normalize(g_lightDirection[i].xyz);
              float curAngleCos = dot(-L, spotdir);
              float innerAngleCos = floor(g_LightDirection[i].w) * 0.001;
              float outerAngleCos = fract(g_LightDirection[i].w);
              float innerMinusOuter = innerAngleCos - outerAngleCos;
              spotFallOff = (curAngleCos - outerAngleCos) / innerMinusOuter;
              spotFallOff = clamp(spotFallOff, 0.0, 1.0);
        }
      #endif
  }

#ifdef ALPHAMAP
  vec4 calculateDiffuseBlend(in vec2 texCoord) {
    vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );
    
    #ifdef ALPHAMAP_1
      vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

    vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord * m_DiffuseMap_0_scale);
    //vec4 diffuseColor_var = texture2D(m_DiffuseMap, texCoord.ts * m_DiffuseMap_0_scale*2.0);
    //diffuseColor *= diffuseColor_var*alphaBlend.r;
    diffuseColor = diffuseColor*alphaBlend.r;
    #ifdef DIFFUSEMAP_1
      vec4 diffuseColor1 = texture2D(m_DiffuseMap_1, texCoord * m_DiffuseMap_1_scale);
      //vec4 diffuseColor1_var = texture2D(m_DiffuseMap_1, texCoord.ts * m_DiffuseMap_1_scale*2.0);
      //diffuseColor = mix( diffuseColor, diffuseColor1*diffuseColor1_var, alphaBlend.g );
      diffuseColor = mix( diffuseColor, diffuseColor1, alphaBlend.g );
      #ifdef DIFFUSEMAP_2
        vec4 diffuseColor2 = texture2D(m_DiffuseMap_2, texCoord * m_DiffuseMap_2_scale);
        diffuseColor = mix( diffuseColor, diffuseColor2, alphaBlend.b );
        #ifdef DIFFUSEMAP_3
          vec4 diffuseColor3 = texture2D(m_DiffuseMap_3, texCoord * m_DiffuseMap_3_scale);
          diffuseColor = mix( diffuseColor, diffuseColor3, alphaBlend.a );
          #ifdef ALPHAMAP_1
              #ifdef DIFFUSEMAP_4
                vec4 diffuseColor4 = texture2D(m_DiffuseMap_4, texCoord * m_DiffuseMap_4_scale);
                diffuseColor = mix( diffuseColor, diffuseColor4, alphaBlend1.r );
                #ifdef DIFFUSEMAP_5
                  vec4 diffuseColor5 = texture2D(m_DiffuseMap_5, texCoord * m_DiffuseMap_5_scale);
                  diffuseColor = mix( diffuseColor, diffuseColor5, alphaBlend1.g );
                  #ifdef DIFFUSEMAP_6
                    vec4 diffuseColor6 = texture2D(m_DiffuseMap_6, texCoord * m_DiffuseMap_6_scale);
                    diffuseColor = mix( diffuseColor, diffuseColor6, alphaBlend1.b );
                    #ifdef DIFFUSEMAP_7
                      vec4 diffuseColor7 = texture2D(m_DiffuseMap_7, texCoord * m_DiffuseMap_7_scale);
                      diffuseColor = mix( diffuseColor, diffuseColor7, alphaBlend1.a );
                      #ifdef ALPHAMAP_2
                          #ifdef DIFFUSEMAP_8
                            vec4 diffuseColor8 = texture2D(m_DiffuseMap_8, texCoord * m_DiffuseMap_8_scale);
                            diffuseColor = mix( diffuseColor, diffuseColor8, alphaBlend2.r );
                            #ifdef DIFFUSEMAP_9
                              vec4 diffuseColor9 = texture2D(m_DiffuseMap_9, texCoord * m_DiffuseMap_9_scale);
                              diffuseColor = mix( diffuseColor, diffuseColor9, alphaBlend2.g );
                              #ifdef DIFFUSEMAP_10
                                vec4 diffuseColor10 = texture2D(m_DiffuseMap_10, texCoord * m_DiffuseMap_10_scale);
                                diffuseColor = mix( diffuseColor, diffuseColor10, alphaBlend2.b );
                                #ifdef DIFFUSEMAP_11
                                  vec4 diffuseColor11 = texture2D(m_DiffuseMap_11, texCoord * m_DiffuseMap_11_scale);
                                  diffuseColor = mix( diffuseColor, diffuseColor11, alphaBlend2.a );
                                #endif
                              #endif
                            #endif
                          #endif
                      #endif
                    #endif
                  #endif
                #endif
              #endif
          #endif
        #endif
      #endif
    #endif
    return diffuseColor;
  }

  vec3 calculateNormal(in vec2 texCoord) {
    vec3 normal = vec3(0,0,1);
    vec3 n = vec3(0,0,0);

    vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );

    #ifdef ALPHAMAP_1
      vec4 alphaBlend1 = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2 = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

    #ifdef NORMALMAP
      n = texture2D(m_NormalMap, texCoord * m_DiffuseMap_0_scale).xyz;
      normal += n * alphaBlend.r;
    #endif

    #ifdef NORMALMAP_1
      n = texture2D(m_NormalMap_1, texCoord * m_DiffuseMap_1_scale).xyz;
      normal += n * alphaBlend.g;
    #endif

    #ifdef NORMALMAP_2
      n = texture2D(m_NormalMap_2, texCoord * m_DiffuseMap_2_scale).xyz;
      normal += n * alphaBlend.b;
    #endif

    #ifdef NORMALMAP_3
      n = texture2D(m_NormalMap_3, texCoord * m_DiffuseMap_3_scale).xyz;
      normal += n * alphaBlend.a;
    #endif

    #ifdef ALPHAMAP_1
        #ifdef NORMALMAP_4
          n = texture2D(m_NormalMap_4, texCoord * m_DiffuseMap_4_scale).xyz;
          normal += n * alphaBlend1.r;
        #endif

        #ifdef NORMALMAP_5
          n = texture2D(m_NormalMap_5, texCoord * m_DiffuseMap_5_scale).xyz;
          normal += n * alphaBlend1.g;
        #endif

        #ifdef NORMALMAP_6
          n = texture2D(m_NormalMap_6, texCoord * m_DiffuseMap_6_scale).xyz;
          normal += n * alphaBlend1.b;
        #endif

        #ifdef NORMALMAP_7
          n = texture2D(m_NormalMap_7, texCoord * m_DiffuseMap_7_scale).xyz;
          normal += n * alphaBlend1.a;
        #endif
    #endif

    #ifdef ALPHAMAP_2
        #ifdef NORMALMAP_8
          n = texture2D(m_NormalMap_8, texCoord * m_DiffuseMap_8_scale).xyz;
          normal += n * alphaBlend2.r;
        #endif

        #ifdef NORMALMAP_9
          n = texture2D(m_NormalMap_9, texCoord * m_DiffuseMap_9_scale);
          normal += n * alphaBlend2.g;
        #endif

        #ifdef NORMALMAP_10
          n = texture2D(m_NormalMap_10, texCoord * m_DiffuseMap_10_scale);
          normal += n * alphaBlend2.b;
        #endif

        #ifdef NORMALMAP_11
          n = texture2D(m_NormalMap_11, texCoord * m_DiffuseMap_11_scale);
          normal += n * alphaBlend2.a;
        #endif
    #endif

    normal = (normal.xyz * vec3(2.0) - vec3(1.0));
    return normalize(normal);
  }

  #ifdef TRI_PLANAR_MAPPING

    vec4 getTriPlanarBlend(in vec4 coords, in vec3 blending, in sampler2D map, in float scale) {
      vec4 col1 = texture2D( map, coords.yz * scale);
      vec4 col2 = texture2D( map, coords.xz * scale);
      vec4 col3 = texture2D( map, coords.xy * scale);
      // blend the results of the 3 planar projections.
      vec4 tex = col1 * blending.x + col2 * blending.y + col3 * blending.z;
      return tex;
    }

    vec4 calculateTriPlanarDiffuseBlend(in vec3 wNorm, in vec4 wVert, in vec2 texCoord) {
        // tri-planar texture bending factor for this fragment's normal
        vec3 blending = abs( wNorm );
        blending = (blending -0.2) * 0.7;
        blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
        float b = (blending.x + blending.y + blending.z);
        blending /= vec3(b, b, b);

        // texture coords
        vec4 coords = wVert;

        // blend the results of the 3 planar projections.
        vec4 tex0 = getTriPlanarBlend(coords, blending, m_DiffuseMap, m_DiffuseMap_0_scale);

        #ifdef DIFFUSEMAP_1
          // blend the results of the 3 planar projections.
          vec4 tex1 = getTriPlanarBlend(coords, blending, m_DiffuseMap_1, m_DiffuseMap_1_scale);
        #endif
        #ifdef DIFFUSEMAP_2
          // blend the results of the 3 planar projections.
          vec4 tex2 = getTriPlanarBlend(coords, blending, m_DiffuseMap_2, m_DiffuseMap_2_scale);
        #endif
        #ifdef DIFFUSEMAP_3
          // blend the results of the 3 planar projections.
          vec4 tex3 = getTriPlanarBlend(coords, blending, m_DiffuseMap_3, m_DiffuseMap_3_scale);
        #endif
        #ifdef DIFFUSEMAP_4
          // blend the results of the 3 planar projections.
          vec4 tex4 = getTriPlanarBlend(coords, blending, m_DiffuseMap_4, m_DiffuseMap_4_scale);
        #endif
        #ifdef DIFFUSEMAP_5
          // blend the results of the 3 planar projections.
          vec4 tex5 = getTriPlanarBlend(coords, blending, m_DiffuseMap_5, m_DiffuseMap_5_scale);
        #endif
        #ifdef DIFFUSEMAP_6
          // blend the results of the 3 planar projections.
          vec4 tex6 = getTriPlanarBlend(coords, blending, m_DiffuseMap_6, m_DiffuseMap_6_scale);
        #endif
        #ifdef DIFFUSEMAP_7
          // blend the results of the 3 planar projections.
          vec4 tex7 = getTriPlanarBlend(coords, blending, m_DiffuseMap_7, m_DiffuseMap_7_scale);
        #endif
        #ifdef DIFFUSEMAP_8
          // blend the results of the 3 planar projections.
          vec4 tex8 = getTriPlanarBlend(coords, blending, m_DiffuseMap_8, m_DiffuseMap_8_scale);
        #endif
        #ifdef DIFFUSEMAP_9
          // blend the results of the 3 planar projections.
          vec4 tex9 = getTriPlanarBlend(coords, blending, m_DiffuseMap_9, m_DiffuseMap_9_scale);
        #endif
        #ifdef DIFFUSEMAP_10
          // blend the results of the 3 planar projections.
          vec4 tex10 = getTriPlanarBlend(coords, blending, m_DiffuseMap_10, m_DiffuseMap_10_scale);
        #endif
        #ifdef DIFFUSEMAP_11
          // blend the results of the 3 planar projections.
          vec4 tex11 = getTriPlanarBlend(coords, blending, m_DiffuseMap_11, m_DiffuseMap_11_scale);
        #endif

        vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );

        #ifdef ALPHAMAP_1
          vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
        #endif
        #ifdef ALPHAMAP_2
          vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
        #endif

        vec4 diffuseColor = tex0 * alphaBlend.r;
        #ifdef DIFFUSEMAP_1
          diffuseColor = mix( diffuseColor, tex1, alphaBlend.g );
          #ifdef DIFFUSEMAP_2
            diffuseColor = mix( diffuseColor, tex2, alphaBlend.b );
            #ifdef DIFFUSEMAP_3
              diffuseColor = mix( diffuseColor, tex3, alphaBlend.a );
              #ifdef ALPHAMAP_1
                  #ifdef DIFFUSEMAP_4
                    diffuseColor = mix( diffuseColor, tex4, alphaBlend1.r );
                    #ifdef DIFFUSEMAP_5
                      diffuseColor = mix( diffuseColor, tex5, alphaBlend1.g );
                      #ifdef DIFFUSEMAP_6
                        diffuseColor = mix( diffuseColor, tex6, alphaBlend1.b );
                        #ifdef DIFFUSEMAP_7
                          diffuseColor = mix( diffuseColor, tex7, alphaBlend1.a );
                          #ifdef ALPHAMAP_2
                              #ifdef DIFFUSEMAP_8
                                diffuseColor = mix( diffuseColor, tex8, alphaBlend2.r );
                                #ifdef DIFFUSEMAP_9
                                  diffuseColor = mix( diffuseColor, tex9, alphaBlend2.g );
                                  #ifdef DIFFUSEMAP_10
                                    diffuseColor = mix( diffuseColor, tex10, alphaBlend2.b );
                                    #ifdef DIFFUSEMAP_11
                                      diffuseColor = mix( diffuseColor, tex11, alphaBlend2.a );
                                    #endif
                                  #endif
                                #endif
                              #endif
                          #endif
                        #endif
                      #endif
                    #endif
                  #endif
              #endif
            #endif
          #endif
        #endif

        return diffuseColor;
    }

    vec3 calculateNormalTriPlanar(in vec3 wNorm, in vec4 wVert,in vec2 texCoord) {
      // tri-planar texture bending factor for this fragment's world-space normal
      vec3 blending = abs( wNorm );
      blending = (blending -0.2) * 0.7;
      blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
      float b = (blending.x + blending.y + blending.z);
      blending /= vec3(b, b, b);

      // texture coords
      vec4 coords = wVert;
      vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );

    #ifdef ALPHAMAP_1
      vec4 alphaBlend1 = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2 = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

      vec3 normal = vec3(0,0,1);
      vec3 n = vec3(0,0,0);

      #ifdef NORMALMAP
          n = getTriPlanarBlend(coords, blending, m_NormalMap, m_DiffuseMap_0_scale).xyz;
          normal += n * alphaBlend.r;
      #endif

      #ifdef NORMALMAP_1
          n = getTriPlanarBlend(coords, blending, m_NormalMap_1, m_DiffuseMap_1_scale).xyz;
          normal += n * alphaBlend.g;
      #endif

      #ifdef NORMALMAP_2
          n = getTriPlanarBlend(coords, blending, m_NormalMap_2, m_DiffuseMap_2_scale).xyz;
          normal += n * alphaBlend.b;
      #endif

      #ifdef NORMALMAP_3
          n = getTriPlanarBlend(coords, blending, m_NormalMap_3, m_DiffuseMap_3_scale).xyz;
          normal += n * alphaBlend.a;
      #endif

      #ifdef ALPHAMAP_1
          #ifdef NORMALMAP_4
              n = getTriPlanarBlend(coords, blending, m_NormalMap_4, m_DiffuseMap_4_scale).xyz;
              normal += n * alphaBlend1.r;
          #endif

          #ifdef NORMALMAP_5
              n = getTriPlanarBlend(coords, blending, m_NormalMap_5, m_DiffuseMap_5_scale).xyz;
              normal += n * alphaBlend1.g;
          #endif

          #ifdef NORMALMAP_6
              n = getTriPlanarBlend(coords, blending, m_NormalMap_6, m_DiffuseMap_6_scale).xyz;
              normal += n * alphaBlend1.b;
          #endif

          #ifdef NORMALMAP_7
              n = getTriPlanarBlend(coords, blending, m_NormalMap_7, m_DiffuseMap_7_scale).xyz;
              normal += n * alphaBlend1.a;
          #endif
      #endif

      #ifdef ALPHAMAP_2
          #ifdef NORMALMAP_8
              n = getTriPlanarBlend(coords, blending, m_NormalMap_8, m_DiffuseMap_8_scale).xyz;
              normal += n * alphaBlend2.r;
          #endif

          #ifdef NORMALMAP_9
              n = getTriPlanarBlend(coords, blending, m_NormalMap_9, m_DiffuseMap_9_scale).xyz;
              normal += n * alphaBlend2.g;
          #endif

          #ifdef NORMALMAP_10
              n = getTriPlanarBlend(coords, blending, m_NormalMap_10, m_DiffuseMap_10_scale).xyz;
              normal += n * alphaBlend2.b;
          #endif

          #ifdef NORMALMAP_11
              n = getTriPlanarBlend(coords, blending, m_NormalMap_11, m_DiffuseMap_11_scale).xyz;
              normal += n * alphaBlend2.a;
          #endif
      #endif

      normal = (normal.xyz * vec3(2.0) - vec3(1.0));
      return normalize(normal);
    }
  #endif

#endif

void main(){

    //----------------------
    // diffuse calculations
    //----------------------
    #ifdef DIFFUSEMAP
      #ifdef ALPHAMAP
        #ifdef TRI_PLANAR_MAPPING
            vec4 diffuseColor = calculateTriPlanarDiffuseBlend(wNormal, wVertex, texCoord);
        #else
            vec4 diffuseColor = calculateDiffuseBlend(texCoord);
        #endif
      #else
        vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord);
      #endif
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

    
    //---------------------
    // normal calculations
    //---------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
      #ifdef TRI_PLANAR_MAPPING
        vec3 normal = calculateNormalTriPlanar(wNormal, wVertex, texCoord);
      #else
        vec3 normal = calculateNormal(texCoord);
      #endif
    #else
      vec3 normal = vNormal;
    #endif
    
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
      vec3 tangent = normalize(vTangent);
      vec3 bitangent = normalize(vBiTangent);
      vec3 norm = normalize(vNormal);

    // view space -> tangent space matrix
      mat4 vsTangentMatrix = mat4(vec4(tangent.x, bitangent.x, norm.x, 0.0),
                                  vec4(tangent.y, bitangent.y, norm.y, 0.0),
                                  vec4(tangent.z, bitangent.z, norm.z, 0.0),
                                  vec4(      0.0,         0.0,    0.0, 1.0));

      // world space -> tangent space matrix
      mat4 wsViewTangentMatrix = vsTangentMatrix * g_ViewMatrix;
    #endif
    //-----------------------
    // lighting calculations
    //-----------------------
    vec3 diffuseLight = vec3(0.0,0.0,0.0);
    
    for(int i = 0; i < NUM_LIGHTS; i++){
      
      vec3 L; // light vector

      vec4 lightPosition = g_LightPosition[i];
      vec4 lightColor = g_LightColor[i];
      vec3 lightVector;
      float attenuation;

      //V = normalize(vView);
      //E = -V;

      calculateLightVector(lightPosition, lightColor, lightVector, attenuation);

      #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
        // world space -> tangent space
        L = vec3(wsViewTangentMatrix * vec4(lightVector, 0.0));
      #else        
        // world space -> view space
        L = vec3(g_ViewMatrix * vec4(lightVector, 0.0));
      #endif

      L = normalize(L);
      float NdotL = dot(normal,L);
      diffuseLight += lightColor.xyz * max(0.0,NdotL) * attenuation;
      
    }

    #ifdef AMBIENT_COLOR
      vec3 ambientMult = g_Ambient.xyz;
    #else
      vec3 ambientMult = AmbientMult;
    #endif

    #ifdef DIFFUSE_COLOR
      vec3 diffuseMult = g_Diffuse.xyz;
    #else
      vec3 diffuseMult = DiffuseMult;
    #endif

    gl_FragColor = vec4(diffuseColor.xyz*(ambientMult * g_AmbientLightColor.xyz + diffuseMult * diffuseLight),1.0);
    //gl_FragColor = vec4(normal*vec3(0.5) + vec3(0.5),1.0);
    #ifdef FOG
      gl_FragColor.rgb = applyFog(gl_FragColor.rgb,vdist,normalize(dir));
    #endif
}

