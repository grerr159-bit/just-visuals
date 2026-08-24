package dev.client.api.injection;

import dev.client.Just;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixins {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null) return;
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            Just.getInstance().keyPressed(-100 + button);
        }
    }
}
