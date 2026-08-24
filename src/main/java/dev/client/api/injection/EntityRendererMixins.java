package dev.client.api.injection;

import dev.client.Just;
import dev.client.api.nullcry.events.EventManager;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.uiClient.clickGui.newgui.FriendsStorage;
import dev.client.modules.core.misc.NameProtect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixins<T extends Entity, S extends EntityRenderState> {
    @Shadow public abstract TextRenderer getTextRenderer();
    @Shadow @Final protected EntityRenderDispatcher dispatcher;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelIfPresentHead(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEvent.RenderLabelsEvent<T, S> renderLabelsEvent = new RenderEvent.RenderLabelsEvent<>(state);
        EventManager.call(renderLabelsEvent);

        if (renderLabelsEvent.isCancelled()) ci.cancel();
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelIfPresentHead2(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        Vec3d vec3d = state.nameLabelPos;
        Text nameToRender = text;
        boolean isReplaced = false;
        boolean bl = !state.sneaking;

        if (Just.getInstance().getFriendManager() != null) {
            for (String friend : Just.getInstance().getFriendManager().getFriends()) {
                if (NameProtect.INSTANCE.isEnabled() && NameProtect.INSTANCE.friend.getEnabled() && text.getString().contains(friend)) {
                    Text newComponent = Text.literal("");

                    if (!text.getSiblings().isEmpty()) {
                        for (Text iTextComponent : text.getSiblings()) {
                            if (iTextComponent.getString().contains(friend)) {
                                Text replacedText = Text.literal(iTextComponent.getString().replace(friend, NameProtect.INSTANCE.nameClient))
                                        .setStyle(iTextComponent.getStyle());
                                newComponent.getSiblings().add(replacedText);
                            } else {
                                newComponent.getSiblings().add(iTextComponent);
                            }
                        }
                    } else {
                        Text replacedText = Text.literal(text.getString().replace(friend, NameProtect.INSTANCE.nameClient))
                                .setStyle(text.getStyle());
                        newComponent = newComponent.copy().append(replacedText);
                    }

                    nameToRender = newComponent;
                    isReplaced = true;
                    break;
                }
            }
        }

        if (!isReplaced && NameProtect.INSTANCE.isEnabled() && text.getString().contains(MinecraftClient.getInstance().player.getGameProfile().getName())) {
            Text newComponent = Text.literal("");

            if (!text.getSiblings().isEmpty()) {
                for (Text iTextComponent : text.getSiblings()) {
                    if (iTextComponent.getString().contains(MinecraftClient.getInstance().player.getGameProfile().getName())) {
                        Text replacedText = Text.literal(iTextComponent.getString().replace(MinecraftClient.getInstance().player.getGameProfile().getName(), NameProtect.INSTANCE.nameClient))
                                .setStyle(iTextComponent.getStyle());
                        newComponent.getSiblings().add(replacedText);
                    } else {
                        newComponent.getSiblings().add(iTextComponent);
                    }
                }
            } else {
                Text replacedText = Text.literal(text.getString().replace(MinecraftClient.getInstance().player.getGameProfile().getName(), NameProtect.INSTANCE.nameClient))
                        .setStyle(text.getStyle());
                newComponent = newComponent.copy().append(replacedText);
            }

            nameToRender = newComponent;
            isReplaced = true;
        }

        matrices.push();
        matrices.translate(vec3d.x, vec3d.y + (double)0.5F, vec3d.z);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(0.025F, -0.025F, 0.025F);
        TextRenderer textRenderer = this.getTextRenderer();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float f = (float) (-textRenderer.getWidth(nameToRender)) / 2.0F;
        int j = (int) (MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;

        boolean isFriend = false;
        String rawName = text.getString();
        for (String friend : FriendsStorage.getFriends()) {
            if (rawName.contains(friend)) {
                isFriend = true;
                break;
            }
        }
        if (isFriend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            float nameWidth = textRenderer.getWidth(nameToRender);
            float iconSpace = 10f;
            float bgPadX = 3f;
            float bgPadY = 2f;
            float bgX = f - bgPadX - iconSpace;
            float bgY = -1f;
            float bgW = nameWidth + bgPadX * 2f + iconSpace - 3f;
            float bgH = 10f;
            int greenBg = 0xCC22CC44;
            var bgBuilder = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bgBuilder.vertex(matrix4f, bgX, bgY + bgH, 0f).color(greenBg);
            bgBuilder.vertex(matrix4f, bgX + bgW, bgY + bgH, 0f).color(greenBg);
            bgBuilder.vertex(matrix4f, bgX + bgW, bgY, 0f).color(greenBg);
            bgBuilder.vertex(matrix4f, bgX, bgY, 0f).color(greenBg);
            BufferRenderer.drawWithGlobalProgram(bgBuilder.end());

            float iconX = bgX + 2f;
            float iconCY = bgY + bgH / 2f;
            int iconColor = 0xFFE0FFE0;

            float headSize = 3f;
            float headX = iconX + (iconSpace - headSize) / 2f;
            float headY = iconCY - 4f;
            var ic = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            ic.vertex(matrix4f, headX, headY + headSize, 0f).color(iconColor);
            ic.vertex(matrix4f, headX + headSize, headY + headSize, 0f).color(iconColor);
            ic.vertex(matrix4f, headX + headSize, headY, 0f).color(iconColor);
            ic.vertex(matrix4f, headX, headY, 0f).color(iconColor);
            BufferRenderer.drawWithGlobalProgram(ic.end());

            float bodyW = 5f;
            float bodyH = 3f;
            float bodyX = iconX + (iconSpace - bodyW) / 2f;
            float bodyY = headY + headSize + 0.5f;
            var ib = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            ib.vertex(matrix4f, bodyX, bodyY + bodyH, 0f).color(iconColor);
            ib.vertex(matrix4f, bodyX + bodyW, bodyY + bodyH, 0f).color(iconColor);
            ib.vertex(matrix4f, bodyX + bodyW, bodyY, 0f).color(iconColor);
            ib.vertex(matrix4f, bodyX, bodyY, 0f).color(iconColor);
            BufferRenderer.drawWithGlobalProgram(ib.end());

            RenderSystem.disableBlend();
        }

        textRenderer.draw(nameToRender, f, 0, -2130706433, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light);
        if (bl) {
            textRenderer.draw(nameToRender, f, 0, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
        }

        matrices.pop();
        ci.cancel();
    }
}
