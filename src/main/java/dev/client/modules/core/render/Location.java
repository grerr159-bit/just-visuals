package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.KeyBind;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class Location extends Module {
    public static Location INSTANCE;

    public Location() {
        super("Location", ModuleCategory.Visuals, "Метки на координатах");
        INSTANCE = this;
    }

    public final KeyBind setMarkerBind = new KeyBind("Поставить метку", () -> true)
            .set(GLFW.GLFW_KEY_UNKNOWN)
            .applyDefault(GLFW.GLFW_KEY_UNKNOWN)
            .register(this);

    public final KeyBind clearMarkerBind = new KeyBind("Убрать метку", () -> true)
            .set(GLFW.GLFW_KEY_UNKNOWN)
            .applyDefault(GLFW.GLFW_KEY_UNKNOWN)
            .register(this);

    public final CheckBox showBeam = new CheckBox("Луч", () -> true).defaultValue(true).register(this);
    public final CheckBox showHud = new CheckBox("Инфо на экране", () -> true).defaultValue(true).register(this);
    public final Slider beamHeight = new Slider("Высота луча", () -> showBeam.getEnabled()).set(50f, 256f, 10f).defaultValue(128f).register(this);
    public final ColorPicker beamColor = new ColorPicker("Цвет луча", () -> showBeam.getEnabled()).set(0xFF00FF00).defaultValue(0xFF00FF00).register(this);
    public final ColorPicker hudColor = new ColorPicker("Цвет HUD", () -> showHud.getEnabled()).set(0xFFFFFFFF).defaultValue(0xFFFFFFFF).register(this);

    private boolean markerActive = false;
    private double markerX, markerY, markerZ;

    @Subscribe
    public void onKey(KeyBindEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getKey() == setMarkerBind.getKey() && setMarkerBind.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            markerX = mc.player.getX();
            markerY = mc.player.getY();
            markerZ = mc.player.getZ();
            markerActive = true;
        }

        if (event.getKey() == clearMarkerBind.getKey() && clearMarkerBind.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            markerActive = false;
        }
    }

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        if (!markerActive || !showBeam.getEnabled()) return;
        if (mc.player == null || mc.gameRenderer == null) return;

        MatrixStack ms = event.getMatrices();
        float tickDelta = event.getTickCounter().getTickDelta(true);
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        double x = markerX - cam.x;
        double z = markerZ - cam.z;

        int color = beamColor.getColorRGBA();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        double height = beamHeight.getValue();
        float anim = (float) ((System.currentTimeMillis() % 4000L) / 4000.0);
        float alpha = MathHelper.sin(anim * (float) Math.PI * 2) * 0.3f + 0.4f;
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
        int col = (a << 24) | (r << 16) | (g << 8) | b;

        ms.push();
        ms.translate(x, markerY - cam.y, z);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float w = 0.15f;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = ms.peek().getPositionMatrix();

        for (int i = 0; i < 4; i++) {
            float angle = (i / 4f) * (float) Math.PI * 2f;
            float nx = (float) Math.cos(angle) * w;
            float nz = (float) Math.sin(angle) * w;
            float nextAngle = ((i + 1) / 4f) * (float) Math.PI * 2f;
            float nnx = (float) Math.cos(nextAngle) * w;
            float nnz = (float) Math.sin(nextAngle) * w;

            buf.vertex(mat, nx, 0f, nz).color(col);
            buf.vertex(mat, nnx, 0f, nnz).color(col);
            buf.vertex(mat, nnx, (float) height, nnz).color(col);
            buf.vertex(mat, nx, (float) height, nz).color(col);
        }

        int innerCol = (a << 24) | (Math.min(255, r + 80) << 16) | (Math.min(255, g + 80) << 8) | Math.min(255, b + 80);
        for (int i = 0; i < 4; i++) {
            float angle = (i / 4f) * (float) Math.PI * 2f;
            float nx = (float) Math.cos(angle) * w * 0.5f;
            float nz = (float) Math.sin(angle) * w * 0.5f;
            float nextAngle = ((i + 1) / 4f) * (float) Math.PI * 2f;
            float nnx = (float) Math.cos(nextAngle) * w * 0.5f;
            float nnz = (float) Math.sin(nextAngle) * w * 0.5f;

            buf.vertex(mat, nx, 0f, nz).color(innerCol);
            buf.vertex(mat, nnx, 0f, nnz).color(innerCol);
            buf.vertex(mat, nnx, (float) height, nnz).color(innerCol);
            buf.vertex(mat, nx, (float) height, nz).color(innerCol);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        float ringY = (float) (Math.abs(Math.sin(System.currentTimeMillis() / 1000.0)) * 2.0);

        float ringRadius = 0.6f;
        int segments = 40;
        BufferBuilder ringBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2.0;
            float px = (float) Math.cos(ang) * ringRadius;
            float pz = (float) Math.sin(ang) * ringRadius;
            ringBuf.vertex(mat, px, ringY, pz).color(col);
        }
        BufferRenderer.drawWithGlobalProgram(ringBuf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        ms.pop();
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (!markerActive || !showHud.getEnabled()) return;
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;

        DrawContext context = event.getContext();
        int color = hudColor.getColorRGBA();

        double dist = mc.player.getPos().distanceTo(new Vec3d(markerX, markerY, markerZ));

        String text = String.format("Metka: %d / %d / %d (%.1fm)",
                (int) markerX, (int) markerY, (int) markerZ, dist);

        int textW = mc.textRenderer.getWidth(text);
        int screenW = context.getScaledWindowWidth();
        int x = (screenW - textW) / 2;
        int y = 40;

        context.fill(x - 4, y - 2, x + textW + 4, y + 10, 0x80000000);
        context.drawTextWithShadow(mc.textRenderer, text, x, y, color);
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        markerActive = false;
    }
}
