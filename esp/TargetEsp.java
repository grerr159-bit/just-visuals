package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.helper.math.MathVector;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ClientTexture;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.animations.nova.extended.Animation;
import dev.client.api.nullcry.render.core.animations.nova.extended.Direction;
import dev.client.api.nullcry.render.core.animations.nova.extended.core.DecelerateAnimation;
import dev.client.modules.core.combat.KillAura;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import static dev.client.api.nullcry.render.VertexUtils.IMAGE;
import static dev.client.api.nullcry.render.VertexUtils.drawImageQuad;

public class TargetEsp extends Module {

    public TargetEsp() {
        super("Target ESP", ModuleCategory.Render, "Красиво выделяет цель");
    }

    ModeElement mode = new ModeElement("Режим", () -> true).set("Старый Кружок", "Новый Кружок", "Старый Квадрат","Новый Квадрат", "Призраки").defaultValue("Старый Кружок").register(this);

    Slider circleOldSpeed = new Slider("Скорость старого круга", () -> mode.isSelected("Старый Кружок")).set(1, 2, 0.1f).defaultValue(1.5f).register(this);
    Slider circleSpeed = new Slider("Скорость нового круга", () -> mode.isSelected("Новый Кружок")).set(0.5f, 3.0f, 0.1f).defaultValue(1.0f).register(this);

    Slider squareSize  = new Slider("Размер квадрата", () -> mode.isSelected("Старый Квадрат") || mode.isSelected("Новый Квадрат")).set(0.5f, 1, 0.1f).defaultValue(0.5f).register(this);
    Slider squareSpeed = new Slider("Скорость квадрата", () -> mode.isSelected("Старый Квадрат") || mode.isSelected("Новый Квадрат")).set(1, 3, 1).defaultValue(1.5f).register(this);
    CheckBox squareStatic = new CheckBox("Статичный", () -> mode.isSelected("Старый Квадрат") || mode.isSelected("Новый Квадрат")).defaultValue(false).register(this);

    Slider ghostSize = new Slider("Размер призраков", () -> mode.isSelected("Призраки")).set(0.3f, 0.5f, 0.1f).defaultValue(0.3f).register(this);
    Slider ghostSpeed = new Slider("Скорость призраков", () -> mode.isSelected("Призраки")).set(1, 5, 1).defaultValue(3).register(this);

    CheckBox canSee = new CheckBox("Рендерить через стены", () -> !mode.getValue().isEmpty()).defaultValue(true).register(this);
    CheckBox hurtTime = new CheckBox("Менять цвет при уроне", () -> !mode.getValue().isEmpty()).defaultValue(true).register(this);

    ModeElement color = new ModeElement("Цвет", () -> true).set("Клиент", "Свой").register(this);
    ColorPicker customColor = new ColorPicker("Свой цвет", () -> color.isSelected("Свой")).set(-1).defaultValue(-1).register(this);

    public final Animation alpha = new DecelerateAnimation(600, 255);
    private LivingEntity currentTarget;
    long time = System.currentTimeMillis();

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        boolean hasTarget = KillAura.INSTANCE.getTarget() != null;
        alpha.setDirection(isEnabled() && hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);

        if (alpha.finished(Direction.BACKWARDS)) {
            return;
        }

