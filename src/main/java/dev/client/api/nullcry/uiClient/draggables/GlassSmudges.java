package dev.client.api.nullcry.uiClient.draggables;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlassSmudges {

    private static final List<Smudge> smudges = new ArrayList<>();
    private static final List<LightStreak> streaks = new ArrayList<>();
    private static final Random random = new Random();
    private static float cachedW = 0;
    private static float cachedH = 0;

    public static void render(Matrix4f matrix, float x, float y, float width, float height, float show) {
        if (show <= 0.01f) {
            smudges.clear();
            streaks.clear();
            return;
        }

        if (smudges.isEmpty() || Math.abs(width - cachedW) > 5f || Math.abs(height - cachedH) > 5f) {
            generateSmudges(width, height);
            generateStreaks(width, height);
            cachedW = width;
            cachedH = height;
        }

        for (Smudge s : smudges) {
            int a = (int)(s.alpha * 70 * show);
            if (a < 1) continue;
            int color = (a << 24) | 0xFFFFFF;
            ClientApi.rectangle()
                    .size(new SizeState(s.w, s.h))
                    .radius(new QuadRadiusState(s.h / 2f))
                    .color(new QuadColorState(color))
                    .build()
                    .render(matrix, x + s.x, y + s.y);
        }

        for (LightStreak st : streaks) {
            float cos = (float) Math.cos(st.angle);
            float sin = (float) Math.sin(st.angle);
            float halfLen = st.length / 2f;
            float cx = x + st.cx;
            float cy = y + st.cy;

            for (int i = 0; i < st.rays; i++) {
                float offset = (i - (st.rays - 1) / 2f) * st.spacing;
                float rayX = cx + sin * offset;
                float rayY = cy - cos * offset;

                int segCount = 12;
                for (int j = 0; j < segCount; j++) {
                    float t = (float) j / (segCount - 1);
                    float fade = 1f - Math.abs(t - 0.5f) * 2f;
                    fade = fade * fade;
                    int a = (int)(st.alpha * 80 * show * fade);
                    if (a < 1) continue;

                    float segLen = st.length / segCount;
                    float segX = rayX + cos * (t * st.length - halfLen);
                    float segY = rayY + sin * (t * st.length - halfLen);

                    int color = (a << 24) | 0xFFFFFF;
                    ClientApi.rectangle()
                            .size(new SizeState(segLen + 1f, st.thickness))
                            .radius(new QuadRadiusState(st.thickness / 2f))
                            .color(new QuadColorState(color))
                            .build()
                            .render(matrix, segX, segY - st.thickness / 2f);
                }
            }
        }
    }

    private static void generateSmudges(float w, float h) {
        smudges.clear();
        int count = 4 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            Smudge s = new Smudge();
            s.w = 18f + random.nextFloat() * 35f;
            s.h = 1.5f + random.nextFloat() * 2.5f;
            s.x = random.nextFloat() * (w - s.w);
            s.y = random.nextFloat() * (h - s.h);
            s.alpha = 0.3f + random.nextFloat() * 0.7f;
            smudges.add(s);
        }
    }

    private static void generateStreaks(float w, float h) {
        streaks.clear();
        int count = 3 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            LightStreak st = new LightStreak();
            st.angle = (float)(Math.PI / 5 + (random.nextFloat() - 0.5f) * 0.5f);
            st.length = 25f + random.nextFloat() * 45f;
            st.thickness = 0.8f + random.nextFloat() * 1.2f;
            st.rays = 2 + random.nextInt(3);
            st.spacing = 2f + random.nextFloat() * 3f;
            st.cx = random.nextFloat() * w;
            st.cy = random.nextFloat() * h;
            st.alpha = 0.4f + random.nextFloat() * 0.6f;
            streaks.add(st);
        }
    }

    private static class Smudge {
        float x, y, w, h;
        float alpha;
    }

    private static class LightStreak {
        float cx, cy, angle, length, thickness;
        int rays;
        float spacing;
        float alpha;
    }
}
