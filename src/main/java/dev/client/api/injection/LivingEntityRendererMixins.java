package dev.client.api.injection;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.client.api.nullcry.rotation.RotationController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixins<T extends LivingEntity, S extends LivingEntityRenderState> {

    @ModifyConstant(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", constant = @Constant(intValue = 654311423))
    private int onModifyInvisibleAlpha(int original, S renderState) {
        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float lerpAngleDegreesHook(float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
        if (entity.equals(MinecraftClient.getInstance().player)) {
            RotationController controller = RotationController.INSTANCE;
            return MathHelper.lerpAngleDegrees(delta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
        }
        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float getLerpedPitchHook(float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
        if (entity.equals(MinecraftClient.getInstance().player)) {
            RotationController controller = RotationController.INSTANCE;
            return MathHelper.lerp(delta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
        }
        return original;
    }
}