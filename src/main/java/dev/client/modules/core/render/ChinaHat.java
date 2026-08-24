package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.awt.*;

public class ChinaHat extends Module {
    public static ChinaHat INSTANCE;

    public ChinaHat() {
        super("ChinaHat", ModuleCategory.Visuals, "Отображает китайскую шляпу над головой");
    }

    CheckBox friend = new CheckBox("Отображать на друзьях", () -> true).defaultValue(true).register(this);
    ModeElement modes = new ModeElement("Режим шляпы", () -> true).set("Default", "Nimb").defaultValue("Default").register(this);
    Slider radiusNimb = new Slider("Setting",  () -> modes.isSelected("Nimb")).set(0.5f,1.0f,0.1f).defaultValue(0.7f).register(this);
    ModeElement color = new ModeElement("Цвет", () -> true).set("Клиентский", "Кастомный").defaultValue("Клиентский").register(this);
    ColorPicker customColor = new ColorPicker("Выбрать цвет", () -> color.isSelected("Кастомный")).set(-1).register(this);

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        renderHat(event, mc.player);

        if (friend.getEnabled()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player && handlerClient().getFriendManager().isFriend(player.getName().getString())) {
                    renderHat(event, player);
                }
            }
        }
    }

    public static void renderHat(RenderEvent.Draw3D event, PlayerEntity player) {
        if ((mc.options.getPerspective().isFirstPerson() && player == mc.player) || player == null) return;
        MatrixStack matrices = getStack(event, player);

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (ChinaHat.INSTANCE.modes.isSelected("Nimb")) {
            renderNimb(matrices);
        } else {
            renderDefaultHat(matrices);
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1f);
        matrices.pop();
    }

    private static void renderDefaultHat(MatrixStack matrices) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, 0, 0.3f, 0).color(ChinaHat.INSTANCE.color.isSelected("Кастомный") ? ChinaHat.INSTANCE.customColor.getColorRGBA() : Interface.INSTANCE.getMainColor());

        for (int i = 0; i <= 360; i += 10) {
            int hatColor = ChinaHat.INSTANCE.color.isSelected("Кастомный")
                    ? ChinaHat.INSTANCE.customColor.getColorRGBA()
                    : ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(i), 128);

            float xOffset = MathHelper.sin((float) Math.toRadians(i)) * 0.65F;
            float zOffset = -MathHelper.cos((float) Math.toRadians(i)) * 0.65F;
            buffer.vertex(matrix, xOffset, 0, zOffset).color(hatColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(3f);
        BufferBuilder buffer1 = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; i += 10) {
            int hatColor = ChinaHat.INSTANCE.color.isSelected("Кастомный")
                    ? ChinaHat.INSTANCE.customColor.getColorRGBA()
                    : Interface.INSTANCE.getMainColor(0) | 0xFF000000;
            float xOffset = MathHelper.sin(i * MathHelper.RADIANS_PER_DEGREE) * 0.65F;
            float zOffset = -MathHelper.cos(i * MathHelper.RADIANS_PER_DEGREE) * 0.65F;
            buffer1.vertex(matrix, xOffset, 0, zOffset)
                    .color(hatColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer1.end());
    }

    private static void renderNimb(MatrixStack matrices) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ClientTexture.of("images/jumpcircles/glow.png").getGlId());

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Color color = new Color(ChinaHat.INSTANCE.color.isSelected("Кастомный")
                ? ChinaHat.INSTANCE.customColor.getColorRGBA()
                : Interface.INSTANCE.getMainColor(), true);

        int alpha = color.getAlpha();
        if (alpha == 0) {
            alpha = 255;
        }

        float radius = ChinaHat.INSTANCE.radiusNimb.getValue();
        float yOffset = 0.10f;

        buffer.vertex(matrix, 0, yOffset, 0)
                .texture(0.5f, 0.5f)
                .color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        for (int i = 0; i <= 360; i += 10) {
            float radians = (float) Math.toRadians(i);
            float sin = MathHelper.sin(radians);
            float cos = MathHelper.cos(radians);
            float xOffset = sin * radius;
            float zOffset = -cos * radius;
            float u = 0.5f + sin * 0.5f;
            float v = 0.5f + cos * 0.5f;

            buffer.vertex(matrix, xOffset, yOffset, zOffset)
                    .texture(u, v)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 0.8f));
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static @NotNull MatrixStack getStack(RenderEvent.Draw3D event, PlayerEntity player) {
        MatrixStack matrices = event.getMatrices();

        matrices.push();
        double x = MathVector.interpolate(player.prevX, player.getX(), MathUtil.getTickDelta());
        double y = MathVector.interpolate(player.prevY, player.getY(), MathUtil.getTickDelta());
        double z = MathVector.interpolate(player.prevZ, player.getZ(), MathUtil.getTickDelta());
        matrices.translate(x - mc.gameRenderer.getCamera().getPos().x, y + player.getHeight() - mc.gameRenderer.getCamera().getPos().y, z - mc.gameRenderer.getCamera().getPos().z);
        return matrices;
    }
}
