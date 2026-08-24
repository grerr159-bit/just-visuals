package dev.client.api.injection;

import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.world.FogEvent;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.modules.core.render.NoRender;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackGroundRendererMixins {

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Bad effects")) info.cancel();
    }

    @Inject(method = "getFogColor", at = @At(value = "HEAD"), cancellable = true)
    private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        FogEvent event = new FogEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(ColorUtils.redf(color), ColorUtils.greenf(color), ColorUtils.bluef(color), ColorUtils.alphaf(color)));
        }
    }

    @Inject(method = "applyFog", at = @At(value = "HEAD"), cancellable = true)
    private static void modifyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Fog")) {
            cir.setReturnValue(Fog.DUMMY);
            return;
        }

        FogEvent event = new FogEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            int color1 = event.getColor();
            cir.setReturnValue(new Fog(2.0F, event.getDistance(), FogShape.CYLINDER, ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1), ColorUtils.alphaf(color1)));
        }
    }
}