
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform vec4 g_LightColor;
 
uniform float m_Time;
uniform float m_WindStrength;
uniform vec2 m_WindDirection;
uniform vec3 m_ObjectCenter;
uniform vec3 m_CamPos;
 
uniform sampler2D m_Texture;
uniform sampler2D m_Noise;
uniform vec4 m_Color;
 
attribute vec2 inTexCoord;
 
attribute vec3 inPosition;
 
 //this is erroring
uniform float moveFactor = 0.16; // Play around with this
 
varying vec2 texCoord;
varying vec4 color;
 
#ifdef VERTEX_COLOR
attribute vec4 inColor;
#endif
 
void main()
{
vec3 displacedVertex;
displacedVertex = inPosition;
texCoord = inTexCoord;
 
float len = length( displacedVertex );
 
float noiseCoord = m_Time;
 
int totalTime = int(m_Time);
if (totalTime > 4096) totalTime -= 4096;
 
int pixelY = int(totalTime/64);
int pixelX = totalTime/ -pixelY;
float noiseFactor = texture2D(m_Noise, vec2( pixelX*10, pixelY*10 ) ).r;
// get pixel from noise map based on time. use to create additional variation
 
vec3 wvPosition = (g_WorldViewProjectionMatrix * vec4(displacedVertex, 1.0)).xyz;
 
if(inPosition.y>=0.1)
{
displacedVertex.x += moveFactor * sin(m_Time * texture2D(m_Noise, wvPosition.xz*50.0).r + len) + (m_WindStrength * noiseFactor * m_WindDirection.x)/10.0;
displacedVertex.z += moveFactor * cos(m_Time * texture2D(m_Noise, wvPosition.zx*50.0).r + len) + (m_WindStrength * noiseFactor * m_WindDirection.y)/10.0;
}
 
gl_Position = g_WorldViewProjectionMatrix * vec4(displacedVertex, 1.0);
 
 
#ifdef VERTEX_COLOR
color = m_Color * inColor;
color.rgb *= g_LightColor.rgb;
#else
color = m_Color;
color.rgb *= g_LightColor.rgb;
#endif
 
}