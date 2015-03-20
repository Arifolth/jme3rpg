#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/PssmShadows15.glsllib"


uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;
uniform mat4 m_ViewProjectionMatrixInverse;
uniform vec4 m_ViewProjectionMatrixRow2;

in vec2 texCoord;
out vec4 outFragColor;

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);

uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;


vec3 getPosition(in float depth, in vec2 uv){
    vec4 pos = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    pos = m_ViewProjectionMatrixInverse * pos;
    return pos.xyz / pos.w;
}

vec4 main_multiSample(in int numSample){
    float depth = fetchTextureSample(m_DepthTexture,texCoord,numSample).r;//getDepth(m_DepthTexture,texCoord).r;
    vec4 color = fetchTextureSample(m_Texture,texCoord,numSample);

    //Discard shadow computation on the sky
    if(depth == 1.0){        
        return color;
    }
    
    // get the vertex in world space
    vec4 worldPos = vec4(getPosition(depth,texCoord),1.0);
  
    // populate the light view matrices array and convert vertex to light viewProj space
    vec4 projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    vec4 projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    vec4 projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    vec4 projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;

    float shadowPosition = m_ViewProjectionMatrixRow2.x * worldPos.x +  m_ViewProjectionMatrixRow2.y * worldPos.y +  m_ViewProjectionMatrixRow2.z * worldPos.z +  m_ViewProjectionMatrixRow2.w;
  
float shadow = 0.0;
    if(shadowPosition < m_Splits.x){
        shadow = GETSHADOW(m_ShadowMap0, projCoord0);
    }else if( shadowPosition <  m_Splits.y){
        shadowBorderScale = 0.5;
        shadow = GETSHADOW(m_ShadowMap1, projCoord1);
    }else if( shadowPosition <  m_Splits.z){
        shadowBorderScale = 0.25;
        shadow = GETSHADOW(m_ShadowMap2, projCoord2);
    }else if( shadowPosition <  m_Splits.w){
        shadowBorderScale = 0.125;
        shadow = GETSHADOW(m_ShadowMap3, projCoord3);
    }
    
    shadow= shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
  
    return color * vec4(shadow, shadow, shadow, 1.0);
}

void main(){  

    #ifdef RESOLVE_MS
        vec4 color = vec4(0.0);
        for (int i = 0; i < m_NumSamples; i++){
            color += main_multiSample(i);
        }
        outFragColor = color / m_NumSamples;
    #else
        outFragColor = main_multiSample(0);
    #endif  

}