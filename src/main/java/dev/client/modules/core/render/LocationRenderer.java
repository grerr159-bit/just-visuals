package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.KeyBind;
import dev.client.api.nullcry.uiClient.clickGui.newgui.LocationStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class LocationRenderer extends Module {
    public static LocationRenderer INSTANCE;

    public LocationRenderer() {
        super("Location", ModuleCategory.Visuals, "Метки на координатах");
        INSTANCE = this;
        setEnabled(true);
    }

    public final KeyBind saveBind = new KeyBind("Сохранить позицию", () -> true)
            .set(GLFW.GLFW_KEY_UNKNOWN)
            .applyDefault(GLFW.GLFW_KEY_UNKNOWN)
            .register(this);

    @Subscribe
    public void onKey(KeyBindEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getKey() == saveBind.getKey() && saveBind.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            BlockPos pos = mc.player.getBlockPos();
            String name = "Waypoint " + (LocationStorage.getWaypoints().size() + 1);
            LocationStorage.addWaypoint(name, pos.getX(), pos.getY(), pos.getZ(), 0xFF00FF00, 0, false);
            return;
        }

        LocationStorage.Waypoint wp = LocationStorage.findByKey(event.getKey());
        if (wp != null) {
            LocationStorage.toggleWaypoint(wp.name);
        }
    }

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        if (mc.player == null || mc.gameRenderer == null || mc.world == null) return;

        List<LocationStorage.Waypoint> waypoints = LocationStorage.getWaypoints();
        for (LocationStorage.Waypoint wp : waypoints) {
            if (!wp.enabled) continue;
            renderWaypoint(event, wp);
        }
    }

    private void renderWaypoint(RenderEvent.Draw3D event, LocationStorage.Waypoint wp) {
        MatrixStack ms = event.getMatrices();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        double px = wp.x + 0.5 - cam.x;
        double pz = wp.z + 0.5 - cam.z;
        double py = wp.y - cam.y;

        int color = wp.color;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float time = (System.currentTimeMillis() % 3000L) / 3000f;
        float pulse = (float) (Math.sin(time * Math.PI * 2) * 0.3 + 0.7);
        int a = MathHelper.clamp((int) (pulse * 255), 80, 255);
        int col = (a << 24) | (r << 16) | (g << 8) | b;

        int brightA = MathHelper.clamp(a + 60, 0, 255);
        int brightCol = (brightA << 24) | (Math.min(255, r + 100) << 16) | (Math.min(255, g + 100) << 8) | Math.min(255, b + 100);

        ms.push();
        ms.translate(px, py, pz);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f mat = ms.peek().getPositionMatrix();
        float beamW = 0.5f;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int face = 0; face < 4; face++) {
            float angle = (face / 4f) * (float) Math.PI * 2f;
            float nx = (float) Math.cos(angle) * beamW;
            float nz = (float) Math.sin(angle) * beamW;
            float nextAngle = ((face + 1) / 4f) * (float) Math.PI * 2f;
            float nnx = (float) Math.cos(nextAngle) * beamW;
            float nnz = (float) Math.sin(nextAngle) * beamW;

            buf.vertex(mat, nx, 0f, nz).color(col);
            buf.vertex(mat, nnx, 0f, nnz).color(col);
            buf.vertex(mat, nnx, 256f, nnz).color(col);
            buf.vertex(mat, nx, 256f, nz).color(col);
        }

        float innerW = beamW * 0.4f;
        for (int face = 0; face < 4; face++) {
            float angle = (face / 4f) * (float) Math.PI * 2f;
            float nx = (float) Math.cos(angle) * innerW;
            float nz = (float) Math.sin(angle) * innerW;
            float nextAngle = ((face + 1) / 4f) * (float) Math.PI * 2f;
            float nnx = (float) Math.cos(nextAngle) * innerW;
            float nnz = (float) Math.sin(nextAngle) * innerW;

            buf.vertex(mat, nx, 0f, nz).color(brightCol);
            buf.vertex(mat, nnx, 0f, nnz).color(brightCol);
            buf.vertex(mat, nnx, 256f, nnz).color(brightCol);
            buf.vertex(mat, nx, 256f, nz).color(brightCol);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        float diamondSize = 1.2f;
        int diamondCol = (Math.min(255, a + 40) << 24) | (r << 16) | (g << 8) | b;
        BufferBuilder diamondBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        diamondBuf.vertex(mat, 0f, 0.02f, 0f).color(brightCol);
        diamondBuf.vertex(mat, diamondSize, 0.02f, 0f).color(diamondCol);
        diamondBuf.vertex(mat, 0f, 0.02f, diamondSize).color(diamondCol);
        diamondBuf.vertex(mat, -diamondSize, 0.02f, 0f).color(diamondCol);
        diamondBuf.vertex(mat, 0f, 0.02f, -diamondSize).color(diamondCol);
        diamondBuf.vertex(mat, diamondSize, 0.02f, 0f).color(diamondCol);
        BufferRenderer.drawWithGlobalProgram(diamondBuf.end());

        float ringY = 2f + (float) Math.sin(System.currentTimeMillis() / 800.0) * 0.5f;
        float ringRadius = 0.8f;
        int segments = 60;
        BufferBuilder ringBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2.0;
            float rx = (float) Math.cos(ang) * ringRadius;
            float rz = (float) Math.sin(ang) * ringRadius;
            ringBuf.vertex(mat, rx, ringY, rz).color(col);
        }
        BufferRenderer.drawWithGlobalProgram(ringBuf.end());

        float ring2Y = ringY + 1f;
        float ring2Radius = 0.5f;
        BufferBuilder ringBuf2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2.0;
            float rx = (float) Math.cos(ang) * ring2Radius;
            float rz = (float) Math.sin(ang) * ring2Radius;
            ringBuf2.vertex(mat, rx, ring2Y, rz).color(brightCol);
        }
        BufferRenderer.drawWithGlobalProgram(ringBuf2.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        ms.pop();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (mc.player == null || mc.world == null || mc.getDebugHud().shouldShowDebugHud()) return;

        DrawContext context = event.getContext();
        List<LocationStorage.Waypoint> waypoints = LocationStorage.getWaypoints();

        int yOffset = 40;
        for (LocationStorage.Waypoint wp : waypoints) {
            if (!wp.enabled) continue;

            double dist = mc.player.getPos().distanceTo(new Vec3d(wp.x + 0.5, wp.y + 0.5, wp.z + 0.5));
            String text = String.format("%s: %d %d %d (%.0fm)", wp.name, wp.x, wp.y, wp.z, dist);

            int textW = mc.textRenderer.getWidth(text);
            int screenW = context.getScaledWindowWidth();
            int xPos = (screenW - textW) / 2;

            context.fill(xPos - 6, yOffset - 3, xPos + textW + 6, yOffset + 11, 0xA0000000);
            context.fill(xPos - 6, yOffset - 3, xPos - 3, yOffset + 11, wp.color);
            context.drawTextWithShadow(mc.textRenderer, text, xPos, yOffset, 0xFFFFFFFF);

            yOffset += 16;
        }
    }
}
