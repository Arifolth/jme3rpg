#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 m_LandColor;
uniform vec4 m_WaterColor;
uniform vec4 m_MountainColor;
uniform vec4 m_ForestColor;
uniform float m_Contrast;
uniform float m_Saturation;

varying vec2 texCoord;

void main() {
    // Generate procedural terrain-like pattern
    vec2 pos = texCoord * 8.0;

    float noise1 = sin(pos.x * 2.0) * cos(pos.y * 1.5) * 0.5 + 0.5;
    float noise2 = sin(pos.x * 0.8 + 3.14) * cos(pos.y * 1.2 + 1.57) * 0.5 + 0.5;
    float noise3 = sin(pos.x * 1.5 + 6.28) * cos(pos.y * 0.9 + 4.71) * 0.5 + 0.5;

    float heightMap = (noise1 + noise2 * 0.5 + noise3 * 0.25) / 1.75;

    vec4 finalColor;
    if (heightMap < 0.3) {
        finalColor = m_WaterColor;
    } else if (heightMap < 0.5) {
        finalColor = mix(m_LandColor, m_ForestColor, (heightMap - 0.3) * 5.0);
    } else if (heightMap < 0.8) {
        finalColor = m_LandColor;
    } else {
        finalColor = m_MountainColor;
    }

    // Apply Renaissance-style processing
    finalColor.rgb = pow(finalColor.rgb, vec3(1.0 / m_Contrast));

    // Desaturate slightly for aged parchment look
    float gray = dot(finalColor.rgb, vec3(0.299, 0.587, 0.114));
    finalColor.rgb = mix(vec3(gray), finalColor.rgb, m_Saturation);

    gl_FragColor = finalColor;
}
