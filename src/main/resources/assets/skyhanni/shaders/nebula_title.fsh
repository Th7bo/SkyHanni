#version 150

// Red nebula backdrop with white-hot lightning sparks darting around.
// Each spark has its own randomized direction, speed, lifetime, phase, and a
// noise-driven wobble on its path, so the motion never feels gridded.
// Built on a per-cell hash for stable frame-to-frame results.

layout(std140) uniform SkyHanniNebulaTitleUniforms {
    vec4 u_params;   // x = framebuffer width, y = framebuffer height, z = time seconds, w = unused
    vec4 u_shadow;   // darkest base
    vec4 u_deep;     // dark fill
    vec4 u_mid;      // mid plasma
    vec4 u_bright;   // bright wisps / spark trail
    vec4 u_hot;      // hot ember halo / spark core highlight
};

in vec4 vertexColor;
out vec4 fragColor;

// ---------- Hash helpers ----------

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec2 hash22(vec2 p) {
    return fract(sin(vec2(
        dot(p, vec2(127.1, 311.7)),
        dot(p, vec2(269.5, 183.3))
    )) * 43758.5453);
}

vec4 hash42(vec2 p) {
    return fract(sin(vec4(
        dot(p, vec2(127.1, 311.7)),
        dot(p, vec2(269.5, 183.3)),
        dot(p, vec2(113.5, 271.9)),
        dot(p, vec2(246.1, 437.7))
    )) * 43758.5453);
}

// ---------- Background nebula ----------

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    mat2 rot = mat2(0.866, -0.5, 0.5, 0.866);
    for (int i = 0; i < 5; i++) {
        v += a * vnoise(p);
        p = rot * p * 2.05 + 11.3;
        a *= 0.5;
    }
    return v;
}

// ---------- Lightning sparks ----------

// One spark per cell. Returns vec3(coreBrightness, trailBrightness, haloGlow).
//   - core    : tiny bright dot at the spark's current position (white-hot)
//   - trail   : motion-blur stretch behind the spark, oriented along its instantaneous velocity
//   - halo    : soft glow that illuminates the surrounding nebula
//
// `gridUv` is the pixel position in cell coordinates (so distances are measured
// in cells), `cellId` is the integer cell we're sampling.
vec3 sparkAt(vec2 gridUv, vec2 cellId, float t) {
    vec4 r1 = hash42(cellId);
    vec4 r2 = hash42(cellId + 7.31);

    // Density: ~25% of cells ever spawn a spark. Sparser than before.
    if (r2.z < 0.75) return vec3(0.0);

    // Random base direction.
    float angle = r1.x * 6.2831853;

    // Random speed in cells/sec. Bounded so max travel stays within the 3x3 neighbourhood.
    float speed = 0.16 + r1.y * 0.30;             // 0.16 .. 0.46

    // Random origin offset inside the cell so sparks aren't on a grid.
    vec2 origin = cellId + r1.zw * 0.5 + 0.25;

    // Random lifetime + phase so they don't all flash at the same instant.
    float lifetime = 1.4 + r2.y * 2.4;            // 1.4 .. 3.8 sec
    float phase    = r2.x * lifetime;
    float life     = mod(t + phase, lifetime);

    // ----- Random path -----
    // Each spark gets two independent low-frequency sine wobbles offset in time.
    // The wobble is added on top of the linear drift so the path curves and darts
    // unpredictably instead of running on a perfect line.
    float wobbleFreqA = 1.6 + r1.z * 2.4;
    float wobbleFreqB = 1.1 + r1.w * 2.0;
    float wobblePhaseA = r2.w * 6.2831853;
    float wobblePhaseB = r2.x * 6.2831853;
    float wobbleAmp    = 0.22 + r2.y * 0.28;       // 0.22 .. 0.50 cells

    vec2 baseDir = vec2(cos(angle), sin(angle));
    vec2 perpDir = vec2(-baseDir.y, baseDir.x);

    vec2 pos = origin
             + baseDir * speed * life
             + perpDir * sin(life * wobbleFreqA + wobblePhaseA) * wobbleAmp
             + baseDir * sin(life * wobbleFreqB + wobblePhaseB) * wobbleAmp * 0.45;

    // Instantaneous velocity = derivative of pos w.r.t. life. We need this so the trail
    // points along the *current* direction of motion rather than the spark's base heading.
    vec2 vel = baseDir * speed
             + perpDir * cos(life * wobbleFreqA + wobblePhaseA) * wobbleAmp * wobbleFreqA
             + baseDir * cos(life * wobbleFreqB + wobblePhaseB) * wobbleAmp * wobbleFreqB * 0.45;
    float velLen = max(length(vel), 1e-4);
    vec2  dir    = vel / velLen;

    // Decompose pixel offset into along-direction and perpendicular components.
    vec2 toSpark = gridUv - pos;
    float along  = dot(toSpark, dir);
    vec2  perpV  = toSpark - along * dir;
    float perp2  = dot(perpV, perpV);

    // Core: tight bright dot at the spark's current position.
    float core = exp(-(along * along + perp2) * 520.0);

    // Trail: asymmetric Gaussian along the motion line. Gentle decay BEHIND the spark
    // (`along < 0`) so it streaks back, sharp decay AHEAD so the leading edge stays clean
    // — no `step()` cutoff, so there's no flat top edge artifact.
    float trailLen     = max(velLen * 0.55, 0.05);
    float backNorm     = max(-along, 0.0) / trailLen;
    float frontNorm    = max( along, 0.0) / trailLen;
    float trailAlongDecay = exp(-(backNorm * backNorm * 2.5
                               +  frontNorm * frontNorm * 60.0));
    float trail = trailAlongDecay * exp(-perp2 * 720.0);

    // Halo: wide soft glow around the spark — lights up nearby nebula.
    float halo = exp(-(along * along + perp2) * 28.0);

    // Lifecycle: rapid ignition, longer cool-down so trails persist briefly.
    float fadeIn  = smoothstep(0.0, 0.06 * lifetime, life);
    float fadeOut = smoothstep(lifetime, 0.45 * lifetime, life);
    float fade    = fadeIn * fadeOut;

    return vec3(core, trail, halo) * fade;
}

