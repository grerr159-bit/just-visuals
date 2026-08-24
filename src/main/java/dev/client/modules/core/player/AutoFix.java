package dev.client.modules.core.player;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.Input;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class AutoFix extends Module {

    public AutoFix() {
        super("AutoFix", ModuleCategory.Utils, "Автоматически чинит предметы");
    }

    Input repairText = new Input("Команда для починки", () -> true).set("/fix").register(this);
    Slider repair = new Slider("Прочность для починки", () -> true).set(1, 100, 1).defaultValue(75).register(this);

    private boolean hasRepaired = false;

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        PlayerInventory inv = mc.player.getInventory();
        boolean foundItemToRepair = false;

        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack stack = inv.main.get(i);
            if (stack.isEmpty() || !stack.isDamageable()) continue;

            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamage();
            int durabilityPercent = (int) ((1.0 - (double) currentDamage / maxDamage) * 100);

            if (durabilityPercent <= repair.getValue()) {
                foundItemToRepair = true;

                if (!hasRepaired) {
                    mc.player.getInventory().selectedSlot = i;
                    printChat(repairText.getValue());
                    hasRepaired = true;
                }

                break;
            }
        }

        if (!foundItemToRepair) {
            hasRepaired = false;
        }
    }
}
