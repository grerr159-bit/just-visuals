package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.ArmorHud;

public class ArmorHudModule extends Module {
    private final ArmorHud armorHud;

    public ArmorHudModule() {
        super("ArmorHud", ModuleCategory.HUD, "Отображает броню");
        this.armorHud = new ArmorHud();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        armorHud.onRender(event);
    }
}