// Sum spark contributions from the surrounding 5x5 cells. We need that wider radius
// because the per-spark wobble can push a spark up to ~2 cells from its origin.
// Cells without an active spark short-circuit at the top of `sparkAt`, so the cost
// in practice is only ~25% of the iterations.
vec3 sparksLayer(vec2 uv, float t, float cellScale) {
    vec2 grid = uv * cellScale;
    vec2 cellId = floor(grid);

    vec3 total = vec3(0.0);
    for (int j = -2; j <= 2; j++) {
        for (int i = -2; i <= 2; i++) {
            total += sparkAt(grid, cellId + vec2(float(i), float(j)), t);
        }
    }
    return total;
}

// ---------- Main ----------

void main() {
    float t   = u_params.z;
    vec2  fc  = gl_FragCoord.xy;
    vec2  res = u_params.xy;
    vec2  uv  = (fc - 0.5 * res) / max(res.y, 1.0);

    // ----- Red nebula background -----
    vec2 nebDrift = vec2(t * 0.022, t * 0.013);
    vec2 p = uv * 1.45 + nebDrift;

    // Domain warp gives the nebula curving "smoke" forms instead of plain noise blobs.
    vec2 warp = vec2(fbm(p * 1.3 + vec2(0.0,  t * 0.05)),
                     fbm(p * 1.3 + vec2(5.2, -t * 0.04)));
    float base = fbm(p + warp * 0.65);

    // A second higher-frequency layer adds wispy filaments inside the cloud.
    float wisp = pow(fbm(p * 2.6 + warp * 0.4), 1.6);

    float nebula = base * 0.62 + wisp * 0.45;
    nebula = pow(clamp(nebula, 0.0, 1.0), 1.05);

    // Color the nebula along the configured palette. Steeper-than-linear midtones
    // so the cloud reads as predominantly dark with bright highlights.
    vec3 col = mix(u_shadow.rgb, u_deep.rgb,   smoothstep(0.00, 0.30, nebula));
    col      = mix(col,           u_mid.rgb,    smoothstep(0.25, 0.60, nebula));
    col      = mix(col,           u_bright.rgb, smoothstep(0.65, 0.95, nebula) * 0.55);

    // ----- Lightning sparks -----
    // Single layer — large coarse cells so only a handful of sparks dance across the screen.
    vec3 s1 = sparksLayer(uv, t, 4.0);

    float coreSum  = s1.x;
    float trailSum = s1.y;
    float haloSum  = s1.z * 0.55;

    // Spark colors: cores are near-white with a hot tint, trails ride the bright
    // palette color, halos use the bright color softer to bleed into the nebula.
    vec3 coreCol  = mix(u_hot.rgb, vec3(1.0), 0.65);
    vec3 trailCol = mix(u_bright.rgb, vec3(1.0), 0.25);
    vec3 haloCol  = u_bright.rgb;

    col += coreCol  * coreSum  * 1.7;
    col += trailCol * trailSum * 0.85;
    col += haloCol  * haloSum  * 0.18;

    // Subtle vignette so the corners feel deeper.
    float vig = 1.0 - dot(uv * 0.85, uv * 0.85) * 0.30;
    col *= vig;

    fragColor = vec4(col, 1.0) * vertexColor;
}
