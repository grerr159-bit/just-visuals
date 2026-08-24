package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.player.PlayerAttackEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.TargetHud;

public class TargetHudModule extends Module {
    private final TargetHud targetHud;

    public TargetHudModule() {
        super("TargetHud", ModuleCategory.HUD, "Отображает информацию о цели");
        this.targetHud = new TargetHud();
    }

    @Subscribe
    public void onPlayerAttack(PlayerAttackEvent event) {
        targetHud.onPlayerAttack(event);
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        targetHud.onRender(event);
    }
}
