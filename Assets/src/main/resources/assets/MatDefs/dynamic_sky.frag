uniform sampler2D m_glow_texture;
uniform sampler2D m_color_texture;

uniform vec3 m_lightPosition;
varying vec3 vertex;

void main()
{
    vec3 V = normalize(vertex);
    vec3 L = normalize(m_lightPosition.xyz);
    // Compute the proximity of this fragment to the sun.
    float vl = dot(V, L);

    // Look up the sky color and glow colors.
    vec4 Kc = texture2D(m_color_texture, vec2((L.y + 1.0) / 2.0, V.y));
    vec4 Kg = texture2D(m_glow_texture,  vec2((L.y + 1.0) / 2.0, vl));

    // Combine the color and glow giving the pixel value.
    gl_FragColor = vec4(Kc.rgb + Kg.rgb * Kg.a / 2.0, Kc.a);
}
