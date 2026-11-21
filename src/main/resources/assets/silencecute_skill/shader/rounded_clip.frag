#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uSize;
uniform vec4 uRect;
uniform float uRadius;
uniform vec4 uTint;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 b = halfSize - vec2(r);
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    vec4 tex = texture(uTexture, uv);

    vec2 p = uv * uSize;
    vec2 center = uRect.xy + uRect.zw * 0.5;
    vec2 halfSize = uRect.zw * 0.5;

    float r = clamp(uRadius, 0.0, min(halfSize.x, halfSize.y));
    float dist = sdRoundedRect(p - center, halfSize, r);
    float aa = max(fwidth(dist), 0.75);
    float mask = 1.0 - smoothstep(0.0, aa, dist);

    color = vec4(tex.rgb * uTint.rgb, tex.a * uTint.a * mask);
}
