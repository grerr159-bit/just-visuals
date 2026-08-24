package dev.client.modules.core.misc;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.input.MotionEvent;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.rotation.Angle;
import dev.client.api.nullcry.rotation.RotationConfig;
import dev.client.api.nullcry.rotation.RotationController;
import dev.other.task.TaskPriority;
import net.minecraft.block.AirBlock;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoClanUpgrade extends Module {

    public AutoClanUpgrade() {
        super("AutoClanUpgrade", ModuleCategory.Utils, "Автоматическое использование улучшения клана");
    }

    private static final float DESIRED_PITCH = 89.0f;

    private int previousSlot = -1;

    @Subscribe
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;
        float yaw = mc.player.getYaw();
        float pitch = DESIRED_PITCH;

        RotationController.INSTANCE.rotateTo(new Angle(yaw, pitch), new RotationConfig(false, false), TaskPriority.STANDARD, this);
        event.setYaw(yaw);
        event.setPitch(pitch);
    }

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!hasRequiredItems()) {
            printClient("Не найдено редстоуна или факелов для улучшения клана");
            if (previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
            }
            toggle();
            return;
        }
        if (!isHoldingRequiredItem()) {
            switchToRequiredItem();
        }

        BlockPos playerPos = mc.player.getBlockPos();
        boolean isAirUnder = mc.world.getBlockState(playerPos).getBlock() instanceof AirBlock;

        if (isAirUnder) {
            Vec3d hitPos = mc.player.getPos().subtract(0, 1, 0);
            BlockHitResult bhr = new BlockHitResult(hitPos, Direction.UP, playerPos.down(), false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.interactionManager.updateBlockBreakingProgress(playerPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean hasRequiredItems() {
        for (int i = 0; i < 9; i++) {
            var it = mc.player.getInventory().getStack(i).getItem();
            if (it == Items.REDSTONE || it == Items.TORCH) return true;
        }
        return false;
    }

    private boolean isHoldingRequiredItem() {
        var it = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem();
        return it == Items.REDSTONE || it == Items.TORCH;
    }

    private void switchToRequiredItem() {
        for (int i = 0; i < 9; i++) {
            var it = mc.player.getInventory().getStack(i).getItem();
            if (it == Items.REDSTONE || it == Items.TORCH) {
                if (previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }
        previousSlot = -1;
    }
}
