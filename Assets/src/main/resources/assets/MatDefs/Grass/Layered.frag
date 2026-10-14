// Layered shell grass - fragment shader.
// Ported from https://github.com/Arifolth/jme3-grass-shader (GLSL 1.10/1.30 era)
// to jMonkeyEngine 3.9.0-stable.
//
// Key changes versus the original shader:
//  - imports Common/ShaderLib/GLSLCompat.glsllib (provides the outFragColor
//    declaration and the texture2D/texture3D -> texture mappings for any
//    GLSL version the engine selects);
//  - the hard distance discard was replaced by a smooth density fade
//    (fadeFactor computed in the vertex shader from the player position);
//  - ANJRpg specific gating (Phase 1 + Phase 2):
//      * grass is discarded under the water level (m_WaterLevel = -70);
//      * grass is discarded on hills/mountains (above m_GrassMaxHeight)
//        and thins out smoothly below that cutoff;
//      * grass is discarded on steep slopes (m_MinGroundUp threshold on
//        the world-space up component of the ground normal);
//  - DEBUG MODE (m_DebugForceAll = 1): the environment gates (water level,
//    altitude, slope) are bypassed - grass is drawn everywhere inside the
//    player radius, exactly the behaviour of the original example project.
//    The 3D blade mask is ALWAYS applied, in debug mode too: it is what
//    carves the shells into individual blades (skipping it renders every
//    shell as a solid wall of ground - that was the "grass wall" bug).
//  - TEXTURE GATE (Phase 3): the vertex shader replicates the exact
//    HeightBasedTerrain region weights (grass/dirt/rock height ranges + the
//    slope blend) and passes them in the groundMix varying. Grass grows
//    unless one of the OTHER textures outweighs the grass texture on the
//    ground below: dirt region wins -> dirt/sand band, no grass; rock region
//    wins -> no grass; slope blend wins -> rocky cliffs, no grass. Ground
//    the stock shader paints with NO region texture at all (this game's
//    shoreline below height 0 with regions grass 15..200 / dirt 0..20 /
//    rock 198..260) is grass-eligible when AllowOnUntextured is on.
//    Result: no grass on dirt, sand or rock - but grass everywhere else,
//    including the lowlands the player actually walks on.
//  - ALPHA CUTOUT: fragments below m_AlphaCutoff are discarded and the
//    material renders WITHOUT blending and WITH depth write. The blades
//    therefore occupy real depth buffer space: the player, trees and all
//    other objects interleave with the grass correctly (some blades in
//    front of the player's legs, some behind them).
//  - COLOR: the shells sample the terrain's own grass region texture with
//    the exact UV mapping of the terrain shader, so the grass palette is
//    the landscape palette. A vertical gradient (darker stems, lighter
//    tips) plus a per-blade-cluster tint keep the blades distinguishable
//    from the ground.

#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_GrassTexture;
uniform sampler2D m_GrassMask;
uniform sampler3D m_ThreeDTex;
uniform float m_MaskScale;
uniform float m_Layer;
uniform float m_WaterLevel;
uniform float m_GrassMaxHeight;
uniform float m_MinGroundUp;
uniform float m_DebugForceAll;
uniform float m_DebugBrightness;
//0.0/1.0: may grass grow on ground the terrain shader paints with NO region
//texture at all (all region weights zero, e.g. the shoreline below h=0)?
uniform float m_AllowOnUntextured;
uniform float m_GrassShareFade;
uniform float m_AlphaCutoff;

varying vec2 texCoord;
varying vec2 groundUV;
varying float fadeFactor;
varying float worldHeight;
varying float groundUp;
varying float grassTint;
//x = grass region weight, y = dirt region weight, z = 2x rock region weight,
//w = slope-rock share of the terrain color (see Layered.vert)
varying vec4 groundMix;

void main() {
    // Beyond the grass radius around the player - always enforced.
    if (fadeFactor <= 0.0) {
        discard;
    }

    // The 3D mask carves the stacked shells into individual grass blades.
    // It is ALWAYS applied (also in debug mode): without it every shell
    // renders as a solid surface and the stack looks like a wall.
#ifdef THREEDTEX
    float sampledAlpha = texture3D(m_ThreeDTex, vec3(texCoord * m_MaskScale, m_Layer)).a;
#else
    float sampledAlpha = texture2D(m_GrassMask, texCoord * m_MaskScale).a;
#endif
    if (sampledAlpha == 0.0) {
        discard;
    }

    float maskAlpha = sampledAlpha;

    if (m_DebugForceAll <= 0.5) {
        // Phase 2: altitude gating.
        // Below the water level (with a small smooth shoreline transition)
        // and on hills/mountains above the grass line - no grass at all.
        if (worldHeight < m_WaterLevel - 3.0 || worldHeight > m_GrassMaxHeight) {
            discard;
        }

        // Steep terrain (cliffs, mountainsides) - no grass.
        if (groundUp < m_MinGroundUp) {
            discard;
        }

        // Smooth transitions: blades thin out towards the shoreline and
        // towards the grass line.
        float heightFade = smoothstep(m_WaterLevel - 3.0, m_WaterLevel + 3.0, worldHeight)
                * (1.0 - smoothstep(m_GrassMaxHeight - 15.0, m_GrassMaxHeight, worldHeight));

        // Phase 3: texture gate. Grass grows wherever NO other terrain
        // texture outweighs the grass texture on this ground texel (the
        // weights were replicated 1:1 from the HeightBasedTerrain fragment
        // shader in the vertex shader):
        //  - dirt region weight wins  -> dirt/sand band: no grass;
        //  - rock region weight wins  -> rock band: no grass;
        //  - slope-rock share wins    -> cliffs painted with the slope rock
        //                                texture: no grass.
        // Each test fades smoothly over the GrassShareFade band, so blades
        // thin out gradually where one texture takes over another.
        float band = max(m_GrassShareFade, 0.0001);
        float texFade = (1.0 - smoothstep(0.0, band, groundMix.y - groundMix.x))
                      * (1.0 - smoothstep(0.0, band, groundMix.z - groundMix.x))
                      * (1.0 - smoothstep(0.0, band, groundMix.w - groundMix.x));

        if (m_AllowOnUntextured < 0.5) {
            // Strict mode: ground the terrain paints with NO region texture
            // (all weights zero) stays bare as well.
            texFade *= smoothstep(0.0, 0.01, groundMix.x);
        }

        maskAlpha = sampledAlpha * heightFade * texFade;
    }

    // Blades thin out towards the edge of the grass radius (always enforced).
    maskAlpha = min(maskAlpha, fadeFactor);

    // ALPHA CUTOUT: hard discard instead of blending. The surviving blades
    // write depth, so scene objects sort against them per fragment.
    if (maskAlpha < m_AlphaCutoff) {
        discard;
    }

    // The very same texture the terrain's grass region shows, with the very
    // same UV mapping -> pixel-perfect palette match with the landscape.
    vec4 color = texture2D(m_GrassTexture, groundUV);

    // Vertical gradient: darker near the ground, slightly brighter at the
    // tips, with a per-blade-cluster brightness variation. This keeps the
    // blades readable against the identically colored ground.
    color.rgb *= mix(0.66, 1.06 + grassTint * 0.16, m_Layer);

    //Brightness multiplier (1.0 = exactly the terrain palette; raise it if
    //the grass should stay visible on very dark nights).
    color.rgb *= m_DebugBrightness;

    //No blending (alpha cutout): alpha is irrelevant, keep it opaque.
    gl_FragColor = vec4(color.rgb, 1.0);
}
