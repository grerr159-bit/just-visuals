package dev.client.api.injection;

import dev.client.api.injection.accessor.IClickableWidgetAccessor;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.cmdHelper.managers.dragHandler.DraggableManager;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixins extends Screen {

    protected ChatScreenMixins() {
        super(Text.empty());
    }

    @Shadow
    protected TextFieldWidget chatField;
    @Unique
    private static boolean streamerMode = false;

    @Unique
    private final int PAD_L = 0, SPACE = 2;
    @Unique
    private final int BOX_PAD_X = 1;
    @Unique
    private final int BOX_EXTRA_W = 4;

    @Unique
    private int streamerBoxX, resetBoxX, buttonBoxY, buttonBoxH, streamerBoxW, resetBoxW;
    @Unique
    private int streamerGlyphX, resetGlyphX, glyphBaselineY;

    @Unique
    private final String[] STREAMER_HIDE = {
            "reg", "register", "l", "login", "call", "tpa", "warp", "cp"
    };

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        int reserve = reservedRight();
        int outer = widgetOuterWidth(chatField);
        setFieldWidth(chatField, Math.max(40, outer - reserve - 1));
        computeGlyphLayout();
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void onResize(MinecraftClient client, int width, int height, CallbackInfo ci) {
        int reserve = reservedRight();
        int outer = widgetOuterWidth(chatField);
        setFieldWidth(chatField, Math.max(40, outer - reserve - 1));
        computeGlyphLayout();
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void shrinkChatRect(Args args) {
        if (this.chatField != null) {
            computeGlyphLayout();
        }

        int originalRight = args.<Integer>get(2);
        int fallbackRight = originalRight - reservedRight() - 1;

        int targetRight = (this.chatField != null ? Math.max(0, this.streamerBoxX - 1) : fallbackRight);
        int newRight = Math.min(originalRight, targetRight);
        args.set(2, newRight);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;render(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.BEFORE))
    private void drawGlyphsWithBackground(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        computeGlyphLayout();
        var tr = MinecraftClient.getInstance().textRenderer;

        int bg = MinecraftClient.getInstance().options.getTextBackgroundColor(Integer.MIN_VALUE);
        ctx.fill(streamerBoxX, buttonBoxY, streamerBoxX + streamerBoxW, buttonBoxY + buttonBoxH, bg);
        ctx.fill(resetBoxX, buttonBoxY, resetBoxX + resetBoxW, buttonBoxY + buttonBoxH, bg);

        int colorR = streamerMode ? -1 : ColorUtils.rgb(180, 180, 180);
        int colorH = ColorUtils.rgb(180, 180, 180);
        ctx.drawText(tr, "R", streamerGlyphX, glyphBaselineY, colorR, false);
        ctx.drawText(tr, "H", resetGlyphX, glyphBaselineY, colorH, false);

        if (isHoveredStreamer(mouseX, mouseY)) {
            ctx.drawTooltip(tr, Text.literal("Режим стримера"), mouseX, mouseY);
        } else if (isHoveredReset(mouseX, mouseY)) {
            ctx.drawTooltip(tr, Text.literal("Сбросить позиции драггейбла"), mouseX, mouseY);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderDragsAndTopMask(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        final Window window = this.client.getWindow();
        final long handle = window.getHandle();
        boolean anyHovered = false;

        DraggableManager.updatePanelCursor(mouseX, mouseY);

        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
        List<DraggableSettingsPanel> openPanels = new ArrayList<>();

        for (var dragging : DraggableManager.draggable.values()) {
            boolean active = dragging.isActive();
            if (active && MouseClick.isClick(mouseX, mouseY, dragging.getX(), dragging.getY(), dragging.getWidth(), dragging.getHeight())) {
                anyHovered = true;
            }
            dragging.drawElement(context, matrix4f, mouseX, mouseY, window);
            var panel = dragging.getSettingsPanel();
            if (panel != null && panel.isVisible() && active) {
                openPanels.add(panel);
            }
        }

        if (anyHovered) {
            GLFW.glfwSetCursor(handle, CursorsUtil.HAND);
        } else {
            GLFW.glfwSetCursor(handle, CursorsUtil.ARROW);
        }

        openPanels.forEach(panel -> panel.render(context, mouseX, mouseY, delta));

        if (streamerMode && shouldHide(chatField.getText())) {
            var tr = MinecraftClient.getInstance().textRenderer;

            boolean bgOn = chatField.drawsBackground();
            float innerLeft = bgOn ? chatField.getX() + 3 : chatField.getX() - 1;
            float innerWidth = chatField.getInnerWidth();

            String visible = tr.trimToWidth(chatField.getText(), (int) innerWidth);
            float textW = tr.getWidth(visible);
            float maskW = Math.min(textW + 1, innerWidth);

            if (maskW > 0) {
                ClientApi.rectangle()
                        .size(new SizeState(maskW, chatField.getHeight() + 0.5f))
                        .color(new QuadColorState(0xFFFFFFFF))
                        .radius(new QuadRadiusState(0f))
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), innerLeft, chatField.getY() - 2);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        computeGlyphLayout();

        for (var dragging : DraggableManager.draggable.values()) {
            var panel = dragging.getSettingsPanel();
            if (panel != null && panel.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (button == 0) {
            if (isHoveredStreamer((int) mouseX, (int) mouseY)) {
                streamerMode = !streamerMode;
                cir.setReturnValue(true);
                return;
            }
            if (isHoveredReset((int) mouseX, (int) mouseY)) {
                DraggableManager.draggable.values().forEach(DraggableHandler::resetPosition);
                DraggableManager.closeAllPanels();
                cir.setReturnValue(true);
                return;
            }
        }

        for (var dragging : DraggableManager.draggable.values()) {
            if (dragging.onMouseClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        DraggableManager.draggable.values().forEach(d -> {
            d.onMouseRelease(button);
            if (d.getSettingsPanel() != null) {
                d.getSettingsPanel().mouseReleased(mouseX, mouseY, button);
            }
        });
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        for (var dragging : DraggableManager.draggable.values()) {
            var panel = dragging.getSettingsPanel();
            if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        DraggableManager.closeAllPanels();
    }

    @Unique
    private void computeGlyphLayout() {
        if (this.chatField == null) return;
        var tr = MinecraftClient.getInstance().textRenderer;

        int wR = tr.getWidth("R");
        int wH = tr.getWidth("H");
        this.streamerBoxW = wR + BOX_PAD_X * 2 + BOX_EXTRA_W;
        this.resetBoxW = wH + BOX_PAD_X * 2 + BOX_EXTRA_W;

        this.buttonBoxH = this.chatField.getHeight();
        this.buttonBoxY = this.chatField.getY() - 2;

        int rightEdge = this.width - 2;
        int totalW = streamerBoxW + SPACE + resetBoxW;

        this.streamerBoxX = rightEdge - totalW;
        this.resetBoxX = this.streamerBoxX + streamerBoxW + SPACE;

        this.streamerGlyphX = (int) (this.streamerBoxX + (streamerBoxW - wR) / 2f + 0.5f);
        this.resetGlyphX = (int) (this.resetBoxX + (resetBoxW - wH) / 2f + 0.5f);
        this.glyphBaselineY = (int) (this.buttonBoxY + (this.buttonBoxH - tr.fontHeight) / 2f + 1.25f);
    }

    @Unique
    private boolean shouldHide(String raw) {
        if (raw == null) return false;
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return false;
        if (t.startsWith("/")) t = t.substring(1);
        for (String k : STREAMER_HIDE) {
            if (t.equals(k) || t.startsWith(k + " ")) return true;
        }
        return false;
    }

    @Unique
    private int reservedRight() {
        var tr = MinecraftClient.getInstance().textRenderer;
        int wR = tr.getWidth("R");
        int wH = tr.getWidth("H");
        int boxRW = wR + BOX_PAD_X * 2 + BOX_EXTRA_W;
        int boxHW = wH + BOX_PAD_X * 2 + BOX_EXTRA_W;
        return PAD_L + boxRW + SPACE + boxHW + 1;
    }

    @Unique
    private int widgetOuterWidth(TextFieldWidget field) {
        int inner = field.getInnerWidth();
        return field.drawsBackground() ? inner + 8 : inner;
    }

    @Unique
    private void setFieldWidth(TextFieldWidget field, int width) {
        try {
            field.setWidth(width);
        } catch (Throwable ignore) {
            ((IClickableWidgetAccessor) field).setWidth(width);
        }
    }

    @Unique
    private boolean isHoveredStreamer(int mouseX, int mouseY) {
        return MouseClick.isClick(mouseX, mouseY, streamerBoxX, buttonBoxY, streamerBoxW, buttonBoxH);
    }

    @Unique
    private boolean isHoveredReset(int mouseX, int mouseY) {
        return MouseClick.isClick(mouseX, mouseY, resetBoxX, buttonBoxY, resetBoxW, buttonBoxH);
    }
}
