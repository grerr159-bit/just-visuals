package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.Keybinds;

public class KeybindsModule extends Module {
    private final Keybinds keybinds;

    public KeybindsModule() {
        super("Keybinds", ModuleCategory.HUD, "Отображает активные клавиши");
        this.keybinds = new Keybinds();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        keybinds.onRender(event);
    }
}
