package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.sliderSetting;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

public class SliderComponent extends SettingComponent {
    private static final float DEFAULT_CLIP_PADDING = 21.5f;
    private static final float VALUE_GAP = 4f;
    private static final float TRACK_LEFT_OFFSET = 0.9f;
    private static final float TRACK_RIGHT_INSET = 2f;
    boolean isHovered;
    float animatedSliderWidth = 0f;
    float textScroll = 0f;
    long scrollTimer = System.currentTimeMillis();
    private float lastClipPadding = DEFAULT_CLIP_PADDING;
    private float lastTrackWidth = 0f;
    private float lastTrackX = 0f;

    public SliderComponent(Slider slider) {
        super(slider);
    }

    @Override
    public void init() {
        float moduleNameHeight = ClientApi.inter().getHeight(getSetting().getName(), 7) + 7;
        size(240f / 2 - 10, moduleNameHeight);
    }

    @Override
    public SliderComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        Slider slider = (Slider) getSetting();
        if (slider.getDragging()) update(mouseX);

        final String name = getSetting().getName();
        float visibility = getGlobalAlpha();
        final float nameSize = 7f;
        final float clipX = getX();
        final float clipY = getY() - 1.5f;

        String valueText = null;
        float valueBoxWidth = 0f;
        if (Objects.nonNull(slider.getValue())) {
            valueText = String.format("%.1f", slider.getValue());
            valueBoxWidth = 10 + ClientApi.inter().getWidth(valueText, 6);
        }

        final float clipPadding = clipPadding(DEFAULT_CLIP_PADDING);
        lastClipPadding = clipPadding;
        final float paddingDelta = DEFAULT_CLIP_PADDING - clipPadding;
        final float clipBaseRight = getX() + getWidth() - clipPadding;
        float clipRight = clipBaseRight;
        float valueBoxX = getX() + getWidth() - valueBoxWidth;
        if (valueBoxWidth > 0f) {
            valueBoxX += paddingDelta;
            clipRight = Math.min(clipBaseRight, valueBoxX - VALUE_GAP);
        }
        clipRight = Math.max(clipRight, clipX);
        final float clipW = Math.max(0f, clipRight - clipX);
        final float clipH = ClientApi.inter().getHeight(name, nameSize) + 4f;
        final float textWidth = ClientApi.inter().getWidth(name, nameSize);

        final boolean isHoveredTextRaw = MouseClick.isClick(mouseX, mouseY, clipX, clipY, clipW, clipH);
        final boolean isDragging = slider.getDragging();
        final boolean isHoveredText = isDragging ? false : isHoveredTextRaw;

        final float scrollSpeed = 1.25f;
        final float loopOffset = textWidth + 20f;

        if (textWidth > clipW) {
            if (isHoveredText) {
                if (System.currentTimeMillis() - scrollTimer > 15L) {
                    textScroll += scrollSpeed;
                    if (textScroll > loopOffset) textScroll = 0f;
                    scrollTimer = System.currentTimeMillis();
                }
            } else {
                if (textScroll > loopOffset / 2f) {
                    textScroll = MathUtil.fast(textScroll, loopOffset, 10f);
                    if (textScroll >= loopOffset - 0.5f) textScroll = 0f;
                } else {
                    textScroll = MathUtil.fast(textScroll, 0f, 10f);
                }
            }
        } else {
            textScroll = 0f;
        }

        ScissorUtil.enable(clipX, clipY, clipW, clipH);

