package dev.client.modules.core.render;

import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.SelectElements;

public class NoRender extends Module {
    public static NoRender INSTANCE;

    public NoRender() {
        super("NoRender", ModuleCategory.Visuals, "Отключает отображение различных элементов");
        INSTANCE = this;
    }

    public final SelectElements mode = new SelectElements("Отключаемые элементы", () -> true).set("NoHurtCam","Camera Shake","Fire","Totem","Blocks","ScoreBoard","BossBar","Particles", "Bad effects", "Armor", "Fog", "Limbs","Clip").defaultValue("NoHurtCam","Fire").register(this);
}
