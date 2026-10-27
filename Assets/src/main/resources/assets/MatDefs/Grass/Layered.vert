// Layered shell grass - vertex shader.
// Ported from https://github.com/Arifolth/jme3-grass-shader (GLSL 1.10/1.30 era)
// to jMonkeyEngine 3.9.0-stable.
//
// Key changes versus the original shader:
//  - imports Common/ShaderLib/GLSLCompat.glsllib so the same source compiles
//    cleanly for any GLSL version the engine selects (attribute/varying/
//    texture2D/gl_FragColor are mapped by the compat layer);
//  - the GL_EXT_gpu_shader4 extension was removed: the random noise was
//    rewritten with extension-free float math (int bit operations are the
//    reason the original failed to compile on modern drivers / Mesa);
//  - grass is now centered on the PLAYER (m_PlayerPosition) instead of the
//    camera and fades out smoothly between m_FadeStart and m_GrassDistance;
//  - world height and the world-space ground normal are passed to the
//    fragment shader so grass can be restricted to flat green lowlands
//    (no grass on hills/mountains and under the water level);
//  - the per-light vertex lighting was removed: the original computed
//    lightIntensity from g_LightPosition but the fragment shader never used it.
//  - ANJRpg Phase 3: the vertex shader replicates the EXACT texture region
//    weights of the stock HeightBasedTerrain fragment shader (same linear
//    region falloff over world height, same model-space-normal slope blend)
//    and passes them to the fragment shader (varying groundMix), so grass
//    only grows where the terrain actually shows the grass texture - or no
//    region texture at all - and never where dirt, rock or slope-rock wins.
//  - it also computes the same texture UV the terrain shader uses for the
//    grass region (worldPos.xz / terrainSize * grassScale), so the grass
//    shells sample the very same texels the ground below them shows -> the
//    blade palette matches the landscape palette exactly.
//  - ANJRpg Phase 4 (field shape): the blades SINK INTO THE GROUND towards
//    the edge of the grass circle (from EdgeHeightStart out to
//    GrassDistance) so the field melts into the terrain texture instead of
//    ending at a visible wall.

#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_WorldMatrix;
//CPU-computed inverse-transpose of the world matrix upper-left 3x3
//(engine UniformBinding: worldMatrix.toRotationMatrix().invert().transpose()).
//Mathematically identical to the transpose(inverse(mat3(g_WorldMatrix))) the
//vertex shader used to build per vertex - but computed once per frame on the
//CPU instead of ~100 ALU ops per vertex (x layers x passes).
uniform mat3 g_WorldNormalMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

uniform float m_Length;
uniform float m_Time;
uniform float m_WaveSpeed;
uniform float m_WaveSize;
uniform float m_Layer;
uniform float m_GrassDistance;
uniform float m_FadeStart;
uniform vec3 m_PlayerPosition;
//field shape: edge sink
uniform float m_EdgeHeightStart;

//HeightBasedTerrain synchronization (values copied from the base material)
uniform float m_TerrainSize;
uniform float m_GrassUVScale;
uniform float m_GrassRegionMin;
uniform float m_GrassRegionMax;
uniform float m_DirtRegionMin;
uniform float m_DirtRegionMax;
uniform float m_RockRegionMin;
uniform float m_RockRegionMax;

varying vec2 texCoord;
varying vec2 groundUV;
varying float fadeFactor;
varying float worldHeight;
varying float groundUp;
varying float grassTint;
//x = grass region weight, y = dirt region weight, z = 2x rock region weight
//(region3 + region4 are both the rock texture), w = slope-rock share of the
//terrain color (computed from the MODEL-space normal like the stock shader)
varying vec4 groundMix;

// Integer-free hash: returns a pseudo random value in the 0..1 range.
// Replacement for the original LFSR_Rand_Gen which required integer bit
// operations from the GL_EXT_gpu_shader4 extension.
float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

