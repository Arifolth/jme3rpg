#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D m_ColorMap;
uniform float m_Metallic;
uniform float m_Roughness;
uniform vec4 m_Specular;

varying vec2 texCoord;

void main() {
    // Sample the color from the scrolling texture
    vec4 baseColor = texture2D(m_ColorMap, texCoord);

    // For simplicity, we output the base color directly.
    // You can extend this shader to add lighting, reflections, or other PBR effects.

    // Apply a simple tint to simulate brass metallic look
    vec3 brassTint = vec3(1.0, 0.85, 0.5);
    vec3 finalColor = baseColor.rgb * brassTint;

    gl_FragColor = vec4(finalColor, baseColor.a);
}