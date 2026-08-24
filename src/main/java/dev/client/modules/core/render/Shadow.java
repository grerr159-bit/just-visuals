package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.game.TickEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.core.DrawUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;

public class Shadow extends Module {
    public static Shadow INSTANCE;

    public Shadow() {
        super("Shadow", ModuleCategory.Visuals, "Отображает призрачные копии в прошлых позициях");
        INSTANCE = this;
    }

    Slider interval = new Slider("Интервал спавна (сек)", () -> true)
            .set(0.5f, 5f, 0.5f)
            .defaultValue(2.0f)
            .register(this);

    Slider maxShadows = new Slider("Максимум теней", () -> true)
            .set(1, 10, 1)
            .defaultValue(5)
            .register(this);

    Slider fadeTime = new Slider("Время затухания (сек)", () -> true)
            .set(1, 10, 1)
            .defaultValue(3)
            .register(this);

    ModeElement colorMode = new ModeElement("Режим цвета", () -> true)
            .set("Клиентский", "Кастомный", "Радужный")
            .defaultValue("Клиентский")
            .register(this);

    ColorPicker customColor = new ColorPicker("Цвет", () -> colorMode.isSelected("Кастомный"))
            .set(new Color(0, 255, 0, 200).getRGB())
            .defaultValue(new Color(0, 255, 0, 200).getRGB())
            .register(this);

    private final ArrayList<ShadowData> shadows = new ArrayList<>();
    private int tickCounter = 0;
    private Vec3d lastPos = null;

    @Subscribe
    public void onTick(TickEvent event) {
        if (mc.player == null || !isEnabled()) return;

        Vec3d currentPos = mc.player.getPos();
        
        // При первом тике или телепорте (>5 блоков) — очищаем тени
        if (lastPos == null || currentPos.distanceTo(lastPos) > 5.0) {
            shadows.clear();
            tickCounter = 0;
            lastPos = currentPos;
            return;
        }
        
        boolean isMoving = currentPos.distanceTo(lastPos) > 0.01;
        
        tickCounter++;

        // Интервал в тиках: секунды * 20 TPS
        int intervalTicks = (int)(interval.getValue() * 20.0f);

        // Создаем новую тень каждые N тиков, только если игрок двигается
        if (tickCounter >= intervalTicks && isMoving) {
            tickCounter = 0;

            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            float bodyYaw = mc.player.bodyYaw;
            float limbAngle = mc.player.limbAnimator.getPos();
            float limbDistance = mc.player.limbAnimator.getSpeed();

            // Спавним тень прямо за спиной игрока
            double rad = Math.toRadians(yaw + 180.0);
            double spawnX = currentPos.x + Math.sin(rad) * 0.3;
            double spawnZ = currentPos.z + Math.cos(rad) * 0.3;

            shadows.add(new ShadowData(new Vec3d(spawnX, currentPos.y, spawnZ), yaw, pitch, bodyYaw, limbAngle, limbDistance, System.currentTimeMillis()));

            // Удаляем старые тени если превышен лимит
            while (shadows.size() > maxShadows.getValue().intValue()) {
                shadows.remove(0);
            }
        }
        
        lastPos = currentPos;

        // Удаляем тени, которые полностью затухли
        long fadeTimeMs = fadeTime.getValue().intValue() * 1000L;
        shadows.removeIf(shadow -> System.currentTimeMillis() - shadow.time > fadeTimeMs);
    }

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        if (mc.player == null || !isEnabled() || shadows.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        long fadeTimeMs = fadeTime.getValue().intValue() * 1000L;

        matrices.push();
        DrawUtil.setupRender();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        boolean hasDrawn = false; // Флаг для проверки, что мы что-то нарисовали

        for (ShadowData shadow : shadows) {
            float progress = (System.currentTimeMillis() - shadow.time) / (float) fadeTimeMs;
            if (progress > 1.0f) continue;

            // Плавное появление (первые 20%) и затухание (последние 40%)
            float fadeIn = Math.min(1.0f, progress / 0.2f);
            float fadeOut = Math.min(1.0f, (1.0f - progress) / 0.4f);
            float alpha = 0.4f * fadeIn * fadeOut;

            // Получаем цвет
            Color color = getShadowColor(progress);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int a = (int) (alpha * 255);

            // Позиция относительно камеры
            Vec3d cameraPos = camera.getPos();
            double x = shadow.pos.x - cameraPos.x;
            double y = shadow.pos.y - cameraPos.y;
            double z = shadow.pos.z - cameraPos.z;

            // Не рендерим тени слишком близко к камере (< 1.5 блоков)
            double distSq = x * x + y * y + z * z;
            if (distSq < 2.25) continue;

            // Применяем поворот всей модели по yaw игрока
            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, -shadow.bodyYaw));
            
            // Рисуем модель игрока из кубов с анимацией конечностей
            
            // Вычисляем углы для конечностей на основе сохраненной анимации
            float limbSwing = shadow.limbAngle;
            float limbSwingAmount = shadow.limbDistance;
            
            // Ограничиваем амплитуду движения для стабильности
            limbSwingAmount = Math.min(limbSwingAmount, 1.0f);
            
            // Углы для рук и ног (в радианах)
            float rightArmAngle = (float) (Math.cos(limbSwing * 0.6662f) * 2.0f * limbSwingAmount * 0.5f);
            float leftArmAngle = (float) (Math.cos(limbSwing * 0.6662f + Math.PI) * 2.0f * limbSwingAmount * 0.5f);
            float rightLegAngle = (float) (Math.cos(limbSwing * 0.6662f + Math.PI) * 1.4f * limbSwingAmount * 0.5f);
            float leftLegAngle = (float) (Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount * 0.5f);

