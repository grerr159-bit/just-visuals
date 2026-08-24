package dev.client.api.injection;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.Color;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixins extends ClickableWidget {

    protected PressableWidgetMixins(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void Just$renderButtonWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof ButtonWidget)) {
            return;
        }

        if (!this.visible) {
            ci.cancel();
            return;
        }

        boolean hovered = this.isHovered();
        float a = MathHelper.clamp(this.alpha, 0.0f, 1.0f);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int bgAlpha = (int)(220 * a);
        int bgColor;
        if (hovered && this.active) {
            bgColor = (bgAlpha << 24);
        } else {
            bgColor = (bgAlpha << 24) | 0x2A2A2A;
        }
        ClientApi.rectangle()
                .size(new SizeState(this.getWidth(), this.getHeight()))
                .radius(new QuadRadiusState(4f))
                .color(new QuadColorState(new Color(bgColor, true)))
                .build()
                .render(matrix, this.getX(), this.getY());

        if (hovered && this.active) {
            int borderColor = ColorUtils.setAlpha(-1, (int)(220 * a));
            ClientApi.outline()
                    .size(new SizeState(this.getWidth(), this.getHeight()))
                    .radius(new QuadRadiusState(4f))
                    .thickness(1.0f)
                    .color(new QuadColorState(new Color(borderColor, true)))
                    .build()
                    .render(matrix, this.getX(), this.getY());
        }

        int alpha = MathHelper.ceil(a * 255.0F);
        int color = (alpha << 24) | 0xFFFFFF;

        Text message = this.getMessage();
        float fontSize = 8f;
        float textWidth = ClientApi.inter().getWidth(message, fontSize);
        float textHeight = ClientApi.inter().getHeight(message, fontSize);

        float textX = this.getX() + (this.getWidth() - textWidth) / 2f;
        float textY = this.getY() + (this.getHeight() - textHeight) / 2f;

        ClientApi.text()
                .font(ClientApi.inter())
                .text(message)
                .size(fontSize)
                .color(color)
                .build()
                .render(matrix, textX, textY);

        ci.cancel();
    }
}
