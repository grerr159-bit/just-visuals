package dev.client.api.injection;

import dev.client.Just;
import dev.client.api.nullcry.cmdHelper.DispatchResult;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.game.PickupItemEvent;
import dev.client.cmd.core.Cmd_Initializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixins {
    @Shadow private ClientWorld world;

    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String text, CallbackInfo ci) {
        var dispatcher = Just.getInstance().getCommandDispatcher();
        Cmd_Initializer initializer = Just.getInstance().getCmdInitializer();
        String prefix = ".";
        if (initializer != null && initializer.getCommandDispatcher() != null) {
            prefix = initializer.getCommandDispatcher().getPrefix().get();
        }
        String normalized = text;
        if (prefix != null && !prefix.isEmpty() && text.startsWith("/" + prefix)) {
            normalized = text.substring(1);
        }
        if (dispatcher != null && dispatcher.dispatch(normalized) == DispatchResult.DISPATCHED) {
            ci.cancel();
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
    private void onItemPickupAnimationInvoke(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        Entity entity = this.world.getEntityById(packet.getEntityId());
        if (!(entity instanceof ItemEntity itemEntity)) return;

        LivingEntity livingEntity = (LivingEntity) this.world.getEntityById(packet.getCollectorEntityId());
        if (livingEntity == null) livingEntity = MinecraftClient.getInstance().player;

        ItemStack fullStack = itemEntity.getStack();
        if (fullStack.isEmpty()) return;

        ItemStack picked = fullStack.copy();
        int pickedAmount = Math.min(picked.getCount(), packet.getStackAmount());
        picked.setCount(pickedAmount);

        EventManager.call(new PickupItemEvent(picked, livingEntity, itemEntity));
    }
}