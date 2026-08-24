package dev.client.modules.core.player;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.helper.entity.MovingUtil;
import dev.client.api.nullcry.helper.other.TimerUtil;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", ModuleCategory.Utils, "Автоматически надевает лучшую броню из инвентаря");
    }

    Slider delay = new Slider("Задержка",  () -> true).set(1f, 100f, 1).defaultValue(50f).register(this);

    private final TimerUtil timerUtil = new TimerUtil();

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (MovingUtil.isMoving()) {
            return;
        }

        PlayerInventory inventoryPlayer = mc.player.getInventory();
        int[] bestIndexes = new int[4];
        int[] bestValues = new int[4];
        boolean foundBetterArmor = false;

        for (int i = 0; i < 4; ++i) {
            bestIndexes[i] = -1;
            ItemStack stack = inventoryPlayer.getArmorStack(i);

            if (!isItemValid(stack) || !(stack.getItem() instanceof ArmorItem armorItem)) {
                continue;
            }

            bestValues[i] = calculateArmorValue(armorItem, stack);
        }

        for (int i = 0; i < 36; ++i) {
            Item item;
            ItemStack stack = inventoryPlayer.getStack(i);

            if (!isItemValid(stack) || !((item = stack.getItem()) instanceof ArmorItem)) continue;

            ArmorItem armorItem = (ArmorItem) item;
            int armorTypeIndex = armorItem.getComponents().get(net.minecraft.component.DataComponentTypes.EQUIPPABLE).slot().getEntitySlotId();
            int value = calculateArmorValue(armorItem, stack);

            if (value > bestValues[armorTypeIndex]) {
                bestIndexes[armorTypeIndex] = i;
                bestValues[armorTypeIndex] = value;
                foundBetterArmor = true;
            }
        }

        if (!foundBetterArmor) {
            if (hasNoArmorInInventory(inventoryPlayer)) {
                printClient("Нет брони в инвентаре");
                toggle();
                return;
            }

            if (isFullyEquipped(inventoryPlayer)) {
                toggle();
                return;
            }
        }

        ArrayList<Integer> randomIndexes = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(randomIndexes);

        for (int index : randomIndexes) {
            int bestIndex = bestIndexes[index];

            if (bestIndex == -1 || (isItemValid(inventoryPlayer.getArmorStack(index)) && inventoryPlayer.getEmptySlot() == -1))
                continue;

            if (bestIndex < 9) {
                bestIndex += 36;
            }

            if (!this.timerUtil.isReached(this.delay.getValue().longValue())) break;

            ItemStack armorItemStack = inventoryPlayer.getArmorStack(index);

            if (isItemValid(armorItemStack)) {
                mc.interactionManager.clickSlot(0, 8 - index, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            mc.interactionManager.clickSlot(0, bestIndex, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.timerUtil.reset();
            break;
        }
    }

    private boolean isItemValid(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private boolean hasNoArmorInInventory(PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            if (inventory.getStack(i).getItem() instanceof ArmorItem) {
                return false;
            }
        }
        return true;
    }

    private boolean isFullyEquipped(PlayerInventory inventory) {
        for (int i = 0; i < 4; i++) {
            if (inventory.getArmorStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private int calculateArmorValue(final ArmorItem armor, final ItemStack stack) {
        var modifiers = stack.get(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS);

        double protection = 0.0;
        double toughness = 0.0;

        if (modifiers != null) {
            EquipmentSlot slot = armor.getComponents().get(net.minecraft.component.DataComponentTypes.EQUIPPABLE).slot();

            protection = modifiers.applyOperations(0.0, slot);

            toughness = modifiers.modifiers().stream()
                    .filter(entry -> entry.attribute().matches(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS))
                    .filter(entry -> entry.slot().matches(slot))
                    .mapToDouble(e -> e.modifier().value())
                    .sum();
        }

        return (int) (protection * 5 + toughness * 2);
    }
}