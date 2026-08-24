package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.Cooldowns;

public class CooldownsModule extends Module {
    private final Cooldowns cooldowns;

    public CooldownsModule() {
        super("Cooldowns", ModuleCategory.HUD, "Отображает кулдауны способностей");
        this.cooldowns = new Cooldowns();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        cooldowns.onRender(event);
    }
}
