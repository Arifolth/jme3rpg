// Layered shell grass - fragment shader (jME 3.9, GLSL 130).
// Ported from https://github.com/Arifolth/jme3-grass-shader.
// GLSL version differences (outFragColor, texture2D/texture3D -> texture)
// are mapped by the imported Common/ShaderLib/GLSLCompat.glsllib.
//
// Every fragment of a shell pass runs this gate pipeline, in order:
//   1. player-radius fade (fadeFactor) + alpha-cutoff early-outs
//   2. shell-budget LOD: shell i fades out where the shell budget
//      (LodA - LodB * distToPlayer) crosses its index (fixed grid
//      Layer = i / MaxLayers, pushed by GpuGrassMaterial)
//   3. 3D blade mask (one slice per shell, world-anchored maskUV) carves
//      the stacked shells into individual blades
//   4. whole-blade density cull: a stable per-column hash removes blades
//      between DensityStart and DensityEnd world units from the player
//   5. environment gates (unless DebugForceAll): water level + shore band,
//      altitude cutoff, slope, terrain-texture dominance (groundMix)
//   6. alpha cutout (no blending, depth write ON), then terrain-palette
//      coloring: vertical gradient + per-cluster tint + ToneMatch, plus
//      the backlight translucency glow on the upper shells
//
// The shadow-receiving twin LayeredPostShadow.frag replicates steps 1-6
// bit for bit, so shadows land exactly on the drawn blades.

#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_GrassTexture;
uniform sampler2D m_GrassMask;
uniform sampler3D m_ThreeDTex;
uniform float m_MaskScale;
uniform float m_Layer;
uniform float m_WaterLevel;
//bare band above the water level (blades fade in up to WaterLevel+ShoreBand)
uniform float m_ShoreBand;
uniform float m_GrassMaxHeight;
uniform float m_MinGroundUp;
uniform float m_DebugForceAll;
uniform float m_DebugBrightness;
//0.0/1.0: may grass grow on ground the terrain shader paints with NO region
//texture at all (all region weights zero, e.g. the shoreline below h=0)?
uniform float m_AllowOnUntextured;
uniform float m_GrassShareFade;
uniform float m_AlphaCutoff;
//tone pairing with the distant landscape texture (1.0 = raw terrain palette)
uniform float m_ToneMatch;
//whole-blade density culling beyond DensityStart units from the player
uniform float m_DensityStart;
uniform float m_DensityEnd;
uniform float m_DensityMin;

//continuous shell-budget LOD. budget(d) = LodA - LodB * d = the number of
//shells the field shows at d world units from the player (full MaxLayers
//inside the inner LOD circle, down to MaxLayers * MIN_LAYERS_RATIO at
//GrassDistance). Shell i (on the fixed grid Layer = i / MaxLayers) fades
//out over the band where the budget crosses i (band width = 1/LodB world
//units). The CPU renders a
//matching per-patch prefix of the fixed grid, skipping only shells that
//are already fully faded (see GpuGrassMaterial.adaptiveLayerCount).
uniform float m_LodA;
uniform float m_LodB;
uniform float m_MaxLayers;

//Backlight translucency (sun through the thin blade tips). All values are
//CPU-computed per frame in GpuGrassMaterial (setSharedSunDirection):
//  m_TransSunH     - glow alignment axis (sun direction + elevation floor
//                    + distortion bias);
//  m_TransSunColor - warm sun tint of the glow;
//  m_TransStrength - overall strength incl. the day-night fade (0 = off);
//  m_TransPower    - sharpness of the view alignment response;
//  m_TransTipStart - shell height where the tip glow starts fading in.
uniform vec3 m_TransSunH;
uniform vec3 m_TransSunColor;
uniform float m_TransStrength;
uniform float m_TransPower;
uniform float m_TransTipStart;

varying vec2 texCoord;
varying vec2 groundUV;
varying float fadeFactor;
varying float worldHeight;
varying float groundUp;
varying float grassTint;
//x = grass region weight, y = dirt region weight, z = 2x rock region weight,
//w = slope-rock share of the terrain color (see Layered.vert)
varying vec4 groundMix;
//Backlight translucency: direction from the shell to the camera
//(per-vertex, see Layered.vert; normalized per fragment below).
varying vec3 viewDirW;
//(see Layered.vert)
varying vec2 maskUV;
varying float distToPlayer;

// Integer-free hash (same function as in Layered.vert): stable
// per-column random values for the density culling.
float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

//mask texture resolution (GrassTextureFactory MASK_SIZE) and the
//density hash cell size in mask texels (~ one blade dot footprint)
const float MASK_TEXELS = 200.0;
const float MASK_CELL_TEXELS = 3.0;

