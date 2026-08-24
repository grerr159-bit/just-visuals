package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {
    public static FullBright INSTANCE;

    public FullBright() {
        super("Fullbright", ModuleCategory.Visuals, "Увеличивает яркость");
    }

    public ModeElement mode = new ModeElement("Режим", () -> true)
            .set("Гамма", "Эффект")
            .defaultValue("Гамма")
            .register(this);

    public Slider gamma = new Slider("Гамма", () -> mode.isSelected("Гамма")).set(1, 20, 1).defaultValue(12).register(this);

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (mode.isSelected("Эффект")) {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                return;
            }
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1337, 1, false, false));
        } else {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public void onDisabled() {
        if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        super.onDisabled();
    }
}
