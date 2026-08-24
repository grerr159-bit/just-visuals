package dev.client.ui.particle;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleUtils {
    
    private static final List<CelestialParticle> particles = new ArrayList<>();
    private static final Random random = new Random();
    private static long lastUpdate = 0;
    private static boolean initialized = false;
    private static final Matrix4f reusableMatrix = new Matrix4f();
    
    public static void drawParticles(float mouseX, float mouseY) {
        try {
            long currentTime = System.currentTimeMillis();
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getWindow() == null) return;
            
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            
            // Инициализируем частицы при первом запуске с правильными размерами
            if (!initialized || particles.isEmpty()) {
                initParticles(width, height);
                initialized = true;
                lastUpdate = currentTime;
            }
            
            // Обновляем частицы каждые 50мс для плавного движения
            if (currentTime - lastUpdate > 50) {
                updateParticles(mouseX, mouseY, width, height);
                lastUpdate = currentTime;
            }
            
            Matrix4f matrix = reusableMatrix.identity();
            
            // Рендерим соединения между частицами
            renderConnections(matrix);
            
            // Рендерим частицы с интерполяцией
            for (CelestialParticle particle : particles) {
                particle.render(matrix);
            }
            
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    private static void initParticles(int width, int height) {
        particles.clear();
        
        // Проверяем, что размеры экрана корректные
        if (width <= 0 || height <= 0) {
            width = 1920; // Значения по умолчанию
            height = 1080;
        }
        
        // Создаем 20 частиц полностью случайно, но с проверкой на расстояние
        for (int i = 0; i < 20; i++) {
            float x, y;
            int attempts = 0;
            
            do {
                // Полностью случайные позиции
                x = (float)(Math.random() * (width - 100) + 50);
                y = (float)(Math.random() * (height - 100) + 50);
                attempts++;
                
                // Если не можем найти хорошую позицию за 50 попыток, используем любую
                if (attempts > 50) break;
                
            } while (isTooCloseToOthers(x, y, 60f)); // Минимальное расстояние 60 пикселей
            
            particles.add(new CelestialParticle(x, y, i));
        }
    }
    
    // Проверяем, не слишком ли близко к другим частицам
    private static boolean isTooCloseToOthers(float x, float y, float minDistance) {
        for (CelestialParticle particle : particles) {
            float dx = x - particle.x;
            float dy = y - particle.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            if (distance < minDistance) {
                return true;
            }
        }
        return false;
    }
    
    private static void updateParticles(float mouseX, float mouseY, int width, int height) {
        // Сначала применяем отталкивание между частицами
        for (int i = 0; i < particles.size(); i++) {
            CelestialParticle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                CelestialParticle p2 = particles.get(j);
                p1.applyRepulsion(p2);
                p2.applyRepulsion(p1);
            }
        }
        
        // Затем обновляем позиции всех частиц
        for (CelestialParticle particle : particles) {
            particle.update(mouseX, mouseY, width, height);
        }
    }
    
    private static void renderConnections(Matrix4f matrix) {
        try {
            int lineColor = ColorUtils.rgba(255, 255, 255, 180); // Белые линии
            
            for (int i = 0; i < particles.size(); i++) {
                CelestialParticle p1 = particles.get(i);
                
                for (int j = i + 1; j < particles.size(); j++) {
                    CelestialParticle p2 = particles.get(j);
                    
                    float dx = p2.x - p1.x;
                    float dy = p2.y - p1.y;
                    float distance = (float)Math.sqrt(dx * dx + dy * dy);
                    
                    // Рисуем линию если частицы близко (как в оригинале)
                    if (distance < 120f) {
                        float alpha = (1f - distance / 120f) * 0.7f;
                        int finalLineColor = ColorUtils.setAlpha(lineColor, (int)(alpha * 255));
                        
                        drawLine(matrix, p1.x, p1.y, p2.x, p2.y, finalLineColor);
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    private static void drawLine(Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        try {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float)Math.sqrt(dx * dx + dy * dy);
            
            if (length < 1f) return;
            
            // Рисуем линию точками (как в оригинале)
            int steps = Math.max(1, (int)(length / 2.5f));
            for (int i = 0; i <= steps; i++) {
                float t = (float)i / steps;
                float x = x1 + dx * t;
                float y = y1 + dy * t;
                
                ClientApi.rectangle()
                        .size(new SizeState(2.5f, 2.5f))
                        .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1.25f))
                        .color(new QuadColorState(color))
                        .build()
                        .render(matrix, x - 1.25f, y - 1.25f);
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    public static void clearParticles() {
        particles.clear();
        initialized = false;
    }
    
    public static void forceReinitialize() {
        particles.clear();
        initialized = false;
    }
    
    private static class CelestialParticle {
        float x, y;
        float vx, vy;
        
        public CelestialParticle(float x, float y, int index) {
            this.x = x;
            this.y = y;
            // Используем индекс для уникального seed'а каждой частицы
            java.util.Random particleRandom = new java.util.Random(index * 12345L + System.currentTimeMillis() / 1000);
            
            // Плавные скорости для частого обновления с уникальными направлениями
            double angle = particleRandom.nextDouble() * 2 * Math.PI;
            float speed = (float)(particleRandom.nextDouble() * 0.25f + 0.15f); // скорость от 0.15 до 0.4
            this.vx = (float)(Math.cos(angle) * speed);
            this.vy = (float)(Math.sin(angle) * speed);
        }
        
        public void update(float mouseX, float mouseY, int width, int height) {
            // Плавное движение частиц
            x += vx;
            y += vy;
            
            // Отражение от границ с отступом
            if (x < 30 || x > width - 30) {
                vx = -vx;
                x = Math.max(30, Math.min(width - 30, x));
            }
            if (y < 30 || y > height - 30) {
                vy = -vy;
                y = Math.max(30, Math.min(height - 30, y));
            }
            
            // Очень небольшая случайность для естественности
            vx += (random.nextFloat() - 0.5f) * 0.003f;
            vy += (random.nextFloat() - 0.5f) * 0.003f;
            
            // Ограничиваем скорость
            float maxSpeed = 0.5f;
            float speed = (float)Math.sqrt(vx * vx + vy * vy);
            if (speed > maxSpeed) {
                vx = (vx / speed) * maxSpeed;
                vy = (vy / speed) * maxSpeed;
            }
            
            // Минимальная скорость для предотвращения остановки
            if (speed < 0.08f) {
                vx += (random.nextFloat() - 0.5f) * 0.02f;
                vy += (random.nextFloat() - 0.5f) * 0.02f;
            }
        }
        
        // Метод для применения силы отталкивания от других частиц
        public void applyRepulsion(CelestialParticle other) {
            float dx = this.x - other.x;
            float dy = this.y - other.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            
            // Если частицы слишком близко, слегка отталкиваем их
            if (distance < 50f && distance > 0) {
                float force = (50f - distance) * 0.0005f; // Уменьшил силу отталкивания
                float forceX = (dx / distance) * force;
                float forceY = (dy / distance) * force;
                
                this.vx += forceX;
                this.vy += forceY;
            }
        }
        
        public void render(Matrix4f matrix) {
            try {
                int color = ColorUtils.rgba(255, 255, 255, 150); // Белые частицы
                
                ClientApi.rectangle()
                        .size(new SizeState(3f, 3f))
                        .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1.5f))
                        .color(new QuadColorState(color))
                        .build()
                        .render(matrix, x - 1.5f, y - 1.5f);
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
    }
}