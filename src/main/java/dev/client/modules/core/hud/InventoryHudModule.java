package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.InventoryHud;

public class InventoryHudModule extends Module {
    private final InventoryHud inventoryHud;

    public InventoryHudModule() {
        super("InventoryHud", ModuleCategory.HUD, "Отображает инвентарь");
        this.inventoryHud = new InventoryHud();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        inventoryHud.onRender(event);
    }
}
