#version 150

#moj_import <just:common.glsl>

uniform vec2 Size;
uniform vec4 Radius;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

uniform float Softness;
uniform float ShadowRadius;

in vec4 FragColor;
in vec2 FragCoord;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord.xy * Size), center - 1.0 - ShadowRadius, Radius);

    if (dist <= .0) discard;

    float alpha = 1.0 - smoothstep(-Softness, ShadowRadius, dist);
    vec4 color = gradient(FragCoord, color1, color2, color3, color4);
    color.a *= alpha;

    OutColor = color;
}