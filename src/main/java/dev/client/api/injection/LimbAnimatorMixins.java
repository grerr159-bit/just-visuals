package dev.client.api.injection;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.client.modules.core.render.NoRender;
import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LimbAnimator.class)
public abstract class LimbAnimatorMixins {

    @ModifyReturnValue(method = "getPos()F", at = @At("RETURN"))
    public float getPos(float original) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Limbs")) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getPos(F)F", at = @At("RETURN"))
    public float getPos2(float original) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Limbs")) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getSpeed()F", at = @At("RETURN"))
    public float getSpeed(float original) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Limbs")) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getSpeed(F)F", at = @At("RETURN"))
    public float getSpeed2(float original) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.mode.isSelected("Limbs")) return 0;
        else return original;
    }
}