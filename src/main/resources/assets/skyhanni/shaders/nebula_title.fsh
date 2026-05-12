#version 150

layout(std140) uniform SkyHanniNebulaTitleUniforms {
    vec4 u_params;
    vec4 u_shadow;
    vec4 u_deep;
    vec4 u_mid;
    vec4 u_bright;
    vec4 u_hot;
};

in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.52;
    mat2 rot = mat2(0.86, -0.5, 0.5, 0.86);
    for (int i = 0; i < 6; i++) {
        v += a * noise(p);
        p = rot * p * 2.02 + 17.0;
        a *= 0.5;
    }
    return v;
}

float ridge(vec2 p) {
    float s = 0.0;
    float a = 0.48;
    for (int i = 0; i < 5; i++) {
        float n = noise(p);
        n = 1.0 - abs(n * 2.0 - 1.0);
        s += n * a;
        p *= 2.08;
        a *= 0.48;
    }
    return s;
}

float voronoiEdge(vec2 x) {
    vec2 n = floor(x);
    vec2 f = fract(x);
    float md = 8.0;
    float md2 = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = vec2(hash(n + g), hash(n + g + vec2(31.7, 19.3)));
            vec2 r = g + o - f;
            float d = dot(r, r);
            if (d < md) {
                md2 = md;
                md = d;
            } else if (d < md2) {
                md2 = d;
            }
        }
    }
    return sqrt(md2) - sqrt(md);
}

void main() {
    float t = u_params.z * 0.09;
    vec2 fc = gl_FragCoord.xy;
    vec2 u_resolution = u_params.xy;
    vec2 uv = (fc - 0.5 * u_resolution) / max(u_resolution.y, 1.0);

    vec2 drift = vec2(sin(t * 0.73) * 0.07, cos(t * 0.51) * 0.055);
    vec2 p = uv * 1.55 + drift;

    vec2 p1 = p * 1.1 + vec2(t * 0.11, t * 0.07);
    vec2 p2 = p * 2.35 - vec2(t * 0.16, -t * 0.12) + vec2(8.4, 2.1);

    float base = fbm(p1 + fbm(p1 * 1.7 + t * 0.05));
    float wisps = ridge(p2);
    float cells = voronoiEdge(p * 3.4 + vec2(t * 0.25, sin(t * 0.4) * 0.2));
    float filaments = smoothstep(0.02, 0.12, cells) * (0.55 + wisps);

    float n = base * 0.58 + wisps * 0.42 + filaments * 0.38;
    n = pow(clamp(n, 0.0, 1.0), 0.92);

    vec2 glowPos = uv - vec2(0.42, 0.28);
    float cornerGlow = 0.22 / (dot(glowPos, glowPos) * 5.5 + 0.08);
    n += cornerGlow * 0.16;

    float grain = hash(fc + u_params.z * 60.0);
    n += (grain - 0.5) * 0.035;

    vec3 cShadow = u_shadow.rgb;
    vec3 cDeep = u_deep.rgb;
    vec3 cMid = u_mid.rgb;
    vec3 cBright = u_bright.rgb;
    vec3 cHot = u_hot.rgb;

    vec3 col = mix(cShadow, cDeep, smoothstep(0.0, 0.28, n));
    col = mix(col, cMid, smoothstep(0.22, 0.55, n));
    col = mix(col, cBright, smoothstep(0.48, 0.82, n));
    col = mix(col, cHot, smoothstep(0.75, 1.0, n) * 0.55);

    float vig = 1.0 - dot(uv * 0.88, uv * 0.88) * 0.38;
    col *= vig;

    fragColor = vec4(col, 1.0) * vertexColor;
}
