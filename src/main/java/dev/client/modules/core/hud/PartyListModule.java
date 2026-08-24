package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.PartyList;

public class PartyListModule extends Module {
    private final PartyList partyList;

    public PartyListModule() {
        super("PartyList", ModuleCategory.HUD, "Отображает список группы");
        this.partyList = new PartyList();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        partyList.onRender(event);
    }
}
