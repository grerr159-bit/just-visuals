package dev.client.api.injection;

import dev.client.Just;
import dev.client.modules.core.render.FullBright;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.PrintWriter;

@Mixin(GameOptions.class)
public class GameOptionsMixins implements Just.DelayHolder {
    @Unique private double Just$throwInventoryScreenDelay = 0.0D;
    @Unique private double Just$chestScreenDelay = 0.0D;

    @Override public double Just$getThrowInventoryScreenDelay() {return Just$throwInventoryScreenDelay;}
    @Override public void Just$setThrowInventoryScreenDelay(double v) {Just$throwInventoryScreenDelay = Math.max(0.0D, v);}
    @Override public double Just$getChestScreenDelay() {return Just$chestScreenDelay;}
    @Override public void Just$setChestScreenDelay(double v) {Just$chestScreenDelay = Math.max(0.0D, v);}

    // FullBright
    @Shadow @Final private SimpleOption<Double> gamma;
    @Unique private static boolean fb$applied = false;
    @Unique private static double fb$savedUserGamma = 0.5D;

    @Unique
    private static boolean fb$differs(double a, double b) {
        return Math.abs(a - b) > 1.0e-6;
    }

    @Inject(method = "getGamma", at = @At("RETURN"))
    private void Just$fullbrightTick(CallbackInfoReturnable<SimpleOption<Double>> cir) {
        boolean enabled = false;
        boolean gammaMode = false;
        double targetGamma = 1.0;

        try {
            var fb = FullBright.INSTANCE;
            enabled = fb.isEnabled();
            gammaMode = fb.mode.isSelected("Гамма");
            targetGamma = fb.gamma.getValue();
        } catch (Throwable ignored) { }

        double current = gamma.getValue();

        if (enabled && gammaMode) {
            if (!fb$applied) {
                fb$savedUserGamma = current;
                fb$applied = true;
            }
            double raw = targetGamma;
            double safe = Math.max(0.0, Math.min(raw, 1.0));
            if (fb$differs(current, safe)) {
                gamma.setValue(safe);
            }
        } else if (fb$applied) {
            if (fb$differs(current, fb$savedUserGamma)) {
                gamma.setValue(fb$savedUserGamma);
            }
            fb$applied = false;
        }
    }

    @Inject(method = "accept", at = @At("TAIL"))
    private void onAccept(GameOptions.Visitor visitor, CallbackInfo ci) {
        Just$throwInventoryScreenDelay = visitor.visitFloat("throwInventoryScreenDelay", (float) Just$throwInventoryScreenDelay);
        Just$chestScreenDelay = visitor.visitFloat("chestScreenDelay", (float) Just$chestScreenDelay);
    }

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;accept(Lnet/minecraft/client/option/GameOptions$Visitor;)V", shift = At.Shift.AFTER), locals = org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILSOFT)
    private void onWrite(CallbackInfo ci, PrintWriter printWriter) {
        printWriter.print("throwInventoryScreenDelay:");
        printWriter.println(String.format(java.util.Locale.ROOT, "%.3f", Just$throwInventoryScreenDelay));

        printWriter.print("chestScreenDelay:");
        printWriter.println(String.format(java.util.Locale.ROOT, "%.3f", Just$chestScreenDelay));
    }

    @Inject(method = "write", at = @At("HEAD"))
    private void Just$beforeWrite(CallbackInfo ci) {
        if (fb$applied) {
            if (fb$differs(gamma.getValue(), fb$savedUserGamma)) {
                gamma.setValue(fb$savedUserGamma);
            }
        }
    }
}
