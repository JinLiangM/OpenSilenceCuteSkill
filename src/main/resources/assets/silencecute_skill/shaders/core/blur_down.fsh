#version 150

uniform sampler2D Sampler0;
uniform float uHalfTexelX;
uniform float uHalfTexelY;
uniform float uOffset;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec2 h = vec2(uHalfTexelX, uHalfTexelY) * uOffset;
    vec4 color = texture(Sampler0, texCoord0) * 4.0;
    color += texture(Sampler0, texCoord0 + vec2( h.x,  h.y));
    color += texture(Sampler0, texCoord0 + vec2( h.x, -h.y));
    color += texture(Sampler0, texCoord0 + vec2(-h.x,  h.y));
    color += texture(Sampler0, texCoord0 + vec2(-h.x, -h.y));
    fragColor = color / 8.0;
}
