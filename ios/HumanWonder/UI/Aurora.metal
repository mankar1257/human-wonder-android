// The "aurora valley" behind the chat: a ribbon of violet-blue-teal light, low in the middle and
// climbing both sides, over true black. Ported line for line from the Android app's GLSL shader
// (ui/GalaxyBackground.kt). calm (0 = empty sky, 1 = conversation) lowers and dims it so it never
// sits behind text; energy lifts it while Orbit 412 answers; pulse sends a soft ring up when a
// question is sent. Positions are in points, like the Android shader's dp-sized surface.

#include <metal_stdlib>
#include <SwiftUI/SwiftUI_Metal.h>
using namespace metal;

static float3 mod289(float3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
static float2 mod289(float2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
static float3 permute(float3 x) { return mod289(((x * 34.0) + 1.0) * x); }

static float snoise(float2 v) {
    const float4 C = float4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    float2 i = floor(v + dot(v, C.yy));
    float2 x0 = v - i + dot(i, C.xx);
    float2 i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
    float4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    float3 p = permute(permute(i.y + float3(0.0, i1.y, 1.0)) + i.x + float3(0.0, i1.x, 1.0));
    float3 m = max(0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    float3 x = 2.0 * fract(p * C.www) - 1.0;
    float3 h = abs(x) - 0.5;
    float3 ox = floor(x + 0.5);
    float3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    float3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

static float fbm(float2 p) {
    float f = 0.0;
    float w = 0.5;
    for (int i = 0; i < 4; i++) {
        f += w * snoise(p);
        p *= 2.03;
        w *= 0.5;
    }
    return f;
}

static float hash21(float2 p) {
    p = fract(p * float2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

[[ stitchable ]] half4 aurora(float2 position, half4 color, float2 size, float time, float energy, float calm, float pulse) {
    // GLSL's gl_FragCoord has y up; SwiftUI's position has y down.
    float2 frag = float2(position.x, size.y - position.y);
    float2 uv = frag / size;
    float t = time;
    float e = energy;

    float x = uv.x - 0.5;
    float edge = pow(abs(x) * 2.0, 2.3);
    float base = mix(0.15, 0.07, calm) + 0.015 * e;
    float lift = mix(0.56, 0.15, calm);
    float wobble = fbm(float2(uv.x * 1.8 + t * 0.025, t * 0.04)) * 0.06;
    float ridgeY = base + lift * edge + wobble;
    float d = uv.y - ridgeY;

    float above = exp(-max(d, 0.0) / 0.095);
    float below = mix(1.0, 0.32, smoothstep(0.0, 0.5, -d));
    float glow = d > 0.0 ? above : below;

    float rays = fbm(float2(uv.x * 5.0 - t * 0.05, d * 9.0 + t * 0.02));
    glow *= 0.72 + 0.55 * rays;

    float ridge = exp(-abs(d) / 0.045);

    float3 deep = float3(0.07, 0.08, 0.26);
    float3 violet = float3(0.55, 0.40, 1.00);
    float3 blue = float3(0.28, 0.50, 1.00);
    float3 cyan = float3(0.22, 0.92, 0.82);
    float hx = uv.x + 0.12 * sin(t * 0.06);
    float3 band = mix(violet, blue, smoothstep(0.15, 0.55, hx));
    band = mix(band, cyan, smoothstep(0.62, 1.05, hx) * 0.85);
    float3 aurora = mix(deep, band, clamp(ridge * 1.1 + 0.25, 0.0, 1.0)) * glow;
    aurora += float3(0.85, 0.85, 1.0) * ridge * ridge * 0.25 * glow;
    aurora *= mix(1.0, 0.58, calm) * (1.0 + 0.3 * e);

    float2 starGrid = frag * 0.11;
    float2 starId = floor(starGrid);
    float starRand = hash21(starId);
    float star = 0.0;
    if (starRand > 0.986) {
        float2 sp = fract(starGrid) - 0.5;
        float tw = sin(t * 1.6 + starRand * 40.0) * 0.5 + 0.5;
        star = (1.0 - smoothstep(0.0, 0.22, length(sp))) * (0.35 + 0.35 * tw);
    }
    star *= smoothstep(0.0, 0.12, d) * mix(1.0, 0.45, calm);

    float3 col = aurora + float3(star);

    if (pulse < 2.4) {
        float2 q = float2((uv.x - 0.5) * size.x / size.y, uv.y + 0.05);
        float ring = 1.0 - smoothstep(0.0, 0.05, abs(length(q) - pulse * 0.5));
        float fade = 1.0 - pulse / 2.4;
        col += band * ring * fade * fade * 0.35;
    }

    return half4(half3(col), 1.0h);
}
