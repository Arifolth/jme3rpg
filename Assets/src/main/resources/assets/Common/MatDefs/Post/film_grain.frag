uniform sampler2D Texture; // jME3's default texture name
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;

// Simple noise function
float noise(vec2 uv) {
    return fract(sin(dot(uv * 1000.0, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    // Sample the scene texture
    vec4 color = texture(Texture, texCoord);

    // Generate grain (adjust grainAmount as needed)
    float grainAmount = 0.05 * (sin(Time) + 1.5);
    float grain = noise(texCoord);

    // Apply grain effect
    fragColor = color - grain * grainAmount;
}