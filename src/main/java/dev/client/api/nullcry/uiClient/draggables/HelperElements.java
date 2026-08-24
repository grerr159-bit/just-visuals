package dev.client.api.nullcry.uiClient.draggables;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.modules.core.render.Interface;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

@UtilityClass
public class HelperElements implements ClientApi {
    private int[] cachedGradient = null;
    private int cachedGradientBaseColor = 0;

    public void rectElements(DrawContext context, float x, float y, float width, float height) {
        rectElements(context, x, y, width, height, 1.0f);
    }

    public void rect(Matrix4f matrix4f, float x, float y, float width, float height) {
        rectElements(matrix4f, x, y, width, height, 1.0f);
    }

    public void rectElements(Matrix4f matrix4f, float x, float y, float width, float height) {
        rectElements(matrix4f, x, y, width, height, 1.0f);
    }

    public void rect(DrawContext context, float x, float y, float width, float height) {
        rectElements(context, x, y, width, height, 1.0f);
    }

    public void rectGui(DrawContext context, float x, float y, float width, float height) {
        rectGui(context, x, y, width, height, 1.0f);
    }

    public void rect(DrawContext context, float x, float y, float width, float height, float show) {
        rectElements(context, x, y, width, height, show);
    }

    public void rect(Matrix4f matrix4f, float x, float y, float width, float height, float show) {
        rectElements(matrix4f, x, y, width, height, show);
    }

    public void rectElements(DrawContext context, float x, float y, float width, float height, float show) {
        float s = clamp01(show);

        if (isBlurEnabled(s)) {
            ClientApi.blur()
                    .blurRadius(getBlurRadius())
                    .radius(new QuadRadiusState(getRadius()))
                    .size(new SizeState(width, height))
                    .alpha(getBlurAlpha(s))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x, y);
        }

