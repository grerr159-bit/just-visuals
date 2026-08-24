package dev.client.api.nullcry.uiClient.draggables;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;

import java.util.Objects;
import java.util.function.IntSupplier;

public final class DraggableHeaderRenderer implements ClientApi {
    private static final float PADDING_X = 6f;
    private static final float SPACING = 5f;
    private static final float SEPARATOR_WIDTH = 1f;
    private static final float SEPARATOR_HEIGHT = 9f;

    private DraggableHeaderRenderer() {
    }

    public static void render(RenderEvent.Draw2D event,
                              float x,
                              float y,
                              float width,
                              float showProgress,
                              float headerHeight,
                              String iconText,
                              float iconSize,
                              String title,
                              float textSize,
                              int baseTextColor,
                              int baseIconColor,
                              IntSupplier separatorColorSupplier) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(separatorColorSupplier, "separatorColorSupplier");

        float clampedShow = clamp01(showProgress);

        // Не рендерим фон здесь - он будет отрендерен вместе с телом

        float iconBaseX = x + PADDING_X;
        float iconBaseY = y + (headerHeight - iconSize) / 2f + 0.5f;
        float iconWidth = 0f;

        if (iconText != null && !iconText.isEmpty()) {
            ClientApi.text()
                    .text(iconText)
                    .font(ClientApi.icons())
                    .size(iconSize)
                    .color(ColorUtils.setAlpha(baseIconColor, (int) (255 * clampedShow)))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), iconBaseX, iconBaseY);
            iconWidth = ClientApi.icons().getWidth(iconText, iconSize);
        }

        float sepX = Math.round(iconBaseX + iconWidth + SPACING);
        float sepY = Math.round(y + (headerHeight - SEPARATOR_HEIGHT) / 2f) + 0.5f;

        int separatorBaseColor = separatorColorSupplier.getAsInt();
        int separatorColor = ColorUtils.setAlpha(separatorBaseColor, (int) (255 * clampedShow));

        ClientApi.rectangle()
                .size(new SizeState(SEPARATOR_WIDTH, SEPARATOR_HEIGHT))
                .color(new QuadColorState(separatorColor))
                .radius(new QuadRadiusState(1f))
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), sepX, sepY);

        float textX = sepX + SEPARATOR_WIDTH + SPACING;
        float textY = y + (headerHeight - textSize) / 2f - 0.5f;

        ClientApi.text()
                .text(title)
                .font(ClientApi.inter())
                .size(textSize)
                .color(ColorUtils.setAlpha(-1, (int) (255 * clampedShow)))
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), textX, textY);
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}
