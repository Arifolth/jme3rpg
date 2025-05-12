in vec3 inPosition;
in vec2 inTexCoord;
out vec2 texCoord;

void main() {
    // Map vertex positions from [0,1] to [-1,1] (NDC)
    gl_Position = vec4(inPosition.xy * 2.0 - 1.0, 0.0, 1.0);
    texCoord = inTexCoord;
}