package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;

import java.util.ArrayList;
import java.util.List;

public class MaceHelper extends Module implements SettingProvider {
    public static MaceHelper INSTANCE;

    private final List<Setting> settings = new ArrayList<>();

    public final CheckBox glowEnabled = new CheckBox("Подсветка булавы", () -> true)
            .defaultValue(true)
            .register(this);

    public final Slider glowR = new Slider("R", () -> glowEnabled.getEnabled()).set(0, 255, 1).defaultValue(255).register(this);
    public final Slider glowG = new Slider("G", () -> glowEnabled.getEnabled()).set(0, 255, 1).defaultValue(50).register(this);
    public final Slider glowB = new Slider("B", () -> glowEnabled.getEnabled()).set(0, 255, 1).defaultValue(50).register(this);
    public final Slider glowAlpha = new Slider("Alpha", () -> glowEnabled.getEnabled()).set(0, 255, 1).defaultValue(160).register(this);

    public MaceHelper() {
        super("MaceHelper", ModuleCategory.Utils, "Подсветка булавы в руке");
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }
}
