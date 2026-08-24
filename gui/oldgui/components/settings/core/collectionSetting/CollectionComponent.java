package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.collectionSetting;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.modules.settings.Collection;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CollectionComponent extends SettingComponent {
    List<SettingComponent> childSettingsComponents = new ArrayList<>();

    public CollectionComponent(Collection collection) {
        super(collection);
        childSettingsComponents.addAll(CollectionHelper.childSettingComponents(collection));
    }

    @Override
    public void init() {
        childSettingsComponents.forEach(SettingComponent::init);
        size((240f / 2) - 10, ClientApi.inter().getHeight(getSetting().getName(), 7.5f) + 5 + CollectionHelper.collectionHeight(childSettingsComponents) + 2.5f);
    }

    @Override
    public CollectionComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        float visibility = getGlobalAlpha();
        ClientApi.text()
                .font(ClientApi.inter())
                .color(ColorUtils.setAlpha(
                        -1,
                        Math.round(ColorUtils.getAlpha(-1) * visibility)
                ))
                .text(getSetting().getName())
                .size(7.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), getX() + getWidth() / 2 - ClientApi.inter().getWidth(getSetting().getName(), 7.5f) / 2, getY());

        AtomicReference<Float> offset = new AtomicReference<>(0f);
        childSettingsComponents.forEach(e -> {
            e.setGlobalAlpha(visibility);
            e.position(getX(), getY() + offset.get() + ClientApi.inter().getHeight(getSetting().getName(), 7.5f) + 5).render(context, mouseX, mouseY, delta);
            offset.set(offset.get() + e.getHeight() + 4f);
        });

        return null;
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return childSettingsComponents.stream().anyMatch(e -> e.mouseReleased(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return childSettingsComponents.stream().anyMatch(e -> e.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return childSettingsComponents.stream().anyMatch(e -> e.keyPressed(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return childSettingsComponents.stream().anyMatch(e -> e.keyReleased(keyCode, scanCode, modifiers));
    }
}
