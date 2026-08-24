package dev.client.api.nullcry.uiClient.draggables.core;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.helper.player.PotionUtil;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingProvider;
import dev.client.api.nullcry.uiClient.draggables.DraggableHeaderRenderer;
import dev.client.api.nullcry.uiClient.draggables.GlassShadow;
import dev.client.api.nullcry.uiClient.draggables.IHelper;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Potions implements IHelper, SettingProvider {
    final DraggableHandler draggableHandler;

    private final List<PotionAnimation> potionAnimations = new ArrayList<>();
    private final List<Setting> settings = new ArrayList<>();
    public CompactAnimation potionsWidth = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation potionsHeight = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation showAnim = new CompactAnimation(Easing.EASE_OUT_CUBIC, 400);

    public final CheckBox showAlways = new CheckBox("Показывать всегда", () -> true)
            .defaultValue(false)
            .register(this);

    public final ColorPicker bgColor = new ColorPicker("Цвет фона", () -> true)
            .defaultValue(0x00000000)
            .register(this);

    public final Slider bgAlpha = new Slider("Прозрачность фона", () -> true)
            .set(0f, 255f, 1f)
            .applyDefault(180f)
            .register(this);

    public final Slider borderRadius = new Slider("Скругление углов", () -> true)
            .set(0f, 20f, 0.5f)
            .applyDefault(4f)
            .register(this);

    public final CheckBox glass = new CheckBox("Стекло", () -> true)
            .defaultValue(false)
            .register(this);

    public Potions() {
        this.draggableHandler = addDraggable("Potions", 4, 136);
        this.draggableHandler.setActiveCondition(() -> {
            return dev.client.modules.core.hud.HudModuleHelper.isPotionsEnabled();
        });
        potionsWidth.setValue(draggableHandler.getWidth());
        potionsHeight.setValue(draggableHandler.getHeight());
        showAnim.setValue(0.0);
        this.draggableHandler.setSettingsPanel(new DraggableSettingsPanel(draggableHandler, this));
    }

    float width;
    float height;

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        float x = draggableHandler.getX();
        float y = draggableHandler.getY();
        float baseWidth = 80f;

        List<StatusEffectInstance> effects = getActiveEffects();

        boolean chatOpen = mc.inGameHud.getChatHud().isChatFocused();
        boolean alwaysShow = showAlways.getEnabled();
        boolean shouldRender = chatOpen || !effects.isEmpty() || alwaysShow;
        showAnim.run(shouldRender ? 1.0 : 0.0);
        showAnim.update();
        float show = (float) showAnim.getValue();
        if (show <= 0f) return;

        AtomicReference<Float> targetWidth = new AtomicReference<>(baseWidth);
        AtomicReference<Float> targetHeight = new AtomicReference<>(21f);

        for (StatusEffectInstance statusEffectInstance : effects) {
            String name = statusEffectInstance.getEffectType().value().getName().getString() + getAmplifier(statusEffectInstance.getAmplifier());
            String duration = StatusEffectUtil.getDurationText(statusEffectInstance, 1.0F, 20F).getString();

            float nameWidth = ClientApi.inter().getWidth(name, 7);
            float durationWidth = ClientApi.inter().getWidth(duration, 7);

            float iconWidth = statusEffectInstance.shouldShowIcon() ? 9f + 3f : 0f;
            float iconDividerExtra = statusEffectInstance.shouldShowIcon() ? 3f : 0f;

            float animationOffset = 15f;
            float padding = 7f;
            float safety = 8f;

            float totalWidth = iconWidth
                    + iconDividerExtra
                    + nameWidth
                    + durationWidth
                    + animationOffset
                    + (padding * 2)
                    + safety;

            if (totalWidth > targetWidth.get()) {
                targetWidth.set(totalWidth);
            }
            targetHeight.set(targetHeight.get() + 13f);
        }

        potionsWidth.run(targetWidth.get());
        potionsHeight.run(targetHeight.get());
        potionsWidth.update();
        potionsHeight.update();

        float animatedWidth = (float) potionsWidth.getValue();
        float animatedHeight = (float) potionsHeight.getValue();

        width = animatedWidth;
        height = animatedHeight;

        draggableHandler.setWidth(animatedWidth);
        draggableHandler.setHeight(animatedHeight);

        List<StatusEffectInstance> current = getActiveEffects();
        for (StatusEffectInstance effect : current) {
            potionAnimations.removeIf(ae -> ae.effect.getEffectType() == effect.getEffectType() && (ae.effect.getDuration() != effect.getDuration() || ae.effect.getAmplifier() != effect.getAmplifier()));
            boolean exists = potionAnimations.stream().anyMatch(ae -> ae.isSame(effect));
            if (!exists) {
                PotionAnimation pa = new PotionAnimation(effect);
                pa.show();
                potionAnimations.add(pa);
            }
        }

        potionAnimations.forEach(ae -> {
            boolean stillActive = current.stream().anyMatch(ae::isSame);
            if (!stillActive) ae.hide();
        });

        potionAnimations.removeIf(PotionAnimation::isFinished);
        potionAnimations.forEach(PotionAnimation::tick);

        float headerHeight = 20f;
        float radius = borderRadius.getValue();
        int bgColorInt = bgColor.getColorRGBA();
        boolean glassEnabled = glass.getEnabled();
        int bgA = glassEnabled ? 0 : bgAlpha.getValue().intValue();

        float s = show;

        float normalizedAlpha = Math.min(1f, bgA / 255f);
        int outlineAlpha = (int) (255 * s * normalizedAlpha);

        if (glassEnabled) {
            float outlineExpand = 2f;
            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius + outlineExpand))
                    .size(new SizeState(width + outlineExpand * 2, height + outlineExpand * 2))
                    .alpha(s * 0.6f)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x - outlineExpand, y - outlineExpand);

            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius))
                    .size(new SizeState(width, height))
                    .alpha(s)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y);

            GlassShadow.render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y, width, height, radius, s);
        }

        if (bgA > 0) {
            int outlineColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), outlineAlpha);

            ClientApi.rectangle()
                    .size(new SizeState(width, height))
                    .color(new QuadColorState(outlineColor))
                    .radius(new QuadRadiusState(radius))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y);

            float innerPad = 1f;
            int fillColor = ColorUtils.setAlpha(bgColorInt, (int) (bgA * s));

            ClientApi.rectangle()
                    .size(new SizeState(width - innerPad * 2, height - innerPad * 2))
                    .color(new QuadColorState(fillColor))
                    .radius(new QuadRadiusState(Math.max(0f, radius - innerPad)))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x + innerPad, y + innerPad);
        }

        DraggableHeaderRenderer.render(
                event,
                x,
                y,
                width,
                show,
                headerHeight,
                "G",
                8f,
                "Potions",
                8f,
                -1,
                -1,
                () -> Interface.INSTANCE.getMainColor()
        );

        AtomicReference<Float> offset = new AtomicReference<>(0f);
        ScissorUtil.enableContext(event.getContext(), x, y, animatedWidth, animatedHeight);

        potionAnimations.sort((a, b) -> {
            boolean aDead = getActiveEffects().stream().noneMatch(a::isSame);
            boolean bDead = getActiveEffects().stream().noneMatch(b::isSame);
            if (aDead && !bDead) return -1;
            if (!aDead && bDead) return 1;
            return a.effect.getEffectType().value().getName().getString().compareTo(b.effect.getEffectType().value().getName().getString());
        });

        for (PotionAnimation potionAnimation : potionAnimations) {
            float progress = potionAnimation.getProgress();
            if (progress <= 0f) continue;
            StatusEffectInstance effect = potionAnimation.effect;

            String name = effect.getEffectType().value().getName().getString() + getAmplifier(effect.getAmplifier());
            String duration = StatusEffectUtil.getDurationText(effect, 1.0F, 20F).getString();

            float durationWidth = ClientApi.inter().getWidth(duration, 7);
            float padding = 7f;

            float itemHeight = 13f;
            float itemY = y + headerHeight + offset.get();

            float nameX = x + padding;
            float durationX = x + animatedWidth - durationWidth - padding;
            float baseY = itemY + (itemHeight - 7f) / 2f - 0.5f;

            int baseAlpha = (int) (255 * progress);
            int pulseAlpha = (int) ((Math.sin(System.currentTimeMillis() / 100.0) * 0.5 + 0.5) * 255 * progress);

            boolean isLowDuration = !effect.isInfinite() && effect.getDuration() <= 200;
            boolean isBad = PotionUtil.isBadEffect(effect);

            int themeColor = Interface.INSTANCE.getMainColor();

            float nameSlideX = nameX + 14f - (1f - progress) * 15f;
            float durationSlideX = durationX + (1f - progress) * 15f;

            if (effect.shouldShowIcon()) {
                StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();
                Sprite sprite = spriteManager.getSprite(effect.getEffectType());

                float effectIconX = x + 6.5f;
                float iconSlideX = effectIconX - (1f - progress) * 15f;
                float iconY = baseY + 0.5f;

                event.getContext().drawSpriteStretched(
                        RenderLayer::getGuiTextured,
                        sprite,
                        (int) iconSlideX,
                        (int) iconY,
                        9,
                        9,
                        ColorUtils.setAlpha(-1, (int) (255 * progress))
                );

                ClientApi.rectangle()
                        .size(new SizeState(1f, 7f))
                        .color(new QuadColorState(ColorUtils.setAlpha(themeColor, (int) (255 * progress))))
                        .radius(new QuadRadiusState(0f))
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), Math.round(iconSlideX + 10f), Math.round(baseY) + 0.5f);
            }

            if (isLowDuration && isBad) {
                int warningColor = ColorUtils.rgba(255, 30, 30, pulseAlpha);
                ClientApi.text()
                        .size(7)
                        .font(ClientApi.inter())
                        .text(name)
                        .color(warningColor)
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameSlideX, baseY);
            } else if (isBad) {
                int badColor = ColorUtils.rgba(255, 85, 85, baseAlpha);
                ClientApi.text()
                        .size(7)
                        .font(ClientApi.inter())
                        .text(name)
                        .color(badColor)
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameSlideX, baseY);
            } else {
                int nameColor = ColorUtils.setAlpha(-1, isLowDuration ? pulseAlpha : baseAlpha);
                ClientApi.text()
                        .font(ClientApi.inter())
                        .text(name)
                        .color(nameColor)
                        .size(7)
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameSlideX, baseY);
            }

            int durationColor = isLowDuration ? ColorUtils.setAlpha(-1, pulseAlpha) : ColorUtils.setAlpha(-1, baseAlpha);
            ClientApi.text()
                    .size(7)
                    .font(ClientApi.inter())
                    .text(duration)
                    .color(durationColor)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), durationSlideX, baseY);

            offset.set(offset.get() + 13f * potionAnimation.getHeightProgress());
        }

        ScissorUtil.disableContext(event.getContext());
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }

    private static List<StatusEffectInstance> getActiveEffects() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return List.of();

        boolean fullbrightActive = dev.client.Just.getInstance() != null
                && dev.client.Just.getInstance().getModuleManager() != null
                && dev.client.Just.getInstance().getModuleManager().stream()
                    .anyMatch(m -> m.getName().equals("Fullbright") && m.isEnabled());

        return player.getStatusEffects().stream()
                .filter(e -> !fullbrightActive || e.getEffectType() != net.minecraft.entity.effect.StatusEffects.NIGHT_VISION)
                .toList();
    }

    private static String getAmplifier(int amplifier) {
        return amplifier > 0 ? " " + (amplifier + 1) : "";
    }

    static class PotionAnimation {
        public final StatusEffectInstance effect;
        public final CompactAnimation anim = new CompactAnimation(Easing.EASE_OUT_QUAD, 250);

        public PotionAnimation(StatusEffectInstance effect) {
            this.effect = effect;
            anim.setValue(0.0);
        }

        public void tick() {
            anim.update();
        }

        public float getProgress() {
            return (float) anim.getValue();
        }

        public void show() {
            anim.run(1.0);
        }

        public void hide() {
            anim.run(0.0);
        }

        public boolean isSame(StatusEffectInstance other) {
            return effect.getEffectType() == other.getEffectType()
                    && effect.getDuration() == other.getDuration()
                    && effect.getAmplifier() == other.getAmplifier();
        }

        public boolean isFinished() {
            return anim.getDestinationValue() == 0.0 && anim.isDone() && getProgress() <= 0.001f;
        }

        public float getHeightProgress() {
            return anim.getDestinationValue() == 0.0 ? getProgress() : 1f;
        }
    }
}
