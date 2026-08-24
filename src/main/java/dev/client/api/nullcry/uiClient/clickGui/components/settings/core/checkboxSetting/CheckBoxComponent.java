package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.checkboxSetting;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.nova.extended.Animation;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;

public class CheckBoxComponent extends SettingComponent {
    boolean isHovered;
    float textScroll = 0;
    long scrollTimer = System.currentTimeMillis();

    public CheckBoxComponent(Setting setting) {
        super(setting);
    }

    @Override
    public void init() {
        float moduleNameHeight = ClientApi.inter().getHeight(getSetting().getName(), 7) + 5;
        size(240f / 2 - 10, moduleNameHeight);
    }

    @Override
    public CheckBoxComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        String text = getSetting().getName();
        float textSize = 7f;
        final float defaultClipPadding = 18f;
        float clipPadding = clipPadding(defaultClipPadding);

        float visibility = getGlobalAlpha();
        float textX = getX();
        float textY = getY() - 1f;

        float iconInset = defaultClipPadding - 16f;
        float iconX = getX() + getWidth() - clipPadding + iconInset - 6f;
        float iconY = getY() - 1.5f;
        float boxW = 19f;
        float boxH = 10f;
        float knobSize = 7.4f;

        float maxTextWidth = Math.max(0f, iconX - getX() - 1.8f);

        float textWidth = ClientApi.inter().getWidth(text, textSize);
        float textHeight = ClientApi.inter().getHeight(text, textSize);

        float scrollSpeed = 1.25f;
        float loopOffset = textWidth + 20f;

        boolean isHoveredText = MouseClick.isClick(mouseX, mouseY, textX, getY(), maxTextWidth, textHeight);

        if (textWidth > maxTextWidth) {
            if (isHoveredText) {
                if (System.currentTimeMillis() - scrollTimer > 15) {
                    textScroll += scrollSpeed;
                    if (textScroll > loopOffset) {
                        textScroll = 0;
                    }
                    scrollTimer = System.currentTimeMillis();
                }
            } else {
                if (textScroll > loopOffset / 2f) {
                    textScroll = Animation.fast(textScroll, loopOffset, 10);
                    if (textScroll >= loopOffset - 0.5f) {
                        textScroll = 0;
                    }
                } else {
                    textScroll = Animation.fast(textScroll, 0, 10);
                }
            }
        } else {
            textScroll = 0;
        }

        ScissorUtil.enable(textX, getY() - 1f, maxTextWidth, getHeight());
        ClientApi.text()
                .size(textSize)
                .color(ColorUtils.setAlpha(-1, Math.round(ColorUtils.getAlpha(-1) * visibility)))
                .text(text)
                .font(ClientApi.inter())
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), textX - textScroll, textY);

        if (textWidth > maxTextWidth) {
            ClientApi.text()
                    .size(textSize)
                    .color(ColorUtils.setAlpha(-1, Math.round(ColorUtils.getAlpha(-1) * visibility)))
                    .text(text)
                    .font(ClientApi.inter())
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), textX - textScroll + loopOffset, textY);
        }

        ScissorUtil.disable();

        boolean isEnabled = ((CheckBox) getSetting()).getEnabled();
        float animationValue = getSetting().getAnimation().getOutput().floatValue();

        float backgroundAlpha = 150f + 25f * animationValue;
        int backgroundAlphaInt = Math.round(backgroundAlpha * visibility);

        if (Interface.INSTANCE.blurStrength.getValue() > 0) {
            ClientApi.blur()
                    .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                    .radius(new QuadRadiusState(4f))
                    .size(new SizeState(boxW, boxH))
                    .alpha(visibility)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), iconX, iconY + 0.5f);
        }

        ClientApi.rectangle()
                .size(new SizeState(boxW, boxH))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), backgroundAlphaInt),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), backgroundAlphaInt),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), backgroundAlphaInt),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), backgroundAlphaInt)
                ))
                .radius(new QuadRadiusState(4f))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), iconX, iconY + 0.5f);

        float knobRadius = knobSize / 2.5f;
        float knobX = iconX + 1.5f + (animationValue * (boxW - knobSize - 2.9f));
        float knobY = iconY + 1.7f;

        int knobColor = ColorUtils.setAlpha(
                isEnabled ? Interface.INSTANCE.getMainColor() : ColorUtils.rgb(120, 120, 120),
                (int) (255 * visibility)
        );

        ClientApi.rectangle()
                .size(new SizeState(knobSize, knobSize))
                .radius(new QuadRadiusState(knobRadius))
                .color(new QuadColorState(knobColor))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), knobX, knobY);

        isHovered = MouseClick.isClick(mouseX, mouseY, iconX, iconY, boxW, boxH);
        if (isHovered) {
            CursorsUtil.setCursor(CursorsUtil.HAND);
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final float defaultClipPadding = 21f;
        float clipPadding = clipPadding(defaultClipPadding - 3f);
        float iconInset = defaultClipPadding - 16f;
        float iconX = getX() + getWidth() - clipPadding + iconInset - 6f;
        float iconY = getY() - 1.5f;
        float boxW = 19f;
        float boxH = 10f;

        boolean hit = MouseClick.isClick(mouseX, mouseY, iconX, iconY, boxW, boxH);
        if (hit) {
            CheckBox checkBox = (CheckBox) getSetting();
            checkBox.applyDefault(!checkBox.getEnabled());
            return true;
        }
        return false;
    }
}
