#version 150

uniform sampler2D Sampler0;
uniform float uHalfTexelX;
uniform float uHalfTexelY;
uniform float uOffset;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec2 h = vec2(uHalfTexelX, uHalfTexelY) * uOffset;
    vec4 color = vec4(0.0);
    color += texture(Sampler0, texCoord0 + vec2(-h.x * 2.0, 0.0)) * 1.0;
    color += texture(Sampler0, texCoord0 + vec2( h.x * 2.0, 0.0)) * 1.0;
    color += texture(Sampler0, texCoord0 + vec2(0.0, -h.y * 2.0)) * 1.0;
    color += texture(Sampler0, texCoord0 + vec2(0.0,  h.y * 2.0)) * 1.0;
    color += texture(Sampler0, texCoord0 + vec2(-h.x, -h.y)) * 2.0;
    color += texture(Sampler0, texCoord0 + vec2( h.x, -h.y)) * 2.0;
    color += texture(Sampler0, texCoord0 + vec2( h.x,  h.y)) * 2.0;
    color += texture(Sampler0, texCoord0 + vec2(-h.x,  h.y)) * 2.0;
    fragColor = vec4((color / 12.0).rgb, 1.0);
}
