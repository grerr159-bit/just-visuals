package dev.client.component.core.inventory;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.input.InputEvent;
import dev.client.api.nullcry.helper.client.ConnectionHelper;
import dev.client.api.nullcry.helper.entity.MovingUtil;
import dev.client.api.nullcry.helper.entity.world.InventoryHelper;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.rotation.Angle;
import dev.client.api.nullcry.rotation.AngleUtil;
import dev.client.api.nullcry.rotation.RotationConfig;
import dev.client.api.nullcry.rotation.RotationController;
import dev.other.task.TaskPriority;
import dev.other.task.scripts.Script;
import lombok.experimental.UtilityClass;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements ClientApi {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public static final Script script = new Script(), postScript = new Script();
    public boolean canMove = true;

    private static final Module ROTATION_PROVIDER = new Module("InventoryComponent", ModuleCategory.Utils, "Inventory Component") {};

    public void tick() {
        script.update();
    }

    public void postMotion() {
        postScript.update();
    }

    public void input(InputEvent e) {
        if (!canMove) e.inputNone();
    }

    public void addTask(Runnable task) {
        if (script.isFinished() && MovingUtil.isMoving()) {
            if (ConnectionHelper.isFT()) {
                script.cleanup().addTickStep(0, () -> {
                    PlayerInventoryComponent.disableMoveKeys();
                    PlayerInventoryComponent.rotateToCamera();
                }).addTickStep(1, () -> {
                    task.run();
                    enableMoveKeys();
                });
                return;
            } else if (ConnectionHelper.isRW()) {
                if (mc.player.isOnGround()) {
                    script.cleanup().addTickStep(0, PlayerInventoryComponent::disableMoveKeys).addTickStep(2, PlayerInventoryComponent::rotateToCamera).addTickStep(3, task::run)
                            .addTickStep(4, PlayerInventoryComponent::enableMoveKeys);
                    return;
                }
            } else if (ConnectionHelper.isSpookyTime() || ConnectionHelper.isFunSky()) {
                script.cleanup().addTickStep(0, ()-> {
                            PlayerInventoryComponent.disableMoveKeys();
                            PlayerInventoryComponent.rotateToCamera();
                        }).addTickStep(1, task::run)
                        .addTickStep(2, PlayerInventoryComponent::enableMoveKeys);
                return;
            }
        }
        script.addTickStep(0, PlayerInventoryComponent::rotateToCamera);
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            InventoryHelper.closeScreen(true);
        });
    }

    private void rotateToCamera() {
        if (mc.player == null) return;
        Angle cameraAngle = new Angle(mc.player.getYaw(), mc.player.getPitch());
        RotationController.INSTANCE.rotateTo(cameraAngle, new RotationConfig(false, false), TaskPriority.HIGH_IMPORTANCE_3, ROTATION_PROVIDER);
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        InventoryHelper.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }
}
