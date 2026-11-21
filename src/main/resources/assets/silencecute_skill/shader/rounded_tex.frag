#version 330 core

precision lowp float;

in vec2 vUv;
in vec2 vLocal;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uSize;
uniform float uRadius;
uniform vec3 uTint;
uniform float uAlpha;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 b = halfSize - vec2(r);
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    vec4 tex = texture(uTexture, vUv);

    vec2 sizePx = max(uSize, vec2(1.0));
    vec2 halfSize = sizePx * 0.5;
    float r = clamp(uRadius, 0.0, min(halfSize.x, halfSize.y));

    vec2 p = vLocal * sizePx - halfSize;
    float dist = sdRoundedRect(p, halfSize, r);
    float aa = max(fwidth(dist), 0.75);
    float mask = 1.0 - smoothstep(0.0, aa, dist);

    float outA = tex.a * clamp(uAlpha, 0.0, 1.0) * mask;
    color = vec4(tex.rgb * uTint, outA);
}
