package dev.client.api.injection;

import dev.client.api.nullcry.helper.other.TimerUtil;
import dev.client.modules.core.misc.AHHelper;
import dev.client.modules.core.player.ItemScroller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixins<T extends ScreenHandler> {
    @Shadow @Final protected T handler;
    @Shadow protected abstract boolean isPointOverSlot(Slot slot, double pointX, double pointY);
    @Shadow protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
    @Shadow @Nullable protected Slot focusedSlot;
    @Unique private final TimerUtil timerUtil = new TimerUtil();

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Кнопки удалены
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (int i1 = 0; i1 < this.handler.slots.size(); ++i1) {
            Slot slot = this.handler.slots.get(i1);
            if (this.isPointOverSlot(slot, mouseX, mouseY) && slot.isEnabled()) {
                if (ItemScroller.INSTANCE.isEnabled() && GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1 && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 340) == 1 && MinecraftClient.getInstance().currentScreen != null && this.timerUtil.isReached(ItemScroller.INSTANCE.delay.getValue().longValue()) && slot.hasStack()) {
                    this.onMouseClick(slot, slot.id, 1, SlotActionType.QUICK_MOVE);
                    this.timerUtil.reset();
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("TAIL"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> ci) {
        if (!MinecraftClient.getInstance().options.dropKey.matchesKey(keyCode, scanCode)) return;
        if (!ItemScroller.INSTANCE.isEnabled() || !Screen.hasShiftDown() || !Screen.hasControlDown()) return;

        if (this.focusedSlot == null || !this.focusedSlot.hasStack()) return;

        ItemStack clicked = this.focusedSlot.getStack();
        if (clicked.isEmpty()) return;

        for (Slot s : this.handler.slots) {
            if (s.hasStack() && s.getStack().isOf(clicked.getItem())) {
                MinecraftClient.getInstance().interactionManager.clickSlot(this.handler.syncId, s.id, 1, SlotActionType.THROW, MinecraftClient.getInstance().player);
            }
        }

        ci.setReturnValue(true);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        AHHelper.INSTANCE.analyze(this.handler);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        AHHelper helper = AHHelper.INSTANCE;
        if (!helper.isEnabled() || !helper.isAuctionScreenActive()) {
            return;
        }

        if (slot == helper.getCheapestSlot()) {
            helper.renderCheapest(context, slot);
        } else if (slot == helper.getBestUnitSlot()) {
            helper.renderBest(context, slot);
        } else if (helper.isHighlightThree() && helper.getAdditionalUnitSlot() != null && slot == helper.getAdditionalUnitSlot()) {
            helper.renderAdditional(context, slot);
        }
    }
}

