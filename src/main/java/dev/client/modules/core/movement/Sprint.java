package dev.client.modules.core.movement;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.events.core.player.KeepSprintEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Sprint extends Module {
    public static Sprint INSTANCE;

    public Sprint() {
        super("Sprint", ModuleCategory.Utils, "Автоматический спринт");
    }

    public CheckBox keepSprint = new CheckBox("Сохранять при атаке", () -> true).defaultValue(true).register(this);
    public CheckBox isWater = new CheckBox("Не работать в воде", () -> true).defaultValue(false).register(this);
    public CheckBox isKeepWater = new CheckBox("Не отжимать спринт в воде", () -> true).defaultValue(false).register(this);
    public CheckBox isFood = new CheckBox("Не работать при еде", () -> true).defaultValue(false).register(this);
    public CheckBox isBlindness = new CheckBox("Не работать при слепоте", () -> true).defaultValue(false).register(this);

    public int tickStop = 0;

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        boolean forwardPressed = mc.options.forwardKey.isPressed();
        boolean inWater = mc.player.isTouchingWater();
        boolean eatingFood = mc.player.isUsingItem() && mc.player.getActiveItem().contains(DataComponentTypes.FOOD);
        boolean blinded = mc.player.hasStatusEffect(StatusEffects.BLINDNESS);

        boolean allowBySettings = (!isWater.getEnabled() || !inWater) && (!isFood.getEnabled() || !eatingFood) && (!isBlindness.getEnabled() || !blinded);
        boolean wantStartSprint = forwardPressed && allowBySettings && !mc.player.isSprinting();

        if (wantStartSprint
                && mc.player.getHungerManager().getFoodLevel() > 6
                && !mc.player.horizontalCollision
                && mc.player.input.movementForward > 0
                && !mc.player.isSneaking()
                && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }

    @Subscribe
    public void onKeepSprint(KeepSprintEvent event) {
        if (mc.player == null || !keepSprint.getEnabled()) return;
        if (!isPlayerMoving()) return;
        mc.player.setSprinting(true);
    }

    private boolean isPlayerMoving() {
        if (mc.player == null || mc.player.input == null) {
            return false;
        }

        if (mc.player.input.movementForward == 0.0f && mc.player.input.movementSideways == 0.0f) {
            return false;
        }

        Vec3d velocity = mc.player.getVelocity();
        return velocity.horizontalLengthSquared() > 1.0E-4;
    }
}
