package dev.client.api.injection.accessor;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatHud.class)
public interface IChatHudAccessor {
    @Invoker("addMessage") void invokeAddMessage(ChatHudLine message);
    @Invoker("addVisibleMessage") void invokeAddVisibleMessage(ChatHudLine message);
}