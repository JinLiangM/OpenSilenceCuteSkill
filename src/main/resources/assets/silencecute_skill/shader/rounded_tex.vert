#version 330 core

precision lowp float;

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aUv;
layout (location = 2) in vec2 aLocal;

out vec2 vUv;
out vec2 vLocal;

void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
    vUv = aUv;
    vLocal = aLocal;
}

