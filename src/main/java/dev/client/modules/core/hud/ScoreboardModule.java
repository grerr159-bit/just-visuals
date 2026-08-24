package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.Scoreboard;
import dev.client.modules.core.render.NoRender;

public class ScoreboardModule extends Module {
    private final Scoreboard scoreboard;

    public ScoreboardModule() {
        super("Scoreboard", ModuleCategory.HUD, "Отображает таблицу счета");
        this.scoreboard = new Scoreboard();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        
        boolean hideScoreboard = NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("ScoreBoard");
        if (hideScoreboard) return;
        
        scoreboard.onRender(event);
    }
}
