package dev.client.api.nullcry.uiClient.draggables.core;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.helper.player.PlayerUtil;
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
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Cooldowns implements IHelper, SettingProvider {
    final DraggableHandler draggableHandler;

    private final List<CooldownsAnimation> cooldownsAnimations = new ArrayList<>();
    private final List<Setting> settings = new ArrayList<>();
    public CompactAnimation cooldownsWidth = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation cooldownsHeight = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
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

    public Cooldowns() {
        this.draggableHandler = addDraggable("Cooldowns", 4, 180);
        this.draggableHandler.setActiveCondition(() -> {
            return dev.client.modules.core.hud.HudModuleHelper.isCooldownsEnabled();
        });
        cooldownsWidth.setValue(draggableHandler.getWidth());
        cooldownsHeight.setValue(draggableHandler.getHeight());
        showAnim.setValue(0.0);
        this.draggableHandler.setSettingsPanel(new DraggableSettingsPanel(draggableHandler, this));
    }

    float width;
    float height;

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        float x = draggableHandler.getX();
        float y = draggableHandler.getY();

        Map<ItemStack, Double> items = PlayerUtil.getCooldownItems();
        for (Map.Entry<ItemStack, Double> entry : items.entrySet()) {
            boolean exists = cooldownsAnimations.stream().anyMatch(ca -> ca.isSame(entry.getKey()));
            if (!exists) {
                CooldownsAnimation ca = new CooldownsAnimation(entry.getKey());
                ca.show();
                cooldownsAnimations.add(ca);
            }
        }
        cooldownsAnimations.forEach(anim -> {
            boolean stillExists = items.keySet().stream().anyMatch(anim::isSame);
            if (!stillExists) anim.hide();
        });
        cooldownsAnimations.removeIf(CooldownsAnimation::isFinished);
        cooldownsAnimations.forEach(CooldownsAnimation::tick);

        boolean chatOpen = mc.inGameHud.getChatHud().isChatFocused();
        boolean alwaysShow = showAlways.getEnabled();
        boolean shouldRender = chatOpen || !cooldownsAnimations.isEmpty() || alwaysShow;
        showAnim.run(shouldRender ? 1.0 : 0.0);
        showAnim.update();
        float show = (float) showAnim.getValue();
        if (show <= 0f) return;
        float baseWidth = 80f;
        float baseHeight = 21f;
        float rowH = 13f;

        float targetWidth = baseWidth;
        for (Map.Entry<ItemStack, Double> entry : items.entrySet()) {
            String name = entry.getKey().getName().getString();
            String duration = String.format("%.1f", entry.getValue());

            float nameW = ClientApi.inter().getWidth(name, 7);
            float timeW = ClientApi.inter().getWidth(duration, 7);

            float totalW = 5f + 11.2f + 2f + 1f + 4f + nameW + 6f + timeW + 6f;
            if (totalW > targetWidth) targetWidth = totalW;
        }

        float targetHeight = baseHeight;
        for (CooldownsAnimation a : cooldownsAnimations) {
            targetHeight += rowH * a.getHeightProgress();
        }

        cooldownsWidth.run(targetWidth);
        cooldownsHeight.run(targetHeight);
        cooldownsWidth.update();
        cooldownsHeight.update();

        float animatedWidth = (float) cooldownsWidth.getValue();
        float animatedHeight = (float) cooldownsHeight.getValue();

        width = animatedWidth;
        height = animatedHeight;

        draggableHandler.setWidth(animatedWidth);
        draggableHandler.setHeight(animatedHeight);

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
                "I",
                8f,
                "Cooldowns",
                8f,
                -1,
                -1,
                () -> Interface.INSTANCE.getMainColor()
        );

        AtomicReference<Float> offsetY = new AtomicReference<>(0f);
        ScissorUtil.enableContext(event.getContext(), x, y, animatedWidth, animatedHeight);

        for (CooldownsAnimation cooldownsAnimation : cooldownsAnimations) {
            float p = cooldownsAnimation.getProgress();
            if (p <= 0f) continue;

            ItemStack stack = cooldownsAnimation.stack;

            double timeLeft = 0.0;
            for (Map.Entry<ItemStack, Double> e : items.entrySet()) {
                if (ItemStack.areItemsEqual(e.getKey(), stack)) {
                    timeLeft = e.getValue();
                    break;
                }
            }

            float itemY = y + headerHeight + offsetY.get();

            float itemX = x + 5f - (1f - p) * 10f;

            MathUtil.scaleStart(event.getContext().getMatrices(), itemX, itemY + 1.5f, 0.7f);
            event.getContext().drawItem(stack, (int) itemX, (int) (itemY + 1f));
            MathUtil.scaleEnd(event.getContext().getMatrices());

            ClientApi.rectangle()
                    .size(new SizeState(1f, 7f))
                    .color(new QuadColorState(ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (255 * p))))
                    .radius(new QuadRadiusState(0f))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), Math.round(itemX + 13f), Math.round(itemY + 2.5f) + 0.5f);

            String name = stack.getName().getString();
            String duration = String.format("%.1f", timeLeft);

            float textBaseY = itemY + (rowH - 7f) / 2f - 0.5f;
            float nameX = x + 6f + 16f - (1f - p) * 15f;
            float durationX = x + animatedWidth - ClientApi.inter().getWidth(duration, 7) - 6f + (1f - p) * 15f;

            int alpha = (int) (255 * p);
            int themeColor = Interface.INSTANCE.getMainColor();

            ClientApi.text()
                    .size(7)
                    .font(ClientApi.inter())
                    .text(name)
                    .color(ColorUtils.setAlpha(themeColor, alpha))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameX, textBaseY);

            ClientApi.text()
                    .size(7)
                    .font(ClientApi.inter())
                    .text(duration)
                    .color(ColorUtils.setAlpha(-1, alpha))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), durationX, textBaseY);

            offsetY.set(offsetY.get() + rowH * cooldownsAnimation.getHeightProgress());
        }

        ScissorUtil.disableContext(event.getContext());
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }

    static class CooldownsAnimation {
        public final ItemStack stack;
        public final CompactAnimation anim = new CompactAnimation(Easing.EASE_OUT_QUAD, 250);

        public CooldownsAnimation(ItemStack stack) {
            this.stack = stack;
            anim.setValue(0.0);
        }

        public void show() {
            anim.run(1.0);
        }

        public void tick() {
            anim.update();
        }

        public float getProgress() {
            return (float) anim.getValue();
        }

        public void hide() {
            anim.run(0.0);
        }

        public boolean isSame(ItemStack other) {
            return ItemStack.areItemsEqual(stack, other);
        }

        public boolean isFinished() {
            return anim.getDestinationValue() == 0.0 && anim.isDone() && getProgress() <= 0.001f;
        }

        public float getHeightProgress() {
            return anim.getDestinationValue() == 0.0 ? getProgress() : 1f;
        }
    }
}
