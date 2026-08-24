package dev.client.api.injection;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.game.InventoryCloseEvent;
import dev.client.api.nullcry.events.core.game.NoSlowEvent;
import dev.client.api.nullcry.events.core.input.MotionEvent;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.events.core.other.NoPushEvent;
import dev.client.api.nullcry.events.core.other.UsingItemEvent;
import dev.client.api.nullcry.events.types.EventType;
import dev.client.component.core.inventory.PlayerInventoryComponent;
import dev.client.api.nullcry.rotation.RotationController;
import dev.client.modules.core.misc.LockSlot;
import dev.client.modules.core.movement.Sprint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixins extends AbstractClientPlayerEntity {

    public ClientPlayerEntityMixins(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow public abstract float getPitch(float tickDelta);
    @Shadow public abstract float getYaw(float tickDelta);
    @Shadow @Final protected MinecraftClient client;
    @Shadow public abstract void sendAbilitiesUpdate();
    @Shadow public Input input;

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        LockSlot lock = LockSlot.INSTANCE;
        if (lock == null || !lock.isEnabled()) return;

        int slot = mc.player.getInventory().selectedSlot;
        boolean ctrl = Screen.hasControlDown();
        boolean protectedNow = lock.isSlotProtected(slot);
        boolean ctrlBypass = ctrl && lock.pressedCtrl.getEnabled();

        if (protectedNow && !ctrlBypass) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean tickMovementInvoke(ClientPlayerEntity player) {
        NoSlowEvent a = new NoSlowEvent();
        EventManager.call(a);
        if (a.isCancelled()) return false;
        return player.isUsingItem();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        UpdateEvent tickEvent = new UpdateEvent();
        EventManager.call(tickEvent);
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void preMotion(CallbackInfo ci) {
        MotionEvent event = new MotionEvent(getX(), getY(), getZ(), getYaw(1), getPitch(1), isOnGround());
        EventManager.call(event);
        if (event.isCancelled()) ci.cancel();
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(float original) {
        return RotationController.INSTANCE.getRotation().getYaw();
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(float original) {
        return RotationController.INSTANCE.getRotation().getPitch();
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean usingItemHook(boolean original) {
        if (original) {
            UsingItemEvent event = new UsingItemEvent(EventType.ON);
            EventManager.call(event);
            if (event.isCancelled()) return false;
            Sprint.INSTANCE.tickStop = 1;
        }
        return original;
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void onPushOutOfBlocks(double x, double z, CallbackInfo callbackInfo) {
        NoPushEvent noPushEvent = new NoPushEvent(NoPushEvent.CollisionEnum.Blocks);
        EventManager.call(noPushEvent);

        if (noPushEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
    public void onCloseHandlerScreen(CallbackInfo ci) {
        InventoryCloseEvent inventoryCloseEvent = new InventoryCloseEvent(MinecraftClient.getInstance().currentScreen, this.currentScreenHandler.syncId);
        EventManager.call(inventoryCloseEvent);
        if (inventoryCloseEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void postMovementPackets(CallbackInfo ci) {
        PlayerInventoryComponent.postMotion();
    }
}
