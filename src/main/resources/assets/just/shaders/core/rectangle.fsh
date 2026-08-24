#version 150

#moj_import <just:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;


uniform vec2 Size;
uniform vec4 Radius;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

uniform float Smoothness;

out vec4 OutColor;

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = gradient(FragCoord, color1, color2, color3, color4);
    color.a *= alpha;

    if (color.a == 0.0) {
        discard;
    }

    OutColor = color;
}