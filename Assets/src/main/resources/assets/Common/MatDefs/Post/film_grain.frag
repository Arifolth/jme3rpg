uniform sampler2D Texture;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;

// 3D noise with time as z dimension
float noise(vec3 p) {
    return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 color = texture(Texture, texCoord);

    // Scale and animate noise coordinates
    float grainScale = 1000.0;
    vec3 p1 = vec3(texCoord * grainScale, fract(Time * 0.5));
    vec3 p2 = vec3(texCoord * grainScale * 1.5, fract(Time * 0.7 + 0.5));
    vec3 p3 = vec3(texCoord * grainScale * 2.0, fract(Time * 0.9 + 0.8));

    // Combine multiple noise layers for natural grain
    float grain = (noise(p1) + noise(p2) + noise(p3)) / 3.0;

    // Center grain around zero and scale intensity
    float grainAmount = 0.085;
    vec3 noisyColor = color.rgb + (grain - 0.5) * grainAmount;

    noisyColor = clamp(noisyColor, 0.0, 1.0);

    // Slight desaturation for film look
    float lum = luminance(noisyColor);
    vec3 desatColor = mix(noisyColor, vec3(lum), 0.28);

    fragColor = vec4(desatColor, color.a);
}