        final float baseX = clipX - textScroll;
        ClientApi.text()
                .size(nameSize)
                .color(ColorUtils.setAlpha(-1, Math.round(ColorUtils.getAlpha(-1) * visibility)))
                .text(name)
                .font(ClientApi.inter())
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), baseX, clipY);

        if (textWidth > clipW) {
            ClientApi.text()
                    .size(nameSize)
                    .color(ColorUtils.setAlpha(-1, Math.round(ColorUtils.getAlpha(-1) * visibility)))
                    .text(name)
                    .font(ClientApi.inter())
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), baseX + loopOffset, clipY);
        }

        ScissorUtil.disable();

        if (valueBoxWidth > 0f && valueText != null) {
            ClientApi.outline()
                    .size(new SizeState(valueBoxWidth, 9))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(ColorUtils.rgba(255, 255, 255, Math.round(64 * visibility))))
                    .thickness(-1f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), valueBoxX, getY() - 1);

            ClientApi.text()
                    .font(ClientApi.inter())
                    .text(valueText)
                    .color(ColorUtils.setAlpha(-1, Math.round(ColorUtils.getAlpha(-1) * visibility)))
                    .size(6)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), valueBoxX + 4, getY());
        }

        float trackW = getTrackWidth(paddingDelta);
        float trackH = 5f;
        float trackX = getX() - TRACK_LEFT_OFFSET;
        float trackY = getY() + getHeight() - 6f;

        lastTrackWidth = trackW;
        lastTrackX = trackX;

        boolean trackHovered = MouseClick.isClick(mouseX, mouseY, trackX, getY() + getHeight() - 5, trackW, 5);
        float trackState = slider.getDragging() ? 1f : (trackHovered ? 0.6f : 0f);
        int trackAlpha = Math.round((70f + (120f - 70f) * trackState) * visibility);
        trackAlpha = Math.max(0, Math.min(255, trackAlpha));

        if (Interface.INSTANCE.blurStrength.getValue() > 0) {
            ClientApi.blur()
                    .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                    .radius(new QuadRadiusState(1f))
                    .size(new SizeState(trackW, trackH))
                    .alpha(visibility)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), trackX, trackY);
        }

        ClientApi.rectangle()
                .size(new SizeState(trackW, trackH))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), (int) (trackAlpha * 0.6f)),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), (int) (trackAlpha * 0.6f)),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), (int) (trackAlpha * 0.6f)),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), (int) (trackAlpha * 0.6f))
                ))
                .radius(new QuadRadiusState(1f))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), trackX, trackY);

        if (Objects.nonNull(slider.getValue())) {
            float fraction = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
            float targetSliderWidth = trackW * fraction;
            animatedSliderWidth = MathUtil.linear(animatedSliderWidth, targetSliderWidth, 15f);
            if (animatedSliderWidth > trackW) animatedSliderWidth = trackW;

            if  (Interface.INSTANCE.blurStrength.getValue() > 0) {
                ClientApi.blur()
                        .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                        .radius(new QuadRadiusState(1f))
                        .size(new SizeState(animatedSliderWidth, trackH))
                        .alpha(visibility)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), trackX, trackY);
            }

            ClientApi.rectangle()
                    .size(new SizeState(animatedSliderWidth, trackH))
                    .color(new QuadColorState(ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), trackAlpha)))
                    .radius(new QuadRadiusState(1f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), trackX, trackY);
        }

        isHovered = trackHovered;
        if (!isDragging && (isHovered || isHoveredText)) {
            CursorsUtil.setCursor(CursorsUtil.IBEAM);
        }

        return null;
    }

    void update(double mouseX) {
        Slider slider = (Slider) getSetting();
        float trackW = lastTrackWidth > 0f ? lastTrackWidth : getTrackWidth(DEFAULT_CLIP_PADDING - lastClipPadding);
        float trackX = lastTrackX != 0f ? lastTrackX : getX() - TRACK_LEFT_OFFSET;
        if (trackW <= 0f) return;

        float clampedMouseX = (float) MathHelper.clamp(mouseX, trackX, trackX + trackW);
        float newValue = slider.getMin() + ((clampedMouseX - trackX) / trackW) * (slider.getMax() - slider.getMin());
        newValue = Math.round(newValue / slider.getIncrements()) * slider.getIncrements();
        newValue = Math.max(slider.getMin(), Math.min(slider.getMax(), newValue));
        slider.applyDefault(newValue);
    }

    private float getTrackWidth(float paddingDelta) {
        return Math.max(0f, getWidth() - (TRACK_LEFT_OFFSET + TRACK_RIGHT_INSET) + paddingDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float trackW = lastTrackWidth > 0f ? lastTrackWidth : getTrackWidth(DEFAULT_CLIP_PADDING - lastClipPadding);
        float trackH = 5f;
        float trackX = lastTrackX != 0f ? lastTrackX : getX() - TRACK_LEFT_OFFSET;
        float trackY = getY() + getHeight() - 6f;

        if (trackW <= 0f) return false;

        if (isHovered) {
            if (MouseClick.isClick(mouseX, mouseY, trackX, trackY + 1f, trackW, trackH)) {
                ((Slider) getSetting()).setDragging(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Slider slider = (Slider) getSetting();
        slider.setDragging(false);
        return false;
    }
}
