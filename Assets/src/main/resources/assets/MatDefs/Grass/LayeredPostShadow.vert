// Layered shell grass - POST SHADOW (shadow RECEIVING) vertex shader.
// Used by the 'PostShadow' technique of Layered.j3md: the PssmShadowRenderer
// re-renders every shadow-receiving geometry once per frame with this
// technique, modulate-blending the shadow factor onto the already rendered
// scene.
//
// The vertex displacement below is a BIT-EXACT copy of Layered.vert (shell
// lifting, edge sink, wind sway). This is what makes the shadow pass land
// exactly on the pixels the main pass drew: the depth test (LEQUAL + the
// small PolyOffset from the technique render state) then lets exactly ONE
// shell per pixel through - the one that actually owns the frame buffer
// pixel - so a blade is darkened once, never twice, and the ground between
// the blades is darkened by the m_Layer == 0 (base) pass instead.
//
// Shadow coordinate math (biasMat, projCoord0..3, shadowPosition) is taken
// verbatim from the engine's Common/MatDefs/Shadow/PostShadow.vert.

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

//HeightBasedTerrain synchronization (values copied from the base material;
//needed to replicate the texture-dominance gates of the main pass)
uniform float m_TerrainSize;
uniform float m_GrassUVScale;
uniform float m_GrassRegionMin;
uniform float m_GrassRegionMax;
uniform float m_DirtRegionMin;
uniform float m_DirtRegionMax;
uniform float m_RockRegionMin;
uniform float m_RockRegionMax;

//PSSM shadow mapping (values pushed onto this material by PssmShadowRenderer)
uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

varying vec2 texCoord;
varying float fadeFactor;
varying float worldHeight;
varying float groundUp;
//x = grass region weight, y = dirt region weight, z = 2x rock region weight,
//w = slope-rock share of the terrain color (see Layered.vert)
varying vec4 groundMix;

//shadow varyings (see engine PostShadow.vert)
varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;
#if defined(PSSM) || defined(FADE)
varying float shadowPosition;
#endif

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);

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

    // ------------------------------------------------------------------
    // Region weights + normals + fades: EXACT copy of Layered.vert so the
    // fragment shader can apply the very same discard decisions as the
    // main pass (only pixels that actually own a blade darken).
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

    groundMix = vec4(wGrass, wDirt, 2.0 * wRock, tblend.x + tblend.z);

    // Grass circle is centered on the player, not on the camera.
    float distToPlayer = distance(m_PlayerPosition, worldPos);
    fadeFactor = 1.0 - smoothstep(m_FadeStart, m_GrassDistance, distToPlayer);

    // Blades also SINK into the ground towards the edge of the circle.
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

    // Blade height scale: only the edge sink (must match Layered.vert).
    float bladeScale = edgeShrink;

    // Vertical shell displacement: every layer is lifted towards the grass tip.
    // m_Layer == 0 is the BASE (ground) pass: undisplaced, unmasked.
    // (m_Length * m_Layer) is uniform-only: hoisted once, reused for the
    // shadow projection below (identical association, bit-for-bit).
    float layerLift = m_Length * m_Layer;
    vec3 liftVec = vec3(0.0, 1.0, 0.0) * (layerLift * bladeScale);
    vec3 P = inPosition + liftVec;

#ifdef WAVE
    // Use a noise function to choose a movement group.
    float gravGroup = noise(worldPos.xzy * 0.35);

    // Cycle the bend factor based on the total time.
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

    // ------------------------------------------------------------------
    // Shadow coordinates (engine PostShadow.vert math). Projected from the
    // world-space DISPLACED position (wind sway omitted - its offset is a
    // few centimeters, far below the shadow map resolution).
    // ------------------------------------------------------------------
#if defined(PSSM) || defined(FADE)
    shadowPosition = gl_Position.z;
#endif
    vec3 shadowWorldPos = worldPos + liftVec;
    projCoord0 = biasMat * m_LightViewProjectionMatrix0 * vec4(shadowWorldPos, 1.0);
    projCoord1 = biasMat * m_LightViewProjectionMatrix1 * vec4(shadowWorldPos, 1.0);
    projCoord2 = biasMat * m_LightViewProjectionMatrix2 * vec4(shadowWorldPos, 1.0);
    projCoord3 = biasMat * m_LightViewProjectionMatrix3 * vec4(shadowWorldPos, 1.0);
}