// Smooth value noise in the 0..1 range, used to group grass blades into
// patches that sway together.
float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    return mix(mix(mix(hash13(i + vec3(0.0, 0.0, 0.0)),
                       hash13(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash13(i + vec3(0.0, 1.0, 0.0)),
                       hash13(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash13(i + vec3(0.0, 0.0, 1.0)),
                       hash13(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash13(i + vec3(0.0, 1.0, 1.0)),
                       hash13(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

void main() {
    vec4 worldPos4 = g_WorldMatrix * vec4(inPosition, 1.0);
    vec3 worldPos = worldPos4.xyz;

    texCoord = inTexCoord;

    // The same texture UV mapping the stock HeightBasedTerrain.frag uses for
    // its regions: p = position / terrainSize, uv = p.xz * regionScale.
    // Sampling the grass region texture with these UVs makes every blade
    // carry exactly the ground color it stands on.
    groundUV = worldPos.xz / max(m_TerrainSize, 1.0) * m_GrassUVScale;

    // ------------------------------------------------------------------
    // Replicate the HeightBasedTerrain texture region weights 1:1 so the
    // grass placement follows the texture the terrain paints.
    //
    // The terrain shader does:
    //   height   = (g_WorldMatrix * inPosition).y      (WORLD space)
    //   blend    = normalize((abs(normal) - 0.2) * 0.7) with the MODEL-space
    //              vertex normal (HeightBasedTerrain.vert passes inNormal
    //              through untransformed!)
    //   weight_r = max(0, (range - |height - regionMax|) / range)
    //              with range = regionMax - regionMin (so the weight is 1 at
    //              regionMax height and falls off to 0 at regionMin)
    //   color    = blend.y * sum(weight_r * texture_r)
    //            + blend.x * slopeTex + blend.z * slopeTex
    //
    // With THIS game's regions (grass 15..200, dirt 0..20, rock 198..260)
    // the weights are: grass = 0 below height 15 (rising towards 200),
    // dirt = only the 0..40 band, rock = unreachable. The shoreline below
    // height 0 gets NO region texture at all. The fragment shader therefore
    // lets grass grow where no texture outweighs the grass texture, instead
    // of demanding a normalized grass share (which wiped out every blade in
    // the lowlands - the "no grass at all" regression).
    // ------------------------------------------------------------------
    float height = worldPos.y;

    vec3 tblend = abs(normalize(inNormal));
    tblend = (tblend - 0.2) * 0.7;
    tblend = normalize(max(tblend, 0.00001));
    float b = tblend.x + tblend.y + tblend.z;
    tblend /= vec3(b, b, b);

    float grassRange = max(m_GrassRegionMax - m_GrassRegionMin, 0.0001);
    float wGrass = max(0.0, (grassRange - abs(height - m_GrassRegionMax)) / grassRange);

    float dirtRange = max(m_DirtRegionMax - m_DirtRegionMin, 0.0001);
    float wDirt = max(0.0, (dirtRange - abs(height - m_DirtRegionMax)) / dirtRange);

    float rockRange = max(m_RockRegionMax - m_RockRegionMin, 0.0001);
    float wRock = max(0.0, (rockRange - abs(height - m_RockRegionMax)) / rockRange);

    // Hand the raw blending weights to the fragment shader. Region4 uses the
    // same rock texture/heights as region3, so rock is counted twice -
    // exactly like the terrain shader sums both into the same texel color.
    groundMix = vec4(wGrass, wDirt, 2.0 * wRock, tblend.x + tblend.z);

    // Grass circle is centered on the player, not on the camera:
    // blades smoothly thin out between FadeStart and GrassDistance.
    float distToPlayer = distance(m_PlayerPosition, worldPos);
    fadeFactor = 1.0 - smoothstep(m_FadeStart, m_GrassDistance, distToPlayer);

    // Blades also SINK into the ground towards the edge of the circle:
    // full height inside EdgeHeightStart, smoothly shrinking to zero height
    // at GrassDistance. Collapsed shells render the ground texel color at
    // ground level - the grass visually melts into the terrain texture.
    float edgeStart = min(m_EdgeHeightStart, m_GrassDistance * 0.99);
    float edgeShrink = 1.0 - smoothstep(edgeStart, m_GrassDistance, distToPlayer);

    // World-space ground normal, correct under the non-uniform terrain
    // scale (TERRAIN_SCALE_X/Y/Z). g_WorldNormalMatrix is the engine's
    // CPU-computed transpose(inverse(mat3(g_WorldMatrix))) of the upper-left
    // 3x3 - the exact matrix the per-vertex expression below used to build
    // (inverse() is a ~100 ALU cofactor expansion, per vertex, per layer,
    // per pass), now computed once per frame on the CPU and applied with a
    // single mat3 multiply.
    vec3 wNormal = normalize(g_WorldNormalMatrix * inNormal);
    groundUp = wNormal.y;

    worldHeight = worldPos.y;

    // Stable per-cell tint so blade clusters differ slightly in brightness.
    grassTint = hash13(floor(worldPos.xzy * 0.5));

    // Blade height scale: only the edge sink. Blade lengths stay uniform (a
    // noise-patch height variation was tried and reverted: rugged carpet).
    float bladeScale = edgeShrink;

    // Vertical shell displacement: every layer is lifted towards the grass tip.
    // (m_Length * m_Layer) is uniform-only: hoisted so the per-vertex work is
    // a single multiply. Association kept: (m_Length * m_Layer) * bladeScale.
    float layerLift = m_Length * m_Layer;
    vec3 P = inPosition + vec3(0.0, 1.0, 0.0) * (layerLift * bladeScale);

#ifdef WAVE
    // Use a noise function to choose a movement group.
    float gravGroup = noise(worldPos.xzy * 0.35);

    // Cycle the bend factor based on the total time.
    // THIS LINE CAUSES THE WAVING EFFECT! Tweak this depending on your application.
    float bendAmt = sin((m_Time + gravGroup * 6.28318) * m_WaveSpeed);

    vec3 swayDir;
    if (gravGroup < 0.33333) {
        swayDir = vec3(-1.0, 0.0, 0.0);
    } else if (gravGroup < 0.66666) {
        swayDir = vec3(1.0, 0.0, 0.0);
    } else {
        swayDir = vec3(0.0, 0.0, 1.0);
    }

    // Bend in world space (X or Z), converted to view space.
    vec3 vGravity = (vec4(swayDir * (bendAmt * 0.1 * m_WaveSize), 0.0) * g_WorldViewMatrix).xyz;
#else
    vec3 vGravity = (vec4(-0.1, 0.0, 0.0, 0.0) * g_WorldViewMatrix).xyz;
#endif

    // The higher the exponent is, the closer to the tip the grass will start curving.
    // pow(x, 4.0) rewritten as two squarings: identical value up to 1 ulp,
    // much cheaper than the exp2/log2 pair pow() lowers to.
    float k = m_Layer * m_Layer;
    k *= k;
    //Sway scales with the same blade height (collapsed blades do not sway).
    P = P + vGravity * k * bladeScale;

    gl_Position = g_WorldViewProjectionMatrix * vec4(P, 1.0);
}
