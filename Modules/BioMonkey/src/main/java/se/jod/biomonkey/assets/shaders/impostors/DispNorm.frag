
uniform sampler2D m_DiffuseMap;

#ifdef NORMALMAP
uniform sampler2D m_NormalMap;
#endif

uniform float m_AlphaDiscardThreshold;

varying vec3 normal;
varying vec3 tangent;
varying vec3 bitangent;
varying vec2 texCoord;
varying float depth;

void main() {
    // If it's a transparent pixel don't include it; otherwise it 
    // will distort the normal value at that point.
    if(texture2D(m_DiffuseMap,texCoord).a < m_AlphaDiscardThreshold){
        discard;
    }
    
    // Rotate the normalmap normal with the tbn matrix to get
    // the actual normal. 
    //
    // To make this clearer - if we have no normal map then we get 
    // the mesh normals as output color.
   
    vec3 norm = normalize(normal);
    vec3 tang = normalize(tangent);
    vec3 bit = normalize(bitangent);

    mat3 tbnMat = mat3(tang, bit, norm);

    vec3 normCol = vec3(0.0,0.0,1.0);
    
    #ifdef NORMALMAP
    normCol = texture2D(m_NormalMap, texCoord).rgb;
    #endif
    //Unpack
    normCol = (normCol - vec3(0.5))*vec3(2.0);
    
    normCol = tbnMat*normCol;

    // Pack it into the regular color ranges again
    // (this is a normal map we're exporting).
    normCol = (normCol.rgb*vec3(0.5)) + vec3(0.5);

    gl_FragColor = vec4(normCol.rgb,1.0);
}

