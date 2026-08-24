package dev.client.api.nullcry.uiClient.draggables.core;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.client.keyboard.KeyboardStorage;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingProvider;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.api.nullcry.uiClient.draggables.DraggableHeaderRenderer;
import dev.client.api.nullcry.uiClient.draggables.IHelper;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import dev.client.modules.core.render.Interface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class Keybinds implements IHelper, SettingProvider {
    final DraggableHandler draggableHandler;

    private final List<KeybindAnimation> keybindAnimations = new ArrayList<>();
    private final List<Setting> settings = new ArrayList<>();
    public CompactAnimation keyBindsWidth = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation keyBindsHeight = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation showAnim = new CompactAnimation(Easing.EASE_OUT_CUBIC, 400);

    public final CheckBox showAlways = new CheckBox("Показывать всегда", () -> true)
            .defaultValue(false)
            .register(this);

    public Keybinds() {
        this.draggableHandler = addDraggable("Keybinds", 4, 114);
        this.draggableHandler.setActiveCondition(() -> {
            return dev.client.modules.core.hud.HudModuleHelper.isKeybindsEnabled();
        });
        keyBindsWidth.setValue(draggableHandler.getWidth());
        keyBindsHeight.setValue(draggableHandler.getHeight());
        showAnim.setValue(0.0);
        this.draggableHandler.setSettingsPanel(new DraggableSettingsPanel(draggableHandler, this));
    }

    Supplier<List<Module>> modules = () -> handlerClient().getModuleManager().getModules().stream()
            .filter(m -> m.getKey() != -1 && m.isEnabled())
            .toList();

    float width;
    float height;

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        float x = draggableHandler.getX();
        float y = draggableHandler.getY();
        float baseWidth = 80f;

        AtomicReference<Float> targetWidth = new AtomicReference<>(baseWidth);
        AtomicReference<Float> targetHeight = new AtomicReference<>(20f);

        boolean chatOpen = mc.inGameHud.getChatHud().isChatFocused();
        boolean hasKeybinds = handlerClient().getModuleManager().getModules().stream().anyMatch(m -> m.getKey() != -1 && m.isEnabled());
        boolean alwaysShow = showAlways.getEnabled();
        boolean shouldRender = chatOpen || hasKeybinds || alwaysShow;

        showAnim.run(shouldRender ? 1.0 : 0.0);
        showAnim.update();
        float show = (float) showAnim.getValue();
        if (show <= 0f) return;

        List<Module> currentModules = modules.get();

        for (Module module : currentModules) {
            KeybindAnimation anim = findAnim(module);
            if (anim == null) {
                anim = new KeybindAnimation(module);
                keybindAnimations.add(anim);
            }
            anim.show();
        }

        for (KeybindAnimation anim : keybindAnimations) {
            if (!currentModules.contains(anim.module)) {
                anim.hide();
            }
            anim.tick();
        }

        keybindAnimations.removeIf(KeybindAnimation::isFinished);

        for (Module m : currentModules) {
            String name = m.getName();
            String key = "[" + KeyboardStorage.getKey(m.getKey()) + "]";
            float combined = ClientApi.inter().getWidth(name, 7) + ClientApi.inter().getWidth(key, 7) + 30f;
            if (combined > targetWidth.get()) targetWidth.set(combined);
        }

        float th = 20f;
        for (KeybindAnimation anim : keybindAnimations) {
            th += 14f * anim.getHeightProgress();
        }
        targetHeight.set(th);

        keyBindsWidth.run(targetWidth.get());
        keyBindsHeight.run(targetHeight.get());
        keyBindsWidth.update();
        keyBindsHeight.update();

        float animatedWidth = (float) keyBindsWidth.getValue();
        float animatedHeight = (float) keyBindsHeight.getValue();

        draggableHandler.setWidth(animatedWidth);
        draggableHandler.setHeight(animatedHeight);

        width = animatedWidth;
        height = animatedHeight;

        float headerHeight = 20f;
        
        // Рендерим один общий фон для всей панели
        HelperElements.rectWatermarkStyle(event.getContext(), x, y, width, height, show);
        
        DraggableHeaderRenderer.render(
                event,
                x,
                y,
                width,
                show,
                headerHeight,
                "F",
                8f,
                "Keybinds",
                8f,
                -1,
                -1,
                () -> Interface.INSTANCE.getMainColor()
        );

        AtomicReference<Float> offset = new AtomicReference<>(0f);
        ScissorUtil.enable(x, y, width, height);

        for (KeybindAnimation anim : keybindAnimations) {
            float a = anim.getProgress();
            if (a <= 0f) continue;

            Module module = anim.module;
            int themeColor = Interface.INSTANCE.getMainColor();

            String moduleName = module.getName();
            String keyText = "[" + KeyboardStorage.getKey(module.getKey()) + "]";
            float keyTextW = ClientApi.inter().getWidth(keyText, 7);

            float itemHeight = 14f;
            float itemY = y + headerHeight + offset.get();
            float padding = 7f;

            // Фон элемента больше не нужен - общий фон уже отрисован

            float moduleX = x + padding - 0.5f - (1f - a) * 15f;
            float keyX = x + width - keyTextW - padding + (1f - a) * 15f;
            float baseY = itemY + (itemHeight - 7f) / 2f - 0.5f;

            HelperElements.renderGradientText(event.getContext().getMatrices().peek().getPositionMatrix(), moduleName, moduleX, baseY, 7, themeColor);

            int keyColor = ColorUtils.setAlpha(-1, (int) (255 * a));
            ClientApi.text()
                    .size(7)
                    .font(ClientApi.inter())
                    .text(keyText)
                    .color(keyColor)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), keyX, baseY);

            offset.set(offset.get() + itemHeight);
        }

        ScissorUtil.disable();
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }

    private KeybindAnimation findAnim(Module m) {
        for (KeybindAnimation a : keybindAnimations) if (a.isSame(m)) return a;
        return null;
    }

    static class KeybindAnimation {
        final Module module;
        final CompactAnimation anim = new CompactAnimation(Easing.EASE_OUT_QUAD, 250);

        KeybindAnimation(Module module) {
            this.module = module;
            anim.setValue(0.0);
        }

        void tick() {
            anim.update();
        }

        void show() {
            anim.run(1.0);
        }

        void hide() {
            anim.run(0.0);
        }

        float getProgress() {
            return (float) anim.getValue();
        }

        boolean isFinished() {
            return anim.getDestinationValue() == 0.0 && anim.isDone() && getProgress() <= 0.001f;
        }

        float getHeightProgress() {
            return anim.getDestinationValue() == 0.0 ? getProgress() : 1f;
        }

        boolean isSame(Module other) {
            return module.equals(other);
        }
    }
}