        if (mode.isSelected("Старый Кружок")) {
            renderOldCircle(event);
        } else if (mode.isSelected("Новый Кружок")) {
            renderCircle(event);
        } else if (mode.isSelected("Старый Квадрат") || mode.isSelected("Новый Квадрат")) {
            renderSquare(event);
        } else if (mode.isSelected("Призраки")) {
            renderGhost(event.getMatrices(), event);
        }
    }

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (KillAura.INSTANCE.getTarget() != null) {
            currentTarget = KillAura.INSTANCE.getTarget();
        }
    }

    public void renderOldCircle(RenderEvent.Draw3D event) {
        if (currentTarget == null) return;
        if (!canSeeTarget(currentTarget)) return;

        MatrixStack ms = event.getMatrices();
        float td = event.getTickCounter().getTickDelta(false);
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        double x = MathHelper.lerp(td, currentTarget.prevX, currentTarget.getX()) - cam.x;
        double y = MathHelper.lerp(td, currentTarget.prevY, currentTarget.getY()) - cam.y;
        double z = MathHelper.lerp(td, currentTarget.prevZ, currentTarget.getZ()) - cam.z;

        float height = currentTarget.getHeight();
        float radius = currentTarget.getWidth() * 0.85f;

        double speed = Math.max(0.001, circleOldSpeed.getValue());
        double half = 750.0 / speed;
        double tt = (System.currentTimeMillis() % (long) (2 * half)) / half;
        boolean goingDown = tt > 1.0;
        double tri = goingDown ? (2.0 - tt) : tt;

        double e = tri < 0.5 ? 2.0 * tri * tri : 1.0 - Math.pow(-2.0 * tri + 2.0, 2.0) / 2.0;

        float yBase = (float) (e * height);

        double tailMag = (tri > 0.5 ? 1.0 - tri : tri);
        float tail = (float) (tailMag * height * 0.5);
        float yTail = yBase + (goingDown ? +tail : -tail) * 1.5f;

        int baseColor = baseColor();
        int a = (int) MathHelper.clamp(alpha.getOutput().floatValue(), 0f, 255f);
        int colMain = ColorUtils.setAlpha(baseColor, (int) (a * 0.50f));
        int colTail = ColorUtils.setAlpha(baseColor, (int) (a * 0.20f));
        int colLine = ColorUtils.setAlpha(baseColor, (int) (a * 0.35f));

        ms.push();
        ms.translate(x, y, z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        final int segments = 90;
        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2.0;
            float px = (float) (Math.cos(ang) * radius);
            float pz = (float) (Math.sin(ang) * radius);
            buf.vertex(ms.peek().getPositionMatrix(), px, yBase, pz).color(colMain);
            buf.vertex(ms.peek().getPositionMatrix(), px, yTail, pz).color(colTail);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2.0;
            float px = (float) (Math.cos(ang) * radius);
            float pz = (float) (Math.sin(ang) * radius);
            buf.vertex(ms.peek().getPositionMatrix(), px, yBase, pz).color(colLine);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        ms.pop();
    }

    public void renderCircle(RenderEvent.Draw3D event) {
        if (currentTarget == null) return;
        if (!canSeeTarget(currentTarget)) return;

        MatrixStack ms = event.getMatrices();
        final float tickDelta = event.getTickCounter().getTickDelta(true);
        final Camera camera = mc.getEntityRenderDispatcher().camera;
        final Vec3d camPos = camera.getPos();

        final double radius = 0.4 + currentTarget.getWidth() / 2.0;
        final float quadSize = 0.30f;
        final double spacing = 155.0;
        final int length = (int) (spacing + currentTarget.getWidth());
        final double speedMul = Math.max(0.001, circleSpeed.getValue());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ClientTexture.of("particle/bloom.png").getGlId());

        ms.push();
        ms.translate(-camPos.x, -camPos.y, -camPos.z);

        Vec3d interpolated = MathUtil.interpolate(
                currentTarget.getPos(),
                new Vec3d(currentTarget.lastRenderX, currentTarget.lastRenderY, currentTarget.lastRenderZ),
                tickDelta
        );

        ms.translate(
                interpolated.x + 0.15,
                interpolated.y + 0.2 + currentTarget.getHeight() / 2.0,
                interpolated.z
        );

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        java.util.function.BiConsumer<Float, Integer> emitBillboardQuad = (size, argb) -> {
            org.joml.Matrix4f mat = ms.peek().getPositionMatrix();
            float[][] verts = {
                    { 0f, -size, 0f },
                    { -size, -size, 0f },
                    { -size, 0f, 0f },
                    { 0f, 0f, 0f }
            };
            float[][] uvs = {{0f,0f},{0f,1f},{1f,1f},{1f,0f}};
            for (int vi = 0; vi < 4; vi++) {
                org.joml.Vector4f v = new org.joml.Vector4f(verts[vi][0], verts[vi][1], verts[vi][2], 1f).mul(mat);
                buffer.vertex(v.x, v.y, v.z).texture(uvs[vi][0], uvs[vi][1]).color(argb);
            }
        };

        final long now = System.currentTimeMillis();
        final int alphaNow = MathHelper.clamp(this.alpha.getOutput().intValue(), 0, 255);

        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < length; i++) {
                Quaternionf q = new Quaternionf(camera.getRotation());

                double angle = 0.1 * (now - time - (i * spacing)) / 30.0;
                double s = Math.sin(angle + j * (Math.PI / 1.5)) * radius;
                double c = Math.cos(angle + j * (Math.PI / 1.5)) * radius;
                double yOffset = Math.sin(now * 0.003 * speedMul + j) * 0.8;

                ms.push();
                ms.translate(0.0, yOffset, 0.0);
                ms.translate(s, 0.0, -c);
                ms.translate(-quadSize / 2f, -quadSize / 2f, 0.0);
                ms.multiply(q);
                ms.translate(quadSize / 2f, quadSize / 2f, 0.0);

                int base = baseColor();
                int argb = ColorUtils.setAlpha(base, alphaNow);
                emitBillboardQuad.accept(quadSize, argb);

                ms.pop();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        ms.pop();
    }

    public void renderGhost(MatrixStack matrixStack, RenderEvent.Draw3D event) {
        if (mc == null || mc.world == null || mc.player == null) return;
        if (this.currentTarget == null || this.currentTarget == mc.player || this.currentTarget.isRemoved() || this.currentTarget.isDead()) return;
        if (!canSeeTarget(currentTarget)) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableDepthTest();

        RenderSystem.setShaderTexture(0, ClientTexture.of("particle/bloom.png").getGlId());

        final double radius = 0.67;
        final float size = ghostSize.getValue();
        final double distance = 19.0;
        final int length = 20;
        final int maxAlpha = 255;
        final int alphaFactor = 15;

        final long currentTime = System.currentTimeMillis();
        final float tickDelta = event.getTickCounter().getTickDelta(true);

        matrixStack.push();
        matrixStack.translate(
                -mc.getEntityRenderDispatcher().camera.getPos().x,
                -mc.getEntityRenderDispatcher().camera.getPos().y,
                -mc.getEntityRenderDispatcher().camera.getPos().z
        );

        Vec3d interpolated = MathUtil.interpolate(
                this.currentTarget.getPos(),
                new Vec3d(this.currentTarget.lastRenderX, this.currentTarget.lastRenderY, this.currentTarget.lastRenderZ),
                tickDelta
        );

        matrixStack.translate(
                interpolated.x + 0.2,
                interpolated.y + 0.25 + this.currentTarget.getHeight() / 2.0,
                interpolated.z
        );

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        java.util.function.BiConsumer<Float, Integer> emitBillboardQuad = (quadSize, argb) -> {
            org.joml.Matrix4f mat = matrixStack.peek().getPositionMatrix();
            float[][] verts = {
                    { 0f, -quadSize, 0f },
                    { -quadSize, -quadSize, 0f },
                    { -quadSize, 0f, 0f },
                    { 0f, 0f, 0f }
            };
            float[][] uvs = {{0f,0f},{0f,1f},{1f,1f},{1f,0f}};

            for (int vi = 0; vi < 4; vi++) {
                org.joml.Vector4f v = new org.joml.Vector4f(verts[vi][0], verts[vi][1], verts[vi][2], 1f).mul(mat);
                buffer.vertex(v.x, v.y, v.z).texture(uvs[vi][0], uvs[vi][1]).color(argb);
            }
        };

        for (int i = 0; i < length; i++) {
            Quaternionf r = new Quaternionf(mc.getEntityRenderDispatcher().camera.getRotation());
            double speedFactor = Math.min(ghostSpeed.getValue() / 100.0, 1.0);
            double angle = 0.15 * (currentTime - time - (i * distance)) * speedFactor;
            double dynamicRadius = radius * (1.0 - 0.5 * speedFactor);
            double s = Math.sin(angle) * dynamicRadius;
            double c = Math.cos(angle) * dynamicRadius;

            matrixStack.translate(s, c, -c);
            matrixStack.translate(-size / 2f, -size / 2f, 0);
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);

            int baseColor = baseColor(i);
            int alpha = Math.min(this.alpha.getOutput().intValue(), MathHelper.clamp(maxAlpha - (i * alphaFactor), 0, maxAlpha));
            emitBillboardQuad.accept(size, ColorUtils.reAlphaInt(baseColor, alpha));

            matrixStack.translate(-size / 2f, -size / 2f, 0);
            r.conjugate();
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);
            matrixStack.translate(-s, -c, c);
        }

        for (int i = 0; i < length; i++) {
            Quaternionf r = new Quaternionf(mc.getEntityRenderDispatcher().camera.getRotation());
            double speedFactor = Math.min(ghostSpeed.getValue() / 100.0, 1.0);
            double angle = 0.15 * (currentTime - time - (i * distance)) * speedFactor;
            double dynamicRadius = radius * (1.0 - 0.5 * speedFactor);
            double s = Math.sin(angle) * dynamicRadius;
            double c = Math.cos(angle) * dynamicRadius;

            matrixStack.translate(-s, s, -c);
            matrixStack.translate(-size / 2f, -size / 2f, 0);
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);

            int baseColor = baseColor(i);
            int alpha = Math.min(this.alpha.getOutput().intValue(), MathHelper.clamp(maxAlpha - (i * alphaFactor), 0, maxAlpha));
            emitBillboardQuad.accept(size, ColorUtils.reAlphaInt(baseColor, alpha));

            matrixStack.translate(-size / 2f, -size / 2f, 0);
            r.conjugate();
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);
            matrixStack.translate(s, -s, c);
        }

        for (int i = 0; i < length; i++) {
            Quaternionf r = new Quaternionf(mc.getEntityRenderDispatcher().camera.getRotation());
            double speedFactor = Math.min(ghostSpeed.getValue() / 100.0, 1.0);
            double angle = 0.15 * (currentTime - time - (i * distance)) * speedFactor;
            double dynamicRadius = radius * (1.0 - 0.5 * speedFactor);
            double s = Math.sin(angle) * dynamicRadius;
            double c = Math.cos(angle) * dynamicRadius;

            matrixStack.translate(-s, -s, c);
            matrixStack.translate(-size / 2f, -size / 2f, 0);
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);

            int baseColor = baseColor(i);
            int alpha = Math.min(this.alpha.getOutput().intValue(), MathHelper.clamp(maxAlpha - (i * alphaFactor), 0, maxAlpha));
            emitBillboardQuad.accept(size, ColorUtils.reAlphaInt(baseColor, alpha));

            matrixStack.translate(-size / 2f, -size / 2f, 0);
            r.conjugate();
            matrixStack.multiply(r);
            matrixStack.translate(size / 2f, size / 2f, 0);
            matrixStack.translate(s, s, -c);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        matrixStack.pop();
    }

    public void renderSquare(RenderEvent.Draw3D event) {
        Entity liveTarget = KillAura.INSTANCE.getTarget();
        Entity targetOrLast = liveTarget != null ? liveTarget : currentTarget;
        if (targetOrLast == null) return;
        if (!canSeeTarget(currentTarget)) return;

        int baseColor = baseColor();
        MatrixStack matrices = event.getMatrices();
        Vec3d targetPos = MathVector.lerpPosition(targetOrLast).add(0, 1f, 0).subtract(mc.gameRenderer.getCamera().getPos());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        matrices.push();
        matrices.translate(targetPos);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(mc.player.getYaw(1.0f)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.player.getPitch(1.0f)));

        float sizes = squareSize.getValue();

        if (squareStatic.getEnabled()) {
            double pulse = 0.1 * Math.sin(System.currentTimeMillis() / (300.0 / squareSpeed.getValue()));
            sizes = sizes + (float) (sizes * pulse);
        } else {
            double sin = Math.sin(System.currentTimeMillis() / (1500.0 / squareSpeed.getValue()));
            float angle = (float) (sin * 360.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        }

        matrices.translate(-targetPos.x, -targetPos.y, -targetPos.z);

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = vcp.getBuffer(IMAGE);

        if (mode.isSelected("Старый Квадрат")) {
            RenderSystem.setShaderTexture(0, ClientTexture.of("images/targetesp/old.png").getGlId());
        } else {
            RenderSystem.setShaderTexture(0, ClientTexture.of("images/targetesp/new.png").getGlId());
        }

        int alphaValue = (int) MathHelper.clamp(alpha.getOutput().floatValue(), 0f, 255f);
        int colorWithAlpha = ColorUtils.setAlpha(baseColor, alphaValue);

        drawImageQuad(
                vc,
                matrices.peek().getPositionMatrix(),
                (float) targetPos.x,
                (float) targetPos.y,
                (float) targetPos.z,
                sizes,
                colorWithAlpha
        );

        vcp.drawCurrentLayer();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    boolean canSeeTarget(Entity e) {
        return canSee.getEnabled() || (mc.player != null && mc.player.canSee(e));
    }

    int themeColor() {
        return color.isSelected("Свой")
                ? customColor.getColorRGBA()
                : Interface.INSTANCE.getMainColor();
    }

    int themeColor(int idx) {
        return color.isSelected("Свой")
                ? customColor.getColorRGBA()
                : ColorUtils.gradient(4, idx, Interface.INSTANCE.getMainColor());
    }

    int baseColor(int idx) {
        if (hurtTime.getEnabled() && currentTarget != null && currentTarget.hurtTime > 0) {
            return 0xFFFF0000;
        }
        return themeColor(idx);
    }

    int baseColor() {
        if (hurtTime.getEnabled() && currentTarget != null && currentTarget.hurtTime > 0) {
            return 0xFFFF0000;
        }
        return themeColor();
    }
}
