package dev.client.api.nullcry.uiClient.draggables;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import org.joml.Matrix4f;

public class GlassShadow {

    public static void render(Matrix4f matrix, float x, float y, float width, float height, float radius, float show) {
        if (show <= 0.01f) return;

        int shadowAlpha = (int)(40 * show);
        if (shadowAlpha < 1) return;

        float spread = 6f;
        int passes = 4;

        for (int i = passes; i >= 1; i--) {
            float expand = spread * i / passes;
            int a = (int)(shadowAlpha * (1f - (float) i / (passes + 1)));
            if (a < 1) continue;
            int color = (a << 24);

            ClientApi.rectangle()
                    .size(new SizeState(width + expand * 2, height + expand * 2))
                    .radius(new QuadRadiusState(radius + expand))
                    .color(new QuadColorState(color))
                    .build()
                    .render(matrix, x - expand, y - expand);
        }
    }
}
