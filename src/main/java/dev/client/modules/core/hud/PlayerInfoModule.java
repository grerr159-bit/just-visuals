package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.PlayerInfo;

public class PlayerInfoModule extends Module {
    private final PlayerInfo playerInfo;

    public PlayerInfoModule() {
        super("PlayerInfo", ModuleCategory.HUD, "Отображает информацию об игроке");
        this.playerInfo = new PlayerInfo();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        playerInfo.onRender(event);
    }
}
