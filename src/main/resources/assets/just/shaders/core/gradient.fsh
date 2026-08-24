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
uniform vec2 Smoothness;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);

    float alpha = 1.0 - smoothstep(1.0 - Smoothness.x, 1.0, dist);

    vec4 color = gradient(FragCoord, color1, color2, color3, color4);
    color *= alpha;

    if (color.a <= 0.0) {
        discard;
    }

    OutColor = color;
}
