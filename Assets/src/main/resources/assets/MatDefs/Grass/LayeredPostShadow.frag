// Layered shell grass - POST SHADOW (shadow RECEIVING) fragment shader.
// Used by the 'PostShadow' technique of Layered.j3md; rendered by the
// PssmShadowRenderer receive pass with Blend Modulate, DepthWrite Off and
// a small PolyOffset. Outputs the PSSM shadow factor, which multiplies the
// already rendered scene color.
//
// The blade pass (m_Layer > 0) replicates the MAIN pass gating of
// Layered.frag EXACTLY (3D mask, alpha cutout, water/shore band, altitude,
// slope, texture dominance, radius fade). Because of that, only fragments
// that actually drew a blade in the main pass survive here, and the depth
// test delivers exactly the shell that owns the pixel -> every blade and
// every ground texel is darkened exactly once.
//
// The base pass (m_Layer == 0) skips the mask entirely: it darkens the bare
// GROUND (the terrain the grass grows on) - in the main pass the ground is
// drawn unmasked as well.
//
// Shadow sampling comes from the engine's Common/ShaderLib/Shadows.glsllib
// (same code the stock PostShadow material uses: PSSM split selection,
// hardware/software compare, PCF edge filtering).

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Shadows.glsllib"

#if defined(PSSM) || defined(FADE)
varying float shadowPosition;
#endif

varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

varying vec2 texCoord;
varying float fadeFactor;
varying float worldHeight;
varying float groundUp;
varying vec4 groundMix;

//0 = base (ground) pass, > 0 = blade shell pass
uniform float m_Layer;

//blade mask + placement gates (same values as the main pass)
uniform float m_MaskScale;
uniform float m_DebugForceAll;
uniform float m_WaterLevel;
//bare band above the water level (blades fade in up to WaterLevel+ShoreBand)
uniform float m_ShoreBand;
uniform float m_GrassMaxHeight;
uniform float m_MinGroundUp;
uniform float m_AllowOnUntextured;
uniform float m_GrassShareFade;
uniform float m_AlphaCutoff;
#ifdef THREEDTEX
uniform sampler3D m_ThreeDTex;
#else
uniform sampler2D m_GrassMask;
#endif

#ifdef FADE
uniform vec2 m_FadeInfo;
#endif

void main() {
    if (m_Layer > 0.0) {
        // --------------------------------------------------------------
        // BLADE SHELL PASS - replicate Layered.frag's discard decisions
        // bit for bit (see that file for the full commentary).
        // --------------------------------------------------------------
        // Beyond the grass radius around the player - always enforced.
        if (fadeFactor <= 0.0) {
            discard;
        }

        // Performance: every fragment with fadeFactor below the alpha cutoff dies
        // by the final "min(maskAlpha, fadeFactor) < AlphaCutoff" test anyway
        // (maskAlpha is always >= 0), so it can be discarded HERE - before the 3D
        // mask fetch and the fade math. Same set of surviving fragments, same
        // pixels: strictly less work per discarded fragment.
        if (fadeFactor < m_AlphaCutoff) {
                discard;
        }

        // The 3D mask carves the stacked shells into individual grass blades.
#ifdef THREEDTEX
    // explicit LOD 0: the grass textures use BilinearNoMipMaps filtering
    // (GrassTextureFactory), so this returns the exact same texel as the
    // implicit-LOD fetch - without the screen-space derivative computation
    // on the hottest fetch in the shader (most fragments discard right
    // after it).
        float sampledAlpha = textureLod(m_ThreeDTex, vec3(texCoord * m_MaskScale, m_Layer), 0.0).a;
#else
        float sampledAlpha = texture2D(m_GrassMask, texCoord * m_MaskScale).a;
#endif
        if (sampledAlpha == 0.0) {
            discard;
        }

        float maskAlpha = sampledAlpha;

        if (m_DebugForceAll <= 0.5) {
            // Phase 2: altitude gating.
            if (worldHeight < m_WaterLevel - 3.0 || worldHeight > m_GrassMaxHeight) {
                discard;
            }

            // Steep terrain (cliffs, mountainsides) - no grass.
            if (groundUp < m_MinGroundUp) {
                discard;
            }

            // Smooth transitions: shoreline bare band + grass line.
            float heightFade = smoothstep(m_WaterLevel, m_WaterLevel + max(m_ShoreBand, 0.0001), worldHeight)
                    * (1.0 - smoothstep(m_GrassMaxHeight - 15.0, m_GrassMaxHeight, worldHeight));

            // Phase 3: texture gate (weights replicated in the vertex shader).
            float band = max(m_GrassShareFade, 0.0001);
            float texFade = (1.0 - smoothstep(0.0, band, groundMix.y - groundMix.x))
                          * (1.0 - smoothstep(0.0, band, groundMix.z - groundMix.x))
                          * (1.0 - smoothstep(0.0, band, groundMix.w - groundMix.x));

            if (m_AllowOnUntextured < 0.5) {
                texFade *= smoothstep(0.0, 0.01, groundMix.x);
            }

            maskAlpha = sampledAlpha * heightFade * texFade;
        }

        // Blades thin out towards the edge of the grass radius.
        maskAlpha = min(maskAlpha, fadeFactor);

        // Same alpha cutout as the main pass: pixels discarded there have
        // ground depth in the depth buffer and MUST be skipped here too,
        // otherwise the ground would be darkened twice.
        if (maskAlpha < m_AlphaCutoff) {
            discard;
        }
    }

    // --------------------------------------------------------------
    // PSSM shadow factor (engine PostShadow.frag math).
    // --------------------------------------------------------------
    float shadow = 1.0;

#ifdef PSSM
    shadow = getDirectionalLightShadows(m_Splits, shadowPosition,
                   m_ShadowMap0, m_ShadowMap1, m_ShadowMap2, m_ShadowMap3,
                   projCoord0, projCoord1, projCoord2, projCoord3);
#endif

#ifdef FADE
    shadow = max(0.0, mix(shadow, 1.0, max(0.0, (shadowPosition - m_FadeInfo.x) * m_FadeInfo.y)));
#endif

    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    gl_FragColor = vec4(shadow, shadow, shadow, 1.0);
}
