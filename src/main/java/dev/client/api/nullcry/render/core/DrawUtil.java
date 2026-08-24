package dev.client.api.nullcry.render.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.render.core.renderers.core.BuiltHeadTexture;
import dev.client.api.nullcry.render.core.renderers.core.BuiltTexture;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@UtilityClass
public class DrawUtil implements ClientApi {
    public static void renderOutline(Box box, Color color, float lineWidth) {
        setupRender();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        RenderSystem.lineWidth(lineWidth);

        setOutlinePoints(box, matrixFrom(box.minX, box.minY, box.minZ), buffer, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        endRender();
    }

    public static void setOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color) {
        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
    }

    public void drawCircle(MatrixStack matrices, float cx, float cy, float radius, Color c) {
        int segments = 1000;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        setupRender();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2);

        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        BufferBuilder builder = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float xOffset = (float) Math.cos(angle) * radius;
            float yOffset = (float) Math.sin(angle) * radius;
            builder.vertex(matrix, cx + xOffset, cy + yOffset, 0.0F).color(c.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        endRender();
    }

    public void drawFilledCircle(MatrixStack matrices, float cx, float cy, float radius, int c) {
        int segments = Math.max(20, (int) (radius * 8));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        setupRender();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, cx, cy, 0.0F).color(c);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float xOffset = (float) Math.cos(angle) * radius;
            float yOffset = (float) Math.sin(angle) * radius;
            builder.vertex(matrix, cx + xOffset, cy + yOffset, 0.0F).color(c);
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableCull();
        endRender();
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public void drawItemStack(DrawContext poseStack, ItemStack stack, double x, double y, float scale) {
        MathUtil.scaleStart(poseStack.getMatrices(), (float) (x + 8), (float) (y + 8), scale);
        poseStack.getMatrices().scale(-0.01f * -200, -0.01f * -200, -0.01f * -200);
        RenderSystem.enableDepthTest();
        poseStack.getMatrices().scale(0.5f, 0.5f, 0.5f);
        poseStack.drawItem(stack, (int) x, (int) y);
        poseStack.drawStackOverlay(mc.textRenderer, stack, (int) x, (int) y);
        RenderSystem.disableDepthTest();
        MathUtil.scaleEnd(poseStack.getMatrices());
    }

    public void renderTexture(MatrixStack stack, float x, float y, float width, float height, float radius, float u, float v, float textWidth, float texHeight, Identifier texture, Color color) {
        BuiltTexture built = ClientApi.drawImage().size(new SizeState(width, height)).radius(new QuadRadiusState(radius)).texture(u, v, textWidth, texHeight, mc.getTextureManager().getTexture(texture)).color(new QuadColorState(color)).build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void renderHeadTexture(MatrixStack stack, float x, float y, float width, float height, float radius, float u, float v, float texWidth, float texHeight, Identifier texture, Color color) {
        renderHeadTexture(stack, x, y, width, height, radius, u, v, texWidth, texHeight, texture, color, 1.0f);
    }

    public void renderHeadTexture(MatrixStack stack, float x, float y, float width, float height, float radius, float u, float v, float texWidth, float texHeight, Identifier texture, Color color, float smoothness) {
        BuiltHeadTexture built = ClientApi.headTexture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .texture(u, v, texWidth, texHeight, mc.getTextureManager().getTexture(texture))
                .color(new QuadColorState(color))
                .smoothness(smoothness)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);

        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);
        return matrices;
    }

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
