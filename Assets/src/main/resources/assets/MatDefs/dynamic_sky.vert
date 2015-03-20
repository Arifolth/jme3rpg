uniform mat4 g_ViewMatrix;
uniform mat4 g_ProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;

varying vec3 vertex;

void main()
{
    vertex      = inPosition.xyz;
    
    vec4 pos = vec4(inPosition, 0.0);
    pos = g_ViewMatrix * pos;
    pos.w = 1.0;
    gl_Position = g_ProjectionMatrix * pos;
    //gl_Position = ftransform(); (this was in the sample)
}
