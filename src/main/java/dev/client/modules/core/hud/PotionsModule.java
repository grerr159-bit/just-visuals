package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.Potions;

public class PotionsModule extends Module {
    private final Potions potions;

    public PotionsModule() {
        super("Potions", ModuleCategory.HUD, "Отображает активные эффекты зелий");
        this.potions = new Potions();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        potions.onRender(event);
    }
}
