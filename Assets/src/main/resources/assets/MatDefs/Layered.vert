#extension GL_EXT_gpu_shader4 : enable

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;
attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;
uniform float m_Length;
uniform float m_Time;
uniform float m_WaveSpeed;
uniform float m_WaveSize;
uniform float m_MaskScale;
uniform sampler2D m_GrassMask;
uniform float m_Layer;
varying vec3 normal;
varying vec2 texCoord;
varying float distFromCam;
uniform mat4 g_WorldMatrix;
uniform vec3 g_CameraPosition;
uniform float m_GrassDistance;

varying float lightIntensity;
uniform vec4 g_LightPosition;

#ifdef ALPHAMAP
varying float alphaVal;
uniform sampler2D m_AlphaMap;
#endif

#ifdef WAVE
uniform bool m_Wave;
#endif

int LFSR_Rand_Gen(in int n)
{
  // <<, ^ and & require GL_EXT_gpu_shader4.
  n = (n << 13) ^ n;
  return (n * (n*n*15731+789221) + 1376312589) & 0x7fffffff;
}

float LFSR_Rand_Gen_f(int n )
{
  return float(LFSR_Rand_Gen(n));
}


float noise(vec3 p) {
  ivec3 ip = ivec3(floor(p));
  vec3 u = fract(p);
  u = u*u*(3.0-2.0*u);

  int n = ip.x + ip.y*57 + ip.z*113;

  float res = mix(mix(mix(LFSR_Rand_Gen_f(n+(0+57*0+113*0)),
                          LFSR_Rand_Gen_f(n+(1+57*0+113*0)),u.x),
                      mix(LFSR_Rand_Gen_f(n+(0+57*1+113*0)),
                          LFSR_Rand_Gen_f(n+(1+57*1+113*0)),u.x),u.y),
                 mix(mix(LFSR_Rand_Gen_f(n+(0+57*0+113*1)),
                          LFSR_Rand_Gen_f(n+(1+57*0+113*1)),u.x),
                      mix(LFSR_Rand_Gen_f(n+(0+57*1+113*1)),
                          LFSR_Rand_Gen_f(n+(1+57*1+113*1)),u.x),u.y),u.z);

  return 1.0 - res*(1.0/1073741824.0);
}

/*float lightComputeDiffuse(in vec3 norm, in vec3 lightdir) {
      return max(0.0, dot(norm, lightdir));
}

void lightComputeDir(in vec3 worldPos, in vec4 color, in vec4 position, out vec4 lightDir) {
    float posLight = step(0.5, color.w);
    vec3 tempVec = position.xyz * sign(posLight - 0.5) - (worldPos * posLight);
    lightDir = vec4(normalize(tempVec), 1.0);
}

vec2 computeLighting(in vec3 wvPos, in vec3 wvNorm, in vec3 wvViewDir, in vec4 wvLightPos) {
     vec4 lightDir;

     lightComputeDir(wvPos, g_LightColor, wvLightPos, lightDir);

     float diffuseFactor = lightComputeDiffuse(wvNorm, lightDir.xyz);
     float specularFactor = 0.0;
     return vec2(diffuseFactor, specularFactor) * vec2(lightDir.w);
}*/

void main(){

    vec3 P = inPosition;
    texCoord = inTexCoord;
    vec4 pos = vec4(inPosition, 1.0);

    vec4 worldPos = g_WorldMatrix * pos;
    distFromCam = distance(g_CameraPosition.xyz, worldPos.xyz);
    //if (distFromCam > m_GrassDistance) {return;}

#ifdef ALPHAMAP
    alphaVal = (texture2D(m_AlphaMap, texCoord.xy)).a;
#ifdef FUR
    P = inPosition + (inNormal * ((m_Length*alphaVal)*m_Layer));
#else
    P = inPosition + (vec3(0.0, 1.0, 0.0) * ((m_Length*alphaVal)*m_Layer));
#endif
#else
#ifdef FUR
    P = inPosition + (inNormal * (m_Length*m_Layer));
#else
    P = inPosition + (vec3(0.0, 1.0, 0.0) * (m_Length*m_Layer));
#endif
#endif

#ifdef WAVE
    //Use a noise function to choose a movement group
    float gravGroup = noise(inPosition);

    //cycle the bend factor based on the total time
    //THIS LINE CAUSES THE WAVING EFFECT!  Tweak this depending on your application.
    float bendAmt = sin((m_Time + gravGroup)*m_WaveSpeed);

    vec3 vGravity = vec3(0,0,0);
    
    if (gravGroup < 0.3) {
       vGravity = ((vec4(((-0.1)+(bendAmt*0.1*m_WaveSize)),0,0,0))*g_WorldViewMatrix).xyz;
    } else if (gravGroup < 0.6) {
       vGravity = ((vec4(((0.1)+(bendAmt*0.1*m_WaveSize)),0,0,0))*g_WorldViewMatrix).xyz;
    } else {
       vGravity = ((vec4(0,0,((-0.1)+(bendAmt*0.1*m_WaveSize)),0))*g_WorldViewMatrix).xyz;
    }
#else
    vec3 vGravity = ((vec4(-0.1,0,0,0))*g_WorldViewMatrix).xyz;
#endif

    float k =  pow(m_Layer, 4.0);  // The higher the exponent is, the closer to the tip the grass will start curving
    P = P + vGravity*k;

   gl_Position = g_WorldViewProjectionMatrix * vec4(P, 1.0);

   //LIGHTING STUFF:
   vec3 vNormal = normalize(g_NormalMatrix * inNormal);
   vec3 wvPosition = vec3(g_WorldViewMatrix * vec4(P, 1.0));
   //vec3 wvPosition = vec3(g_WorldViewMatrix * vec4(P, 1.0));
   vec3 lightDir =  normalize(g_LightPosition.xyz - wvPosition);
   lightIntensity = dot(lightDir,normalize(vNormal));
}