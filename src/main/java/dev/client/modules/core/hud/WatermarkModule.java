package dev.client.modules.core.hud;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.uiClient.draggables.core.Watermark;

public class WatermarkModule extends Module {
    private final Watermark watermark;

    public WatermarkModule() {
        super("Watermark", ModuleCategory.HUD, "Отображает водяной знак");
        this.watermark = new Watermark();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        watermark.onRender(event);
    }
}
