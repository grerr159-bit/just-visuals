#version 150

#moj_import <just:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float BlurRadius;

out vec4 OutColor;

const int DIRECTION_COUNT = 8;
const int MAX_STEPS = 12;
const vec2 SAMPLE_DIRECTIONS[DIRECTION_COUNT] = vec2[](
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(0.70710678, 0.70710678),
    vec2(-0.70710678, 0.70710678),
    vec2(-1.0, 0.0),
    vec2(0.0, -1.0),
    vec2(-0.70710678, -0.70710678),
    vec2(0.70710678, -0.70710678)
);

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    float radius = max(BlurRadius, 0.0);

    vec3 accumulated = texture(Sampler0, TexCoord).rgb;
    float weightSum = 1.0;

    if (radius > 0.0) {
        float steps = clamp(radius, 1.0, float(MAX_STEPS));
        float sigma = max(radius * 0.5, 0.75);

        for (int dir = 0; dir < DIRECTION_COUNT; dir++) {
            vec2 direction = SAMPLE_DIRECTIONS[dir] * texelSize;

            for (int step = 1; step <= MAX_STEPS; step++) {
                if (float(step) > steps) {
                    break;
                }

                float offsetScale = float(step);
                float weight = gaussian(offsetScale, sigma);
                vec2 offset = direction * offsetScale;

                vec3 positive = texture(Sampler0, TexCoord + offset).rgb;
                vec3 negative = texture(Sampler0, TexCoord - offset).rgb;

                accumulated += (positive + negative) * weight;
                weightSum += weight * 2.0;
            }
        }
    }

    vec3 average = accumulated / max(weightSum, 0.0001);

    vec4 color = vec4(average, 1.0) * FragColor;
    color.a *= ralpha(Size, FragCoord, Radius, Smoothness);

    if (color.a == 0.0) {
        discard;
    }

    OutColor = color;
}