            // Голова (8x8x8 пикселей = 0.5x0.5x0.5 блоков) с вращением
            matrices.push();
            matrices.translate(0, 1.5, 0); // Позиция шеи
            // Поворот головы по yaw (относительно тела)
            float headYaw = shadow.yaw - shadow.bodyYaw;
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, -headYaw));
            // Поворот головы по pitch (вверх-вниз)
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, shadow.pitch));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            drawPlayerPart(bufferBuilder, matrix, 0, 0, 0, 0.5, 0.5, 0.5, r, g, b, a);
            matrices.pop();
            
            // Тело (8x12x4 пикселей = 0.5x0.75x0.25 блоков)
            matrices.push();
            matrix = matrices.peek().getPositionMatrix();
            drawPlayerPart(bufferBuilder, matrix, 0, 0.75, 0, 0.5, 0.75, 0.25, r, g, b, a);
            matrices.pop();
            
            // Правая рука с вращением
            matrices.push();
            matrices.translate(-0.375, 1.5, 0); // Точка вращения (плечо - верх руки)
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, (float) Math.toDegrees(rightArmAngle)));
            matrix = matrices.peek().getPositionMatrix();
            // Рисуем руку вниз от точки вращения
            drawPlayerPartLocal(bufferBuilder, matrix, 0, -0.75, 0, 0.25, 0.75, 0.25, r, g, b, a);
            matrices.pop();
            
            // Левая рука с вращением
            matrices.push();
            matrices.translate(0.375, 1.5, 0); // Точка вращения (плечо - верх руки)
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, (float) Math.toDegrees(leftArmAngle)));
            matrix = matrices.peek().getPositionMatrix();
            // Рисуем руку вниз от точки вращения
            drawPlayerPartLocal(bufferBuilder, matrix, 0, -0.75, 0, 0.25, 0.75, 0.25, r, g, b, a);
            matrices.pop();
            
            // Правая нога с вращением
            matrices.push();
            matrices.translate(-0.125, 0.75, 0); // Точка вращения (бедро - верх ноги)
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, (float) Math.toDegrees(rightLegAngle)));
            matrix = matrices.peek().getPositionMatrix();
            // Рисуем ногу вниз от точки вращения
            drawPlayerPartLocal(bufferBuilder, matrix, 0, -0.75, 0, 0.25, 0.75, 0.25, r, g, b, a);
            matrices.pop();
            
            // Левая нога с вращением
            matrices.push();
            matrices.translate(0.125, 0.75, 0); // Точка вращения (бедро - верх ноги)
            matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, (float) Math.toDegrees(leftLegAngle)));
            matrix = matrices.peek().getPositionMatrix();
            // Рисуем ногу вниз от точки вращения
            drawPlayerPartLocal(bufferBuilder, matrix, 0, -0.75, 0, 0.25, 0.75, 0.25, r, g, b, a);
            matrices.pop();
            
            matrices.pop(); // Возвращаем матрицу после поворота всей модели
            
            hasDrawn = true;
        }

        // Только если мы что-то нарисовали, завершаем рендер
        if (hasDrawn) {
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        DrawUtil.endRender();
        matrices.pop();
    }

    private Color getShadowColor(float progress) {
        if (colorMode.isSelected("Радужный")) {
            float hue = (System.currentTimeMillis() % 3000) / 3000.0f;
            return Color.getHSBColor(hue, 1.0f, 1.0f);
        } else if (colorMode.isSelected("Клиентский")) {
            return new Color(Interface.INSTANCE.getMainColor(), true);
        } else {
            return new Color(customColor.getColorRGBA(), true);
        }
    }

    private void drawPlayerPart(BufferBuilder bufferBuilder, Matrix4f matrix, double x, double y, double z, double width, double height, double depth, int r, int g, int b, int a) {
        Box box = new Box(
                x - width / 2, y, z - depth / 2,
                x + width / 2, y + height, z + depth / 2
        );

        // Нижняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Верхняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        // Передняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // Задняя грань
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Левая грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // Правая грань
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
    }

    // Рисует куб в локальных координатах (для вращающихся конечностей)
    private void drawPlayerPartLocal(BufferBuilder bufferBuilder, Matrix4f matrix, double x, double y, double z, double width, double height, double depth, int r, int g, int b, int a) {
        // y - это верхняя точка, рисуем вниз
        Box box = new Box(
                x - width / 2, y, z - depth / 2,
                x + width / 2, y + height, z + depth / 2
        );

        // Нижняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Верхняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        // Передняя грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // Задняя грань
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Левая грань
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // Правая грань
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
    }

    @Override
    public void onDisabled() {
        shadows.clear();
        tickCounter = 0;
        lastPos = null;
    }

    private static class ShadowData {
        private final Vec3d pos;
        private final float yaw;
        private final float pitch;
        private final float bodyYaw;
        private final float limbAngle;
        private final float limbDistance;
        private final long time;

        public ShadowData(Vec3d pos, float yaw, float pitch, float bodyYaw, float limbAngle, float limbDistance, long time) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.bodyYaw = bodyYaw;
            this.limbAngle = limbAngle;
            this.limbDistance = limbDistance;
            this.time = time;
        }
    }
}
