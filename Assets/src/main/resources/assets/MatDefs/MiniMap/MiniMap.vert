#import "Common/ShaderLib/Instancing.glsllib"

in vec3 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main()
{
    texCoord = inTexCoord;
    vec4 modelSpacePos = vec4(inPosition, 1.0);

    // Assuming TransformWorldViewProjection is a function defined in Instancing.glsllib
    gl_Position = TransformWorldViewProjection(modelSpacePos);
}
