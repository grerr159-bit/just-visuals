package dev.client.api.injection;

import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.other.CameraClipEvent;
import dev.client.modules.core.render.NoRender;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixins {

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    public void onUpdate(Args args) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Clip")) args.set(0, -3.5f);
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    public void onClipToSpace(float distance, CallbackInfoReturnable<Float> cir) {
        CameraClipEvent event = new CameraClipEvent();
        EventManager.call(event);

        if (event.isCancelled()) {
            cir.setReturnValue(distance);
        }

        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Clip"))
            cir.setReturnValue(3.5f);
    }
}