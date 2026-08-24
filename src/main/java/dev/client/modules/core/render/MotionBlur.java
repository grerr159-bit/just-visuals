package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public class MotionBlur extends Module {
    public static MotionBlur INSTANCE;

    public MotionBlur() {
        super("MotionBlur", ModuleCategory.Visuals, "Искусственная плавность при движении камеры");
        INSTANCE = this;
    }

    public final Slider strength = new Slider("Сила", () -> true)
            .set(0.1f, 1.0f, 0.01f)
            .applyDefault(0.8f)
            .register(this);

    private SimpleFramebuffer prevFbo;
    private int prevW, prevH;
    private float prevYaw, prevPitch;
    private boolean hasPrev = false;
    private boolean needsCapture = true;
    private float smoothDelta = 0f;

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Framebuffer mainFb = MinecraftClient.getInstance().getFramebuffer();
        if (mainFb == null) return;

        int w = mainFb.textureWidth;
        int h = mainFb.textureHeight;

        if (prevFbo == null || prevW != w || prevH != h) {
            if (prevFbo != null) prevFbo.delete();
            prevFbo = new SimpleFramebuffer(w, h, false);
            prevW = w;
            prevH = h;
            hasPrev = false;
            needsCapture = true;
        }

        if (mc.gameRenderer.getCamera() != null) {
            float yaw = mc.gameRenderer.getCamera().getYaw();
            float pitch = mc.gameRenderer.getCamera().getPitch();
            float deltaYaw = yaw - prevYaw;
            float deltaPitch = pitch - prevPitch;
            float rawDelta = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            prevYaw = yaw;
            prevPitch = pitch;

            smoothDelta = smoothDelta * 0.85f + rawDelta * 0.15f;

            float blendAlpha = strength.getValue().floatValue() * Math.min(smoothDelta / 3f, 1f);
            blendAlpha = Math.max(blendAlpha, 0.02f);

            if (hasPrev) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                RenderSystem.disableDepthTest();
                RenderSystem.setShaderTexture(0, prevFbo.getColorAttachment());

                var buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buf.vertex(0, h, -90).texture(0, 1).color(1f, 1f, 1f, blendAlpha);
                buf.vertex(0, 0, -90).texture(0, 0).color(1f, 1f, 1f, blendAlpha);
                buf.vertex(w, 0, -90).texture(1, 0).color(1f, 1f, 1f, blendAlpha);
                buf.vertex(w, h, -90).texture(1, 1).color(1f, 1f, 1f, blendAlpha);
                BufferRenderer.drawWithGlobalProgram(buf.end());

                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }
        }

        prevFbo.beginWrite(false);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        mainFb.draw(prevFbo.textureWidth, prevFbo.textureHeight);
        mainFb.beginWrite(false);
        hasPrev = true;
    }

    @Override
    public void onDisabled() {
        if (prevFbo != null) {
            prevFbo.delete();
            prevFbo = null;
        }
        hasPrev = false;
        smoothDelta = 0f;
    }
}
