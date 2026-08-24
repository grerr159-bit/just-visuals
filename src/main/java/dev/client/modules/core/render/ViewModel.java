package dev.client.modules.core.render;

import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ClickSetting;
import dev.client.api.nullcry.modules.settings.Slider;

public class ViewModel extends Module {
    public static ViewModel INSTANCE;

    public ViewModel() {
        super("ViewModel", ModuleCategory.Visuals, "Изменяет положение руки");
    }

    public final Slider right_x = new Slider("Правая рука X", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);
    public final Slider right_y = new Slider("Правая рука Y", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);
    public final Slider right_z = new Slider("Правая рука Z", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);

    public final Slider left_x = new Slider("Левая рука X", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);
    public final Slider left_y = new Slider("Левая рука Y", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);
    public final Slider left_z = new Slider("Левая рука Z", () -> true).set(-2.0f, 2.0f, 0.1F).defaultValue(0.0F).register(this);
    
    public final ClickSetting reset = new ClickSetting("Сбросить положение", () -> true)
            .onClick(() -> {
                right_x.resetToDefault();
                right_y.resetToDefault();
                right_z.resetToDefault();
                left_x.resetToDefault();
                left_y.resetToDefault();
                left_z.resetToDefault();
            }).register(this);
}
