package dev.client.api.injection;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.events.core.world.AspectRatioEvent;
import dev.client.api.nullcry.helper.client.projection.ProjectionUtil;
import dev.client.api.nullcry.render.core.Draw3DUtil;
import dev.client.api.nullcry.rotation.RaytracingUtil;
import dev.client.api.nullcry.rotation.RotationController;
import dev.client.modules.core.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixins {
    @Shadow
    private float zoom;
    @Shadow
    private float zoomX;
    @Shadow
    private float zoomY;

    @Shadow
    public abstract float getFarPlaneDistance();

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void onRender3DCombined(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f mvMat) {
        {
            Camera camera = client.gameRenderer.getCamera();
            MatrixStack ms = new MatrixStack();
            RenderSystem.getModelViewStack().pushMatrix();
            try {
                RenderSystem.getModelViewStack().mul(ms.peek().getPositionMatrix());
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

                ProjectionUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
                ProjectionUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
                ProjectionUtil.lastWorldSpaceMatrix.set(ms.peek().getPositionMatrix());

                EventManager.call(new RenderEvent.Draw3D(ms, tickCounter));
            } finally {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }

        {
            MatrixStack ms = new MatrixStack();
            ms.multiplyPositionMatrix(mvMat);
            ms.translate(client.getEntityRenderDispatcher().camera.getPos().negate());
            Draw3DUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
            Draw3DUtil.setLastWorldSpaceMatrix(ms.peek());
            Draw3DUtil.onDraw3D(new RenderEvent.Draw3D(ms, tickCounter));
        }
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioEvent aspectRatioEvent = new AspectRatioEvent();
        EventManager.call(aspectRatioEvent);
        if (aspectRatioEvent.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            if (zoom != 1.0f) {
                matrix4f.translate(zoomX, -zoomY, 0.0f);
                matrix4f.scale(zoom, zoom, 1.0f);
            }
            matrix4f.perspective(fovDegrees * 0.01745329238474369F, aspectRatioEvent.getRatio(), 0.05f, getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }

    @Inject(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(" + "Lnet/minecraft/entity/Entity;" + "Lnet/minecraft/util/math/Vec3d;" + "Lnet/minecraft/util/math/Vec3d;" + "Lnet/minecraft/util/math/Box;" + "Ljava/util/function/Predicate;" + "D)" + "Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    public void onFindCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir) {
        // Removed NoEntityTrace cheat functionality
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hookRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        if (instance != client.player) return instance.raycast(maxDistance, tickDelta, includeFluids);
        return RaytracingUtil.raycast(maxDistance, RotationController.INSTANCE.getRotation(), includeFluids);
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookRotationVector(Entity instance, float tickDelta) {
        return RotationController.INSTANCE.getRotation().toVector();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    public void showFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Totem")) ci.cancel();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void tiltViewWhenHurt(CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("NoHurtCam")) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void bobView(CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Camera Shake")) ci.cancel();
    }

}
