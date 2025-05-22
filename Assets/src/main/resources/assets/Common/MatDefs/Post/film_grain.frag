uniform sampler2D Texture;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;

// Random functions from old shader
float rand(float co) {
    return fract(sin(co * 91.3458) * 47453.5453);
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float rand(vec3 co) {
    return rand(co.xy + rand(co.z));
}

// Compute luminance for desaturation
float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

// Parameters you can tweak
uniform float brightness; // around 0.0 means no change, negative to darken, positive to brighten
uniform float contrast;   // 1.0 means no change, <1 to reduce contrast, >1 to increase contrast

vec3 adjustBrightnessContrast(vec3 color, float brightness, float contrast) {
    // Shift color by brightness
    color += brightness;

    // Apply contrast around 0.5 mid-point
    color = (color - 0.5) * contrast + 0.5;

    return clamp(color, 0.0, 1.0);
}

void main() {
    // Normalized pixel coordinates (uv)
    vec2 uv = texCoord; // assuming texCoord in [0,1]

    // Noise strength (adjust for desired grain amount)
    float strength = 0.132;

    // Generate animated noise vector from time and uv
    vec4 noise = vec4(
        rand(vec3(Time * 3.0, uv.x, uv.y)),
        rand(vec3(Time * 1.0, uv.y, uv.x)),
        rand(vec3(Time * 0.3, cos(uv.x), uv.y)),
        1.0
    );

    // Sample scene texture
    vec4 color = texture(Texture, uv);

    // Convert noise to grain effect centered around zero and scaled
    vec3 grainEffect = vec3((noise.r - 0.5) * strength,
        (noise.g - 0.5) * strength,
        (noise.b - 0.5) * strength);

    // Add grain effect to original color (additive)
    vec3 noisyColor = color.rgb + grainEffect;

    // Clamp color to valid range
    noisyColor = clamp(noisyColor, 0.0, 1.0);

    // Slight desaturation for film look
    float lum = luminance(noisyColor);
    vec3 desatColor = mix(noisyColor, vec3(lum), 0.28);

    // Apply tone mapping (Reinhard)
    vec3 toneMapped = desatColor / (desatColor + vec3(1.0));

    // Adjust brightness and contrast
    float brightness = -0.1; // negative to darken slightly
    float contrast = 0.9;    // less than 1 to reduce contrast a bit
    vec3 finalColor = adjustBrightnessContrast(desatColor, brightness, contrast);

    float gamma = 1.2;
    finalColor = pow(finalColor, vec3(1.0 / gamma));

    fragColor = vec4(finalColor, color.a);
}