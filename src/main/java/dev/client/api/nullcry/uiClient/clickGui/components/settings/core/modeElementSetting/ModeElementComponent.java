package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.modeElementSetting;

import com.google.common.base.Suppliers;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

public class ModeElementComponent extends SettingComponent {
    private final Supplier<ModeElement> modeSetting = Suppliers.memoize(() -> (ModeElement) getSetting());
    boolean isHovered;

    public ModeElementComponent(Setting setting) {
        super(setting);
    }

    @Override
    public void init() {
        float nameH = ClientApi.inter().getHeight(getSetting().getName(), 7f) + 5f;
        size(240f / 2f - 10f, nameH + 12f);
    }

    @Override
    public ModeElementComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        ModeElement mode = modeSetting.get();
        float visibility = getGlobalAlpha();

        if (mode.getValue() == null && !mode.getValues().isEmpty()) {
            mode.set(mode.getValues().getFirst());
        }

        if (mode.getValues().isEmpty()) {
            float nameSize = 7f;
            float nameH = ClientApi.inter().getHeight(getSetting().getName(), nameSize);
            ClientApi.text()
                    .size(nameSize)
                    .color(ColorUtils.setAlpha(
                            -1,
                            Math.round(ColorUtils.getAlpha(-1) * visibility)
                    ))
                    .text(getSetting().getName())
                    .font(ClientApi.inter())
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), getX(), getY() - 1f);

            size(getWidth(), nameH + 6f);
            return null;
        }

        float nameSize = 7f;
        float nameH = ClientApi.inter().getHeight(getSetting().getName(), nameSize);
        ClientApi.text()
                .size(nameSize)
                .color(ColorUtils.setAlpha(
                        -1,
                        Math.round(ColorUtils.getAlpha(-1) * visibility)
                ))
                .text(getSetting().getName())
                .font(ClientApi.inter())
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), getX(), getY() - 1f);

        final float chipGapX = 2f;
        final float chipGapY = 2f;
        final float chipH = 12f;
        final float padX = 5f;
        final float textSize = 6f;

        float startX = getX() - 1;
        float x = startX;
        float y = getY() + nameH + 3f;
        float maxRight = getX() + getWidth() - 4;

        boolean anyHover = false;
        float lastBottom = y + chipH;
        int mainColor = Interface.INSTANCE.getMainColor();
        float minAlpha = 45f;
        float maxAlpha = 150f;

        for (String val : mode.getValues()) {
            String t = (val == null ? "N/A" : val);
            float tw = ClientApi.inter().getWidth(t, textSize);
            Integer previewColor = mode.getColor(val);
            float previewSize = previewColor != null ? chipH - 4f : 0f;
            float previewGap = previewColor != null ? 3f : 0f;
            float chipW = tw + padX * 2f + (previewColor != null ? previewSize + previewGap : 0f);

            if (x + chipW > maxRight) {
                x = startX;
                y += chipH + chipGapY;
            }

            boolean selected = mode.isSelected(val);
            boolean hovered = MouseClick.isClick(mouseX, mouseY, x, y, chipW, chipH);
            float state = selected ? 1f : (hovered ? 0.5f : 0f);
            int backgroundAlpha = Math.round((minAlpha + (maxAlpha - minAlpha) * state) * visibility);
            backgroundAlpha = Math.max(0, Math.min(255, backgroundAlpha));

            if (Interface.INSTANCE.blurStrength.getValue() > 0) {
                ClientApi.blur()
                        .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                        .radius(new QuadRadiusState(2f))
                        .size(new SizeState(chipW, chipH))
                        .alpha(visibility)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), x, y);
            }

            ClientApi.rectangle()
                    .size(new SizeState(chipW, chipH))
                    .color(new QuadColorState(
                            ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), (backgroundAlpha)),
                            ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), (backgroundAlpha)),
                            ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), (backgroundAlpha)),
                            ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), (backgroundAlpha))
                    ))
                    .radius(new QuadRadiusState(2f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x, y);

            int textColorBase = selected
                    ? -1
                    : ColorUtils.rgb(180, 180, 180);
            int textColor = ColorUtils.setAlpha(
                    textColorBase,
                    Math.round(ColorUtils.getAlpha(textColorBase) * visibility)
            );

            float textArea = chipW - padX * 2f - (previewColor != null ? previewSize + previewGap : 0f);
            float tx = (float) Math.round(x + padX + Math.max(0f, (textArea - tw) / 2f));
            float ty = y + (chipH - ClientApi.inter().getHeight(t, textSize)) / 2f - 0.5f;

            ClientApi.text()
                    .size(textSize)
                    .color(textColor)
                    .text(t)
                    .font(ClientApi.inter())
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), tx, ty);

            if (previewColor != null) {
                int previewCol = ColorUtils.setAlpha(
                        previewColor,
                        Math.round(ColorUtils.getAlpha(previewColor) * visibility)
                );
                float px = x + chipW - padX - previewSize;
                float py = y + (chipH - previewSize) / 2f;
                ClientApi.rectangle()
                        .radius(new QuadRadiusState(2f))
                        .size(new SizeState(previewSize, previewSize))
                        .color(new QuadColorState(previewCol))
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), px, py);
            }

            isHovered = hovered;
            if (isHovered) anyHover = true;

            x += chipW + chipGapX;
            lastBottom = y + chipH;
        }

        if (anyHover && mode.getValues().size() > 1) {
            CursorsUtil.setCursor(CursorsUtil.HAND);
        }

        size(getWidth(), (lastBottom + 2f) - getY());
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ModeElement mode = modeSetting.get();

        if (mode.getValues().size() <= 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        final float chipGapX = 2f;
        final float chipGapY = 2f;
        final float chipH = 12f;
        final float padX = 5f;
        final float textSize = 6f;

        float nameH = ClientApi.inter().getHeight(getSetting().getName(), 7f);
        float startX = getX() - 1f;
        float x = startX;
        float y = getY() + nameH + 3f;
        float maxWidth = getX() + getWidth() - 4;

        for (String val : mode.getValues()) {
            String t = (val == null ? "N/A" : val);
            float tw = ClientApi.inter().getWidth(t, textSize);
            Integer previewColor = mode.getColor(val);
            float previewSize = previewColor != null ? chipH - 4f : 0f;
            float previewGap = previewColor != null ? 3f : 0f;
            float chipW = tw + padX * 2f + (previewColor != null ? previewSize + previewGap : 0f);

            if (x + chipW > maxWidth) {
                x = startX;
                y += chipH + chipGapY;
            }

            if (MouseClick.isClick(mouseX, mouseY, x, y, chipW, chipH)) {
                if (val != null) mode.set(val);
                return true;
            }

            x += chipW + chipGapX;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
