package dev.client.api.nullcry.render.core.renderers.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.render.core.providers.ResourceProvider;
import dev.client.api.nullcry.render.core.renderers.IRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float Smoothness,
        float blurRadius
) implements IRenderer {

    private static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(1920, 1024, false));
    private static final Framebuffer MAIN_FBO = MinecraftClient.getInstance().getFramebuffer();
    private static float lastCaptureGameTime = Float.NaN;
    private static int lastCapturedWidth = -1;
    private static int lastCapturedHeight = -1;
    private static Screen lastCapturedScreen = null;
    private static boolean forceNextCapture = false;

    public static void resetCaptureState() {
        lastCaptureGameTime = Float.NaN;
        lastCapturedWidth = -1;
        lastCapturedHeight = -1;
        lastCapturedScreen = null;
        forceNextCapture = true;

        SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        fbo.beginWrite(false);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        MAIN_FBO.beginWrite(false);
    }

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        if (this.blurRadius <= 0.0f) {
            return;
        }

        float width = this.size.width();
        float height = this.size.height();
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        int alphaMask = 0;
        alphaMask |= (this.color.color1() >>> 24) & 0xFF;
        alphaMask |= (this.color.color2() >>> 24) & 0xFF;
        alphaMask |= (this.color.color3() >>> 24) & 0xFF;
        alphaMask |= (this.color.color4() >>> 24) & 0xFF;
        if (alphaMask == 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Screen currentScreen = client.currentScreen;
        if (currentScreen != lastCapturedScreen) {
            forceNextCapture = true;
            lastCapturedScreen = currentScreen;
        }

        SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        if (fbo.textureWidth != MAIN_FBO.textureWidth || fbo.textureHeight != MAIN_FBO.textureHeight) {
            fbo.resize(MAIN_FBO.textureWidth, MAIN_FBO.textureHeight);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float gameTime = RenderSystem.getShaderGameTime();
        boolean sizeChanged = fbo.textureWidth != lastCapturedWidth || fbo.textureHeight != lastCapturedHeight;
        if (forceNextCapture || sizeChanged || Float.compare(lastCaptureGameTime, gameTime) != 0) {
            fbo.beginWrite(false);
            MAIN_FBO.draw(fbo.textureWidth, fbo.textureHeight);
            MAIN_FBO.beginWrite(false);

            lastCaptureGameTime = gameTime;
            lastCapturedWidth = fbo.textureWidth;
            lastCapturedHeight = fbo.textureHeight;
            forceNextCapture = false;
        }

        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

        ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER_KEY);
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.Smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}