        ClientApi.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), getBackgroundAlpha(s))
                ))
                .radius(new QuadRadiusState(getRadius()))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);

    }

    public void rectElements(Matrix4f context, float x, float y, float width, float height, float show) {
        float s = clamp01(show);

        if (isBlurEnabled(s)) {
            ClientApi.blur()
                    .blurRadius(getBlurRadius())
                    .radius(new QuadRadiusState(getRadius()))
                    .size(new SizeState(width, height))
                    .alpha(getBlurAlpha(s))
                    .build()
                    .render(context, x, y);
        }

        ClientApi.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), getBackgroundAlpha(s))
                ))
                .radius(new QuadRadiusState(getRadius()))
                .build()
                .render(context, x, y);
    }

    public void rectGui(DrawContext context, float x, float y, float width, float height, float show) {
        float s = clamp01(show);

        if (isBlurEnabled(s)) {
            ClientApi.blur()
                    .blurRadius(getBlurRadius())
                    .radius(new QuadRadiusState(10 / 2f))
                    .size(new SizeState(width, height))
                    .alpha(getBlurAlpha(s))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x, y);
        }

        ClientApi.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), getBackgroundAlpha(s)),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), getBackgroundAlpha(s))
                ))
                .radius(new QuadRadiusState(10 / 2f))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private boolean isBlurEnabled(float showProgress) {
        float alpha = getBlurAlpha(showProgress);
        return alpha > 0f && getBlurRadius() > 0f;
    }

    private float getBlurRadius() {
        return Interface.INSTANCE != null && Interface.INSTANCE.blurStrength.getValue() != null
                ? Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue()))
                : 0f;
    }

    private float getRadius() {
        return 4f;
    }

    private int getBackgroundAlpha(float showProgress) {
        int base = 175;
        int alpha = Math.round(Math.min(255f, Math.max(0f, base)) * clamp01(showProgress));
        return Math.max(0, Math.min(255, alpha));
    }

    private float getBlurAlpha(float showProgress) {
        return clamp01(showProgress);
    }

    public int getTotalCount(Item item) {
        if (mc == null || mc.player == null) return 0;
        var inv = mc.player.getInventory();
        int total = 0;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty() && st.getItem() == item) {
                total += st.getCount();
            }
        }

        ItemStack off = mc.player.getOffHandStack();
        if (!off.isEmpty() && off.getItem() == item) total += off.getCount();

        return total;
    }

    public float smoothAnimation(float speed, float current, float target, long lastTimeMillis) {
        long now = System.currentTimeMillis();
        float dt = lastTimeMillis == 0 ? 0f : (now - lastTimeMillis) / 1000f;
        float t = 1f - (float) Math.exp(-speed * dt);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return current + (target - current) * t;
    }

    public void rectWatermarkStyle(DrawContext context, float x, float y, float width, float height, float show) {
        float s = clamp01(show);
        float radius = 4f; // Меньший радиус для минимализма
        float outlineWidth = 1f; // Тонкая обводка

        // Сначала рисуем градиентную обводку (внешний прямоугольник)
        renderGradientOutline(context.getMatrices().peek().getPositionMatrix(), x, y, width, height, radius, s, outlineWidth);

        // Потом рисуем черный фон поверх (внутренний прямоугольник)
        int bgAlpha = (int) (180 * s);
        int blackColor = ColorUtils.setAlpha(ColorUtils.rgb(0, 0, 0), bgAlpha);
        
        ClientApi.rectangle()
                .size(new SizeState(width - outlineWidth * 2, height - outlineWidth * 2))
                .color(new QuadColorState(blackColor))
                .radius(new QuadRadiusState(Math.max(0f, radius - outlineWidth)))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + outlineWidth, y + outlineWidth);
    }

    public void rectWatermarkStyle(Matrix4f matrix, float x, float y, float width, float height, float show) {
        float s = clamp01(show);
        float radius = 4f; // Меньший радиус для минимализма
        float outlineWidth = 1f; // Тонкая обводка

        // Сначала рисуем градиентную обводку (внешний прямоугольник)
        renderGradientOutline(matrix, x, y, width, height, radius, s, outlineWidth);

        // Потом рисуем черный фон поверх (внутренний прямоугольник)
        int bgAlpha = (int) (180 * s);
        int blackColor = ColorUtils.setAlpha(ColorUtils.rgb(0, 0, 0), bgAlpha);
        
        ClientApi.rectangle()
                .size(new SizeState(width - outlineWidth * 2, height - outlineWidth * 2))
                .color(new QuadColorState(blackColor))
                .radius(new QuadRadiusState(Math.max(0f, radius - outlineWidth)))
                .build()
                .render(matrix, x + outlineWidth, y + outlineWidth);
    }

    private void renderGradientOutline(Matrix4f matrix, float x, float y, float width, float height, float radius, float alpha, float outlineWidth) {
        int themeColor = Interface.INSTANCE.getMainColor();
        
        // Создаем градиент из 4 углов
        int[] gradientColors = createThemeGradient(themeColor);
        
        // Используем анимированный градиент для каждого угла
        int topLeftColor = ColorUtils.gradient(30, 0, gradientColors);
        int topRightColor = ColorUtils.gradient(30, 25, gradientColors);
        int bottomRightColor = ColorUtils.gradient(30, 50, gradientColors);
        int bottomLeftColor = ColorUtils.gradient(30, 75, gradientColors);
        
        int outlineAlpha = (int) (255 * alpha);
        topLeftColor = ColorUtils.setAlpha(topLeftColor, outlineAlpha);
        topRightColor = ColorUtils.setAlpha(topRightColor, outlineAlpha);
        bottomRightColor = ColorUtils.setAlpha(bottomRightColor, outlineAlpha);
        bottomLeftColor = ColorUtils.setAlpha(bottomLeftColor, outlineAlpha);
        
        // Рисуем внешний прямоугольник с градиентом
        ClientApi.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(topLeftColor, topRightColor, bottomRightColor, bottomLeftColor))
                .radius(new QuadRadiusState(radius))
                .build()
                .render(matrix, x, y);
    }

    public void renderGradientText(Matrix4f matrix, String text, float x, float y, float size, int themeColor) {
        int[] gradientColors = createThemeGradient(themeColor);
        float currentX = x;
        
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            float charWidth = ClientApi.inter().getWidth(character, size);
            
            int color = ColorUtils.gradient(30, i * 5, gradientColors);
            
            ClientApi.text()
                    .font(ClientApi.inter())
                    .text(character)
                    .color(color)
                    .size(size)
                    .build()
                    .render(matrix, currentX, y);
            
            currentX += charWidth;
        }
    }

    public int[] createThemeGradient(int baseColor) {
        if (cachedGradient != null && cachedGradientBaseColor == baseColor) {
            return cachedGradient;
        }
        int r = ColorUtils.red(baseColor);
        int g = ColorUtils.green(baseColor);
        int b = ColorUtils.blue(baseColor);
        
        int color1 = ColorUtils.rgb(
            Math.min(255, (int)(r * 1.5)),
            Math.min(255, (int)(g * 1.5)),
            Math.min(255, (int)(b * 1.5))
        );
        int color2 = ColorUtils.rgb(
            Math.min(255, (int)(r * 1.35)),
            Math.min(255, (int)(g * 1.35)),
            Math.min(255, (int)(b * 1.35))
        );
        int color3 = ColorUtils.rgb(
            Math.min(255, (int)(r * 1.2)),
            Math.min(255, (int)(g * 1.2)),
            Math.min(255, (int)(b * 1.2))
        );
        int color4 = ColorUtils.rgb(
            Math.min(255, (int)(r * 1.1)),
            Math.min(255, (int)(g * 1.1)),
            Math.min(255, (int)(b * 1.1))
        );
        int color5 = baseColor;
        int color6 = ColorUtils.rgb(
            (int)(r * 0.9),
            (int)(g * 0.9),
            (int)(b * 0.9)
        );
        int color7 = ColorUtils.rgb(
            (int)(r * 0.8),
            (int)(g * 0.8),
            (int)(b * 0.8)
        );
        int color8 = ColorUtils.rgb(
            (int)(r * 0.7),
            (int)(g * 0.7),
            (int)(b * 0.7)
        );
        int color9 = ColorUtils.rgb(
            (int)(r * 0.6),
            (int)(g * 0.6),
            (int)(b * 0.6)
        );
        
        cachedGradientBaseColor = baseColor;
        cachedGradient = new int[]{color1, color2, color3, color4, color5, color6, color7, color8, color9, color8, color7, color6, color5, color4, color3, color2};
        return cachedGradient;
    }
}
