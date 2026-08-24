package dev.client.api.injection;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.input.MovementEvent;
import dev.client.api.nullcry.events.core.input.PostMovementEvent;
import dev.client.api.nullcry.events.core.player.PlayerVelocityStrafeEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixins {
    @Shadow public abstract void setVelocity(Vec3d velocity);
    @Shadow public abstract float getYaw();
    @Shadow public abstract Vec3d getVelocity();
    @Shadow public abstract float getYaw(float tickDelta);
    @Shadow public abstract EntityType<?> getType();
    @Unique private double preX;
    @Unique private double preZ;

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean onFixFallDistance(boolean original) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            return false;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            EventManager.call(event);
            return event.getVelocity();
        }
        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }

    @ModifyVariable(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPosition(DDD)V", shift = At.Shift.BEFORE), index = 2)
    private Vec3d onMoveVariable(Vec3d movement) {
        Entity self = (Entity) (Object) this;

        MovementEvent moveEvent = new MovementEvent(
                self.getPos(),
                self.getPos().add(movement),
                movement,
                self.isOnGround(),
                self.horizontalCollision,
                self.verticalCollision,
                self.getBoundingBox()
        );

        EventManager.call(moveEvent);

        if (moveEvent.isCancelled()) {
            return Vec3d.ZERO;
        }

        return moveEvent.getMotion();
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMoveHead(MovementType type, Vec3d movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        this.preX = self.getX();
        this.preZ = self.getZ();
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void onMoveTail(MovementType type, Vec3d movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (self instanceof ClientPlayerEntity) {
            double deltaX = self.getX() - this.preX;
            double deltaZ = self.getZ() - this.preZ;

            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            PostMovementEvent event = new PostMovementEvent(distance);
            EventManager.call(event);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        // Removed SeeInvisible cheat functionality
    }
}
