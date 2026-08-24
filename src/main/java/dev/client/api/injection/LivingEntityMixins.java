package dev.client.api.injection;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.client.Just;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.game.JumpEvent;
import dev.client.api.nullcry.events.core.game.UseItemEvent;
import dev.client.api.nullcry.events.core.other.NoPushEvent;
import dev.client.api.nullcry.events.core.player.PlayerDamageReceivedEvent;
import dev.client.api.nullcry.events.core.player.PlayerJumpEvent;
import dev.client.api.nullcry.helper.client.PlayerHelper;
import dev.client.api.nullcry.rotation.RotationController;
import dev.client.modules.core.movement.Sprint;
import dev.client.modules.core.render.SwordAnimation;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixins extends Entity {

    public LivingEntityMixins(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract float getYaw(float tickDelta);
    @Shadow public float bodyYaw;
    @Unique private final MinecraftClient client = MinecraftClient.getInstance();

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean onSetSprinting(boolean sprinting) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            if (Sprint.INSTANCE.isEnabled() && Sprint.INSTANCE.isKeepWater.getEnabled() && (mc.player.isTouchingWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.5, 0))).getBlock() instanceof FluidBlock)) {
                return true;
            }
        }
        return sprinting;
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void onIsPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
        NoPushEvent noPushEvent = new NoPushEvent(NoPushEvent.CollisionEnum.Players);
        EventManager.call(noPushEvent);

        if ((Object) this instanceof ClientPlayerEntity && noPushEvent.isCancelled()) {
            infoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            float defaultValue = player.getControllingPassenger() instanceof PlayerEntity ? player.getMovementSpeed() * 0.1F : 0.02F;
            cir.setReturnValue(defaultValue);
        }
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void onJumpPre(CallbackInfo ci) {
        EventManager.call(new PlayerJumpEvent(true));
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void onJumpPost(CallbackInfo ci) {
        EventManager.call(new PlayerJumpEvent(false));
        EventManager.call(new JumpEvent((LivingEntity) (Object) this));
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d original) {
        if ((Object) this != client.player) {
            return original;
        }
        float yaw = RotationController.INSTANCE.getMoveRotation().getYaw() * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if ((Object) this != client.player) {
            return original;
        }
        return RotationController.INSTANCE.getMoveRotation().getPitch();
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
        if ((Object) this != client.player) {
            return original;
        }
        return RotationController.INSTANCE.getMoveRotation().toVector();
    }

    @Inject(method = "handleFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;serverDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"))
    private void onHandleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ClientPlayerEntity) {
            EventManager.call(new PlayerDamageReceivedEvent(PlayerDamageReceivedEvent.DamageType.FALL));
        }
    }

    @Inject(method = "consumeItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;"))
    private void onConsumeItem(CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living && living instanceof PlayerEntity) {
            ItemStack using = living.getActiveItem();
            EventManager.call(new UseItemEvent(living, using.copy()));
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void injectCustomHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        SwordAnimation animation = SwordAnimation.INSTANCE;
        if (animation != null && animation.isEnabled() && !animation.mode.isSelected("None") && (Object) this instanceof ClientPlayerEntity) {
            int val = 25 - (animation.speed.getValue().intValue() * 2);
            if (val < 1) val = 1;
            cir.setReturnValue(val);
        }
    }

    @ModifyExpressionValue(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;wrapDegrees(F)F", ordinal = 1))
    private float wrapDegreesHook(float original) {
        if ((Object) this == client.player) {
            return MathHelper.wrapDegrees(RotationController.INSTANCE.getRotation().getYaw() - bodyYaw);
        }
        return original;
    }
}