void main() {
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

    //Shell budget of this fragment (see the uniform comment above). Shell
    //index = m_Layer * MaxLayers on the fixed shell grid; the base ground
    //pass (m_Layer == 0) always passes. Discarding here skips the mask
    //fetch for every fully-faded shell, so the prefix shells the CPU still
    //renders for far patches cost almost no fragment work.
    float shellFade = clamp(m_LodA - m_LodB * distToPlayer
            - m_Layer * m_MaxLayers + 1.0, 0.0, 1.0);
    if (shellFade < m_AlphaCutoff) {
        discard;
    }

    // The 3D mask carves the stacked shells into individual grass blades.
    // It is ALWAYS applied (also in debug mode): without it every shell
    // renders as a solid surface and the stack looks like a wall.
#ifdef THREEDTEX
    // explicit LOD 0: the grass textures use BilinearNoMipMaps filtering
    // (GrassTextureFactory), so this returns the exact same texel as the
    // implicit-LOD fetch - without the screen-space derivative computation
    // on the hottest fetch in the shader (most fragments discard right
    // after it).
    float sampledAlpha = textureLod(m_ThreeDTex, vec3(maskUV, m_Layer), 0.0).a;
#else
    float sampledAlpha = texture2D(m_GrassMask, texCoord * m_MaskScale).a;
#endif
    if (sampledAlpha == 0.0) {
        discard;
    }

    //Distance density falloff: cull WHOLE blades beyond DensityStart world
    //units from the player. One hash cell ~= one blade dot footprint; the
    //nested mask slices keep every blade's dots at the same (x, y), so the
    //cell hash is constant per blade column and whole blades vanish (no
    //per-shell flicker, pattern frozen in world space).
    float densityKeep = mix(1.0, m_DensityMin,
            smoothstep(m_DensityStart, max(m_DensityEnd, m_DensityStart + 0.001), distToPlayer));
    if (densityKeep < 1.0) {
        vec2 maskCell = floor(maskUV * MASK_TEXELS / MASK_CELL_TEXELS);
        if (hash13(vec3(maskCell, 7.0)) > densityKeep) {
            discard;
        }
    }

    float maskAlpha = sampledAlpha;

    if (m_DebugForceAll <= 0.5) {
        // Altitude gating: below the water level (with a small smooth
        // shoreline transition) and on hills/mountains above the grass
        // line - no grass at all.
        if (worldHeight < m_WaterLevel - 3.0 || worldHeight > m_GrassMaxHeight) {
            discard;
        }

        // Steep terrain (cliffs, mountainsides) - no grass.
        if (groundUp < m_MinGroundUp) {
            discard;
        }

        // Smooth transitions: blades thin out towards the shoreline (an
        // (almost) bare band of ShoreBand height above the water level,
        // Morrowind style) and towards the grass line.
        float heightFade = smoothstep(m_WaterLevel, m_WaterLevel + max(m_ShoreBand, 0.0001), worldHeight)
                * (1.0 - smoothstep(m_GrassMaxHeight - 15.0, m_GrassMaxHeight, worldHeight));

        // Texture gate: grass grows wherever NO other terrain texture
        // outweighs the grass texture on this ground texel (the weights
        // were replicated 1:1 from the HeightBasedTerrain fragment shader
        // in the vertex shader):
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

    //Continuous distance LOD: the shell fades out where the budget crosses
    //its index (see the uniform comment above).
    maskAlpha *= shellFade;

    // Blades thin out towards the edge of the grass radius (always enforced).
    maskAlpha = min(maskAlpha, fadeFactor);

    // ALPHA CUTOUT: hard discard instead of blending. The surviving blades
    // write depth, so scene objects sort against them per fragment.
    if (maskAlpha < m_AlphaCutoff) {
        discard;
    }

    // The very same texture the terrain's grass region shows, with the very
    // same UV mapping -> pixel-perfect palette match with the landscape.
    // explicit LOD 0 - same bit-exact texel for BilinearNoMipMaps (see above).
    vec4 color = textureLod(m_GrassTexture, groundUV, 0.0);

    // Vertical gradient: darker near the ground, slightly brighter at the
    // tips (stems 0.74, tips 1.02..1.14 x ToneMatch), with a per-blade-
    // cluster brightness variation. This keeps the blades readable against
    // the identically colored ground and puts the blade tone inside the
    // distant landscape tone (the terrain reads ~0.73x of its raw texels
    // through slope blending + shadows).
    color.rgb *= mix(0.74, 1.02 + grassTint * 0.12, m_Layer) * m_ToneMatch;

    //Brightness multiplier (1.0 = exactly the terrain palette; raise it if
    //the grass should stay visible on very dark nights).
    color.rgb *= m_DebugBrightness;

    // ------------------------------------------------------------------
    // BACKLIGHT TRANSLUCENCY: sun shining through the thin blade tips.
    // Additive warm term, concentrated on the UPPER shells (tip mask) and
    // strongest when looking towards the sun (view alignment):
    //  - align = pow(max(dot(V, -H), 0), power): H is the CPU-computed glow
    //    axis (sun direction with an elevation floor + distortion bias), so
    //    the response stays alive around noon;
    //  - tip = smoothstep over the shell height (m_Layer 0 = ground shell,
    //    1 = top shell): only the upper part of every blade lights up - the
    //    field keeps its color, the tips catch the light;
    //  - (1 - color.rgb): thin/light texels transmit more light than dense
    //    dark ones.
    // m_TransStrength carries the day-night fade, so a below-horizon sun
    // lights nothing. PSSM shadows apply automatically: this pass renders
    // before the shadow post pass, which modulates the whole pixel (glow
    // included) by the shadow factor afterwards.
    // Cost: ~15 ALU per surviving blade fragment, no new texture fetches.
    // ------------------------------------------------------------------
    if (m_TransStrength > 0.0) {
        vec3 viewDir = viewDirW / max(length(viewDirW), 0.0001);
        float align = max(dot(viewDir, -m_TransSunH), 0.0);
        float tip = smoothstep(min(m_TransTipStart, 0.999), 1.0, m_Layer);
        color.rgb += pow(align, m_TransPower) * (tip * m_TransStrength)
                * m_TransSunColor * (vec3(1.0) - color.rgb);
    }

    //No blending (alpha cutout): alpha is irrelevant, keep it opaque.
    gl_FragColor = vec4(color.rgb, 1.0);
}
