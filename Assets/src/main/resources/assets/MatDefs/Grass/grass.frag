/*
* fragment shader template
*/

vec3 minGrass = vec3(0, 0, 0);
vec3 maxGrass = vec3(1.0,1.0,1.0);

void main(){
    vec4 base_pixel = texture2D(texture,texCoord);
    vec4 mask_pixel = texture2D(dissolveTexture,texCoord);

    vec3 finalDiffuse = diffuseLight;
    finalDiffuse += vec3(3,3,3);
    finalDiffuse = vec3(mix(minGrass, finalDiffuse.rgb, texCoord.y));
    finalDiffuse = vec3(mix(finalDiffuse.rgb, maxGrass, texCoord.y));

    base_pixel.rgb = base_pixel.rgb * finalDiffuse;
    base_pixel.rgb = base_pixel.rgb * colorToMix;

    float mMult = 1.0 - pow(min(camDist / grassDist, 1.0), 10.0);
    base_pixel.a = base_pixel.a*mMult;
    base_pixel.a = base_pixel.a-min(((mask_pixel.r*((camDist/(grassDist))))),(camDist/grassDist));
    if(base_pixel.a < 0.01){
        discard;
    }
    outColor = base_pixel;
}

