package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.game.JumpEvent;
import dev.client.api.nullcry.events.core.game.TickEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ClientTexture;
import dev.client.api.nullcry.render.core.DrawUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;

public class  JumpCircles extends Module {

    public JumpCircles() {
        super("JumpCircles", ModuleCategory.Visuals, "Отображает круги при прыжке");
    }

    ModeElement mode = new ModeElement("Режим",() -> true).set("Glow","Client","Blocks").defaultValue("Client").register(this);
    Slider radiusCircle = new Slider("Setting", () -> true).set(1,6,1).defaultValue(4).register(this);
    CheckBox renderThroughBlocks = new CheckBox("Рендерить через блоки", () -> true).defaultValue(true).register(this);
    ModeElement color = new ModeElement("Цвет", () -> true).set("Клиентский", "Кастомный").defaultValue("Клиентский").register(this);
    ColorPicker customColor = new ColorPicker("Выбрать цвет", () -> color.isSelected("Кастомный")).set(-1).register(this);

    private final ArrayList<Circle> circles = new ArrayList<>();

    @Subscribe
    public void onJump(JumpEvent event) {
        if (mc.player == null) return;
        if (event.getEntity() != mc.player) return;

        Vec3d pos = event.getEntity().getPos().add(0, 0.05, 0);
        circles.add(new Circle(pos));
    }

    @Subscribe
    public void onTick(TickEvent event) {
        circles.removeIf(circle -> System.currentTimeMillis() - circle.time > 1000);
    }

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        if (circles.isEmpty()) return;
        
        if (mode.isSelected("Blocks")) {
            renderBlockHighlights(event);
        } else {
            renderCircleTextures(event);
        }
    }

    private void renderBlockHighlights(RenderEvent.Draw3D event) {
        for (Circle circle : circles) {
            float progress = (System.currentTimeMillis() - circle.time) / 1000.0f;
            if (progress > 1.0f) continue;

            float alpha = MathUtil.lerp(1.0f, 0.0f, progress * progress);
            float radius = MathUtil.lerp(0.5f, radiusCircle.getValue().floatValue(), progress);

            Color circleColor = color.isSelected("Кастомный")
                    ? new Color(customColor.getColorRGBA(), true)
                    : new Color(Interface.INSTANCE.getMainColor(), true);

            int alphaInt = (int) (alpha * 255);
            int highlightColor = new Color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), alphaInt).getRGB();

            BlockPos centerPos = BlockPos.ofFloored(circle.position);
            List<BlockPos> blocksToHighlight = getBlocksInRadius(centerPos, radius);

            for (BlockPos pos : blocksToHighlight) {
                if (mc.world == null) continue;
                BlockState state = mc.world.getBlockState(pos);
                if (state.isAir()) continue;

                VoxelShape shape = state.getOutlineShape(mc.world, pos);
                if (shape.isEmpty()) continue;

                dev.client.api.nullcry.render.core.Draw3DUtil.drawShapeAlternative(
                        pos,
                        shape,
                        highlightColor,
                        2,
                        true,
                        true
                );
            }
        }
    }

    private List<BlockPos> getBlocksInRadius(BlockPos center, float radius) {
        List<BlockPos> blocks = new ArrayList<>();
        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= radius) {
                    blocks.add(center.add(x, 0, z));
                    blocks.add(center.add(x, -1, z));
                }
            }
        }
        return blocks;
    }

    private void renderCircleTextures(RenderEvent.Draw3D event) {
        MatrixStack matrices = event.getMatrices();
        Camera camera = mc.getEntityRenderDispatcher().camera;

        matrices.push();
        DrawUtil.setupRender();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        boolean renderInBlocks = renderThroughBlocks.getEnabled();
        if (renderInBlocks) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }

        if (mode.isSelected("Glow")) {
            RenderSystem.setShaderTexture(0, ClientTexture.of("images/jumpcircles/glow.png").getGlId());
        } else {
            RenderSystem.setShaderTexture(0, ClientTexture.of("images/jumpcircles/client.png").getGlId());
        }

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (Circle circle : circles) {
            float progress = (System.currentTimeMillis() - circle.time) / 1000.0f;
            float radius = MathUtil.lerp(0.0f, radiusCircle.getValue(), progress);
            float alpha = MathUtil.lerp(1.0f, 0.0f, progress * progress);
            if (progress > 1.0f) alpha = 0.0f;

            Vec3d pos = circle.position;
            double x = pos.x - camera.getPos().x;
            double y = pos.y - camera.getPos().y;
            double z = pos.z - camera.getPos().z;

            float halfSize = radius / 2.0f;
            float minX = (float) x - halfSize;
            float maxX = (float) x + halfSize;
            float minZ = (float) z - halfSize;
            float maxZ = (float) z + halfSize;

            Color circleColor = color.isSelected("Кастомный")
                    ? new Color(customColor.getColorRGBA(), true)
                    : new Color(Interface.INSTANCE.getMainColor(), true);

            int alphaInt = (int) (alpha * 255);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            bufferBuilder.vertex(matrix, minX, (float) y, minZ).texture(0.0f, 0.0f).color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), alphaInt);
            bufferBuilder.vertex(matrix, maxX, (float) y, minZ).texture(1.0f, 0.0f).color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), alphaInt);
            bufferBuilder.vertex(matrix, maxX, (float) y, maxZ).texture(1.0f, 1.0f).color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), alphaInt);
            bufferBuilder.vertex(matrix, minX, (float) y, maxZ).texture(0.0f, 1.0f).color(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), alphaInt);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        if (renderInBlocks) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
        DrawUtil.endRender();
        matrices.pop();
    }

    private static class Circle {
        private final Vec3d position;
        private final long time;

        public Circle(Vec3d position) {
            this.position = position;
            this.time = System.currentTimeMillis();
        }
    }
}
