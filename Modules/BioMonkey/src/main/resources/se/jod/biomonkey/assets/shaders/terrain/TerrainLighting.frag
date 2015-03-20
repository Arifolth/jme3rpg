#import "Shaders/Libraries/Fog.glsllib"

uniform vec4 g_LightDirection;

varying vec4 AmbientSum;
varying vec4 DiffuseSum;

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;
varying vec3 lightVec;

#ifdef FOG
varying float vdist;
varying vec3 dir;
#endif

uniform sampler2D m_ColorTexture0;


#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif



float tangDot(in vec3 v1, in vec3 v2){
    float d = dot(v1,v2);
    #ifdef V_TANGENT
        d = 1.0 - d*d;
        return step(0.0, d) * sqrt(d);
    #else
        return d;
    #endif
}

float lightComputeDiffuse(in vec3 norm, in vec3 lightdir, in vec3 viewdir){
    return max(0.0, dot(norm, lightdir));
}

float computeLighting(in vec3 wvPos, in vec3 wvNorm, in vec3 wvViewDir, in vec3 wvLightDir){
   float diffuseFactor = lightComputeDiffuse(wvNorm, wvLightDir, wvViewDir);
   return diffuseFactor*vLightDir.w;
}


  #ifdef TRI_PLANAR_MAPPING
    vec4 calculateTriPlanarDiffuse(in vec3 wNorm, in vec4 wVert, in vec2 texCoord) {
        // tri-planar texture bending factor for this fragment's normal
        vec3 blending = abs( wNorm );
        blending = (blending -0.2) * 0.7;
        blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
        float b = (blending.x + blending.y + blending.z);
        blending /= vec3(b, b, b);

        // texture coords
        vec4 coords = wVert;

        vec4 col1 = texture2D( m_ColorTexture0, coords.yz);
        vec4 col2 = texture2D( m_ColorTexture0, coords.xz);
        vec4 col3 = texture2D( m_ColorTexture0, coords.xy);
        // blend the results of the 3 planar projections.
        vec4 tex = col1 * blending.x + col2 * blending.y + col3 * blending.z;

        return tex;
    }

    vec3 calculateTriPlanarNormal(in vec3 wNorm, in vec4 wVert,in vec2 texCoord) {
      // tri-planar texture bending factor for this fragment's world-space normal
      vec3 blending = abs( wNorm );
      blending = (blending -0.2) * 0.7;
      blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
      float b = (blending.x + blending.y + blending.z);
      blending /= vec3(b, b, b);

      // texture coords
      vec4 coords = wVert;

      // TODO add normal texture(s).
      vec4 col1 = texture2D( map, coords.yz);
      vec4 col2 = texture2D( map, coords.xz);
      vec4 col3 = texture2D( map, coords.xy);
      // blend the results of the 3 planar projections.
      vec4 normal = col1 * blending.x + col2 * blending.y + col3 * blending.z;

      return normalize(normal);
    }
  #endif



void main(){

    vec4 diffuseColor = vec4(1.0);
    #ifdef TRI_PLANAR_MAPPING
        diffuseColor = calculateTriPlanarDiffuseBlend(wNormal, wVertex, texCoord);
    #else
        diffuseColor = texture2D(m_ColorTexture0,texCoord);
    #endif

        float spotFallOff = 1.0;
        if(g_LightDirection.w!=0.0){
              vec3 L=normalize(lightVec.xyz);
              vec3 spotdir = normalize(g_LightDirection.xyz);
              float curAngleCos = dot(-L, spotdir);             
              float innerAngleCos = floor(g_LightDirection.w) * 0.001;
              float outerAngleCos = fract(g_LightDirection.w);
              float innerMinusOuter = innerAngleCos - outerAngleCos;

              spotFallOff = (curAngleCos - outerAngleCos) / innerMinusOuter;

              if(spotFallOff <= 0.0){
                  gl_FragColor = AmbientSum * diffuseColor;
                  return;
              }else{
                  spotFallOff = clamp(spotFallOff, 0.0, 1.0);
              }
        }
    
    //---------------------
    // normal calculations
    //---------------------
    #if defined(NORMALMAP)
      #ifdef TRI_PLANAR_MAPPING
        vec3 normal = calculateNormalTriPlanar(wNormal, wVertex, texCoord);
      #else
        vec3 normal = calculateNormal(texCoord);
      #endif
    #else
      vec3 normal = vNormal;
    #endif


    //-----------------------
    // lighting calculations
    //-----------------------
    vec4 lightDir = vLightDir;
    lightDir.xyz = normalize(lightDir.xyz);

    float light = computeLighting(vPosition, normal, vViewDir.xyz, lightDir.xyz)*spotFallOff;


    //--------------------------
    // final color calculations
    //--------------------------
    gl_FragColor =  AmbientSum * diffuseColor + 
            DiffuseSum * diffuseColor  * light;
    //gl_FragColor = diffuseColor;
    gl_FragColor.a = 1.0;

    #ifdef FOG
    gl_FragColor.rgb = applyFog(gl_FragColor.rgb,vdist,normalize(dir));
    #endif
}

