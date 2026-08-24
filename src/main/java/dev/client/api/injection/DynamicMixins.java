package dev.client.api.injection;

import dev.client.Just;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class DynamicMixins {
    @Shadow private float lastFrameDuration;
    @Shadow private float tickDelta;
    @Shadow private long prevTimeMillis;
    @Final @Shadow private float tickTime;

    @Inject(method = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
    private void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if (Just.tickTimer == 1)
            return;

        this.lastFrameDuration = ((timeMillis - this.prevTimeMillis) / this.tickTime) * Just.tickTimer;
        this.prevTimeMillis = timeMillis;
        this.tickDelta += this.lastFrameDuration;
        int i = (int) this.tickDelta;
        this.tickDelta -= i;
        cir.setReturnValue(i);
    }
}