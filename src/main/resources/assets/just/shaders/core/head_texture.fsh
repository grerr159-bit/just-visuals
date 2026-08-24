#version 150

#moj_import <just:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;

out vec4 OutColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 radius) {
    radius.xy = (p.x > 0.0) ? radius.xy : radius.wz;
    radius.x = (p.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(p) - b + radius.x;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius.x;
}

void main() {
    vec2 pixel = FragCoord * Size;
    vec2 halfSize = Size * 0.5;
    vec2 local = pixel - halfSize;
    vec2 bounds = max(halfSize - vec2(1.0), vec2(0.0));

    float dist = sdRoundedBox(local, bounds, Radius);
    float aa = max(fwidth(dist) * Smoothness, 1e-4);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    vec4 texColor = texture(Sampler0, TexCoord) * FragColor;
    texColor.a *= alpha;

    if (texColor.a <= 0.0) {
        discard;
    }

    OutColor = texColor;
}
