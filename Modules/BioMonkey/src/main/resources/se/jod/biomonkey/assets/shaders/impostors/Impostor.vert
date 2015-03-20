uniform mat4 g_WorldMatrix;
uniform mat4 g_WorldViewProjectionMatrix;
uniform vec3 g_CameraPosition;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
//xy = vertice position offset, z = texture rotation offset.
attribute vec3 inTexCoord2;

varying vec4 texCoords;
varying vec2 noiseTexCoords;

#ifdef FOG
varying float vdist;
varying vec3 dir;
#endif

varying vec3 normal;
varying float fadeVal;
varying float angleFract;
varying vec4 worldPos;

const float PI = 3.1415927;
const float TWO_PI_INV = 0.1591549;

vec2 mapOffset(float number){
    float offsetX = mod(number,4.0);
    number = mod(number,8.0);
    float offsetY = floor(number * 0.25);
    return vec2(offsetX,offsetY);
}

//Get the angle between v = (0,-1) and an xz vector.
//The angle is on the form 0 <= angle <= 2*PI.
float getAngle(vec2 norm){
    return atan(norm.y,norm.x) + 1.5*PI;
}

void main() {
    
    vec4 pos = vec4(inPosition,1.0);
    worldPos = vec4(g_WorldMatrix*pos);
    vec2 CtoP = worldPos.xz - g_CameraPosition.xz;
    
    fadeVal = length(CtoP);
    
    #ifdef FOG
    vec3 ray = worldPos.xyz - g_CameraPosition;
    vdist = length(ray);
    dir = ray/vdist;
    #endif

    //This is the mesh normal everywhere (since it's a camera-aligned quad)
    vec2 norm = -CtoP/fadeVal;
    //Add the modified position to the original point using
    //the "normal of the normal".
    pos.xyz += vec3(norm.y*inTexCoord2.x,inTexCoord2.y,-norm.x*inTexCoord2.x);
    
    //To get the angle relative to the positive z axis, dot the normal.
    float angle = getAngle(norm) + inTexCoord2.z;
    
    // The angle is scaled to [0,8) and shifted some to make the images
    // appear correctly, and make sure the angle is larger then 0.
    float angleNorm = mod(angle*8.0*TWO_PI_INV + 8.5,8.0);
    //Which texture ordinal does this angle correspond to?
    float angleOrd = floor(angleNorm);
    //How far "within" the particular textures angle-range is the camera [0,1)?
    angleFract = fract(angleNorm);

    texCoords.xy = (inTexCoord + mapOffset(angleOrd))*vec2(0.25);
    texCoords.zw = (inTexCoord + mapOffset(angleOrd + 1.0))*vec2(0.25);
    noiseTexCoords = inTexCoord;
    normal = vec3(norm.x,0.0,norm.y);

    gl_Position = g_WorldViewProjectionMatrix*pos;
}