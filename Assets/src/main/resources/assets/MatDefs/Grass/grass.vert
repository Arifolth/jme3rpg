/*
* vertex shader template
*/

uniform vec4 g_LightPosition;
uniform vec4 g_LightColor;
uniform vec4 g_LightDirection;
uniform mat4 g_ViewProjectionMatrix;

vec3 toDirection(vec4 direction){
    return vec3(direction.x, direction.y, direction.z);
}

vec3 CalcDirLight(vec3 light, vec3 normal, vec3 viewDir)
{
        normal = normalize(normal);
        viewDir = normalize(viewDir);
        light = normalize(-light);
        return vec3(0.5,0.5,0.5) * max(0.0, dot(normal, light));
} 

void main(){ 

    bool toDo = true;
    vec4 modelSpacePos = vec4(modelPosition,1.0);
    vec3 cameraOffset = (cameraPosition - modelPosition);
    /*if(cameraPosition.z - modelPosition.z > 0){
        if((cameraPosition.x - modelPosition.x < texCoord2.y && cameraPosition.x - modelPosition.x > -texCoord2.y)){
            toDo = false;        
        }
    }else{
        if((cameraPosition.x - modelPosition.x > texCoord2.y && cameraPosition.x - modelPosition.x < -texCoord2.y)){
            toDo = false;        
        }
    }*/

    camDist = length(vec2(cameraOffset.x,cameraOffset.z));

    /*if(texCoord2.y > 0 && cameraOffset.x > 0 && toDo){        
        vec3 toAdd = vec3(0,0,0);
        cameraOffset = normalize(cameraOffset);
        float angle = atan(cameraOffset.z, cameraOffset.x); 
        toAdd.z += -cos(angle) * abs(texCoord2.y);
        toAdd.x += sin(angle) * abs(texCoord2.y);
        toAdd.x += abs(texCoord2.y);
        modelSpacePos.xyz += toAdd/2;
    }
    if(texCoord2.y > 0 && cameraOffset.x < 0 && toDo){
        vec3 toAdd = vec3(0,0,0);
        cameraOffset = normalize(cameraOffset);
        float angle = atan(cameraOffset.z, cameraOffset.x);
        toAdd.z += -cos(angle) * abs(texCoord2.y);
        toAdd.x += sin(angle) * abs(texCoord2.y);
        toAdd.x += -abs(texCoord2.y);
        modelSpacePos.xyz += -toAdd/2;
    }
    if(texCoord2.y < 0 && cameraOffset.x > 0 && toDo){
        vec3 toAdd = vec3(0,0,0);
        cameraOffset = normalize(cameraOffset);
        float angle = atan(cameraOffset.z, cameraOffset.x);   
        toAdd.z += -cos(angle) * abs(texCoord2.y);
        toAdd.x += sin(angle) * abs(texCoord2.y);
        toAdd.x += abs(texCoord2.y);
        modelSpacePos.xyz += -toAdd/2;
    }
    if(texCoord2.y < 0 && cameraOffset.x < 0 && toDo){
        vec3 toAdd = vec3(0,0,0);
        cameraOffset = normalize(cameraOffset);
        float angle = atan(cameraOffset.z, cameraOffset.x);
        toAdd.z += sign(texCoord2.y)*cos(angle) * abs(texCoord2.y);
        toAdd.x += sin(angle) * abs(texCoord2.y);
        toAdd.x += -abs(texCoord2.y);
        modelSpacePos.xyz += toAdd/2;
    }*/
    if(cameraPosition.z - modelPosition.z > 0.0){
        if((cameraPosition.x - modelPosition.x < texCoord2.y && cameraPosition.x - modelPosition.x > -texCoord2.y)){
            toDo = false;        
        }
    }else{
        if((cameraPosition.x - modelPosition.x > texCoord2.y && cameraPosition.x - modelPosition.x < -texCoord2.y)){
            toDo = false;        
        }
    }

    if(toDo){
        vec3 toAdd = vec3(0,0,0);
        cameraOffset = normalize(cameraOffset);
        float angle = atan(cameraOffset.z, cameraOffset.x);
        toAdd.z += -cos(angle) * abs(texCoord2.y);
        toAdd.x += sin(angle) * abs(texCoord2.y);
        toAdd.x += sign(cameraOffset.x)*abs(texCoord2.y);
        modelSpacePos.xyz += sign(cameraOffset.x)*sign(texCoord2.y)*toAdd/2.0;
    }
    projPosition = projectionMatrix * viewMatrix * modelSpacePos;
    if(g_LightPosition.w == -1.0){
    /*
        diffuseLight += g_LightColor.rgb * CalcDirLight(g_LightPosition.xyz, modelNormal, cameraDirection);
        WARNING : TEMPORARY FIX
    */
    }
    colorToMix = texCoord2.x;
    minX = texCoord3.x;
    texSize = texCoord3.y;
    texCoord1.x = texCoord1.x*texSize+minX;

}
