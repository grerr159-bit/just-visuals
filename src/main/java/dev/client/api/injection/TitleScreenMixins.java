package dev.client.api.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.authlib.minecraft.BanDetails;
import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.render.ClientTexture;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.render.core.renderers.core.BuiltBlur;
import dev.client.api.nullcry.uiClient.altManager.TitleScreenAlphaAccess;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.modules.core.render.Interface;
import dev.client.ui.particle.ParticleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.client.ui.particle.ParticleNode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixins extends Screen implements TitleScreenAlphaAccess {

    protected TitleScreenMixins(Text title) {
        super(title);
    }

    @Unique private final List<String> nc$texts = new ArrayList<>();
    @Unique private final List<Runnable> nc$actions = new ArrayList<>();
    @Unique private final List<Boolean> nc$disabled = new ArrayList<>();
    @Unique private final dev.client.ui.changelog.ChangeLogManager nc$changeLogManager = new dev.client.ui.changelog.ChangeLogManager();
    @Unique private double nc$scrollOffset = 0.0;

    @Unique private float btnWidth = 200f;
    @Unique private float btnHeight = 20f;
    @Unique private float btnSpacing = 5f;
    @Unique private float btnRadius = 4f;

    @Unique private final CompactAnimation nc$alphaAnimation = new CompactAnimation(Easing.EASE_OUT_CUBIC, 400);
    @Unique private boolean nc$alphaInitialized = false;
    @Unique private boolean nc$transitioning = false;
    @Unique private Runnable nc$pendingAction = null;
    
    @Unique private final List<ParticleNode> nc$particles = new ArrayList<>();
    @Unique private long nc$lastParticleUpdate = 0;
    @Unique private float[][] nc$staticDots = null;
    @Unique private static final Identifier NC$BG_TEXTURE = Identifier.of("just", "images/title_bg.png");

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CursorsUtil.beginFrame();

        var m = ctx.getMatrices().peek().getPositionMatrix();

        float alpha = 1.0f;

        if (nc$transitioning && nc$pendingAction != null) {
            Runnable action = nc$pendingAction;
            nc$pendingAction = null;
            nc$transitioning = false;
            CursorsUtil.applyFrameCursor();
            ci.cancel();
            action.run();
            return;
        }

        // === Фон: чёрный ===
        try {
            int bg = nc$applyAlpha(ColorUtils.rgba(0, 0, 0, 255), alpha);
            ClientApi.rectangle()
                    .size(new SizeState(this.width, this.height))
                    .color(new QuadColorState(bg))
                    .build()
                    .render(m, 0, 0);
        } catch (Exception e) {
            int bg = nc$applyAlpha(ColorUtils.rgba(0, 0, 0, 255), alpha);
            ClientApi.rectangle()
                    .size(new SizeState(this.width, this.height))
                    .color(new QuadColorState(bg))
                    .build()
                    .render(m, 0, 0);
        }

        int mainColor = Interface.INSTANCE.getMainColor();

        final float centerX = this.width / 2f;
        final float centerY = this.height / 2f;

        // ChangeLog панель слева
        nc$renderChangeLog(m, mouseX, mouseY, alpha);

        // Логотип JUST
        int logoColor = nc$applyAlpha(-1, alpha);
        float logoSize = 58f;
        float logoWidth = ClientApi.inter().getWidth("JUST", logoSize);
        ClientApi.text()
                .text("JUST").size(logoSize).font(ClientApi.inter())
                .color(logoColor)
                .build()
                .render(m, centerX - logoWidth / 2f, centerY - 120f);

        // Кнопки (как в примере - компактные)
        final int count = nc$texts.size();
        final float btnX = centerX - btnWidth / 2f;
        float btnY = centerY + 4f;

        boolean anyHover = false;

        for (int i = 0; i < count; i++) {
            String label = nc$texts.get(i);
            boolean disabled = nc$disabled.get(i);
            boolean hover = !disabled && MouseClick.isClick(mouseX, mouseY, btnX, btnY, btnWidth, btnHeight);
            if (hover) anyHover = true;
            
            float buttonAlpha = alpha * (hover ? 1.0f : 0.85f);
            if (disabled) buttonAlpha *= 0.4f;

            // Фон кнопки (темнее, как в примере)
            int btnBg = nc$applyAlpha(ColorUtils.rgba(30, 30, 40, 255), buttonAlpha);
            ClientApi.rectangle()
                    .size(new SizeState(btnWidth, btnHeight))
                    .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(btnRadius))
                    .color(new QuadColorState(btnBg))
                    .build()
                    .render(m, btnX, btnY);

            // Обводка при наведении
            if (hover && !disabled) {
                int borderColor = nc$applyAlpha(mainColor, alpha);
                ClientApi.rectangle()
                        .size(new SizeState(btnWidth, btnHeight))
                        .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(btnRadius))
                        .color(new QuadColorState(ColorUtils.setAlpha(borderColor, (int)(80 * alpha))))
                        .build()
                        .render(m, btnX - 0.5f, btnY - 0.5f);
            }

            // Текст кнопки
            float tw = ClientApi.inter().getWidth(label, 9f);
            float tX = btnX + (btnWidth - tw) / 2f;
            float tY = btnY + (btnHeight - 9f) / 2f;

            int textColor = nc$applyAlpha(-1, alpha);
            int renderColor;
            if (label.equalsIgnoreCase("Exit") && hover) {
                renderColor = nc$applyAlpha(ColorUtils.red, alpha);
            } else {
                renderColor = nc$applyAlpha(textColor, alpha * (disabled ? 0.5f : hover ? 1f : 0.9f));
            }

            ClientApi.text()
                    .text(label).size(9f).font(ClientApi.inter())
                    .color(renderColor)
                    .build()
                    .render(m, tX, tY);

            btnY += btnHeight + btnSpacing;
        }

        // Welcome текст внизу справа (как в примере)
        MinecraftClient mc = MinecraftClient.getInstance();
        String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        String welcome = "Welcome, " + username;
        float welcomeSize = 8f;
        float welcomeWidth = ClientApi.inter().getWidth(welcome, welcomeSize);
        float welcomeX = this.width - welcomeWidth - 5f;
        float welcomeY = this.height - 15f;
        int welcomeColor = nc$applyAlpha(-1, alpha);
        
        ClientApi.text()
                .text(welcome).size(welcomeSize).font(ClientApi.inter())
                .color(welcomeColor)
                .build()
                .render(m, welcomeX, welcomeY);

        // Версия внизу слева (как в примере)
        String version = "Just Client (#1.0)";
        float versionSize = 8f;
        float versionX = 5f;
        float versionY = this.height - 15f;
        int versionColor = nc$applyAlpha(-1, alpha);
        
        ClientApi.text()
                .text(version).size(versionSize).font(ClientApi.inter())
                .color(versionColor)
                .build()
                .render(m, versionX, versionY);

        if (anyHover) {
            CursorsUtil.setCursor(CursorsUtil.HAND);
        }

        CursorsUtil.applyFrameCursor();
        ci.cancel();
    }

    @Unique
    private void nc$renderChangeLog(org.joml.Matrix4f matrix, int mouseX, int mouseY, float alpha) {
        float panelX = 8f;
        float panelY = 23f;
        float panelWidth = (float)this.width / 4f;
        float panelHeight = this.height - 50f;
        
        // Фон панели (как в примере - более прозрачный)
        int panelBg = nc$applyAlpha(ColorUtils.rgba(25, 25, 30, 180), alpha * 0.8f);
        ClientApi.rectangle()
                .size(new SizeState(panelWidth, panelHeight))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(4f))
                .color(new QuadColorState(panelBg))
                .build()
                .render(matrix, panelX, panelY);
        
        // Заголовок
        String title = "ChangeLog";
        float titleSize = 12f;
        int titleColor = nc$applyAlpha(ColorUtils.rgba(185, 185, 185, 255), alpha);
        ClientApi.text()
                .text(title).size(titleSize).font(ClientApi.inter())
                .color(titleColor)
                .build()
                .render(matrix, panelX + 7f, panelY - 13.5f);
        
        // Рендер записей с scissor
        float contentY = panelY + 5f;
        float contentHeight = panelHeight - 10f;
        
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(
            (int)(panelX * 2), 
            (int)((this.height - (contentY + contentHeight)) * 2),
            (int)(panelWidth * 2),
            (int)(contentHeight * 2)
        );
        
        float logY = contentY - (float)nc$scrollOffset;
        int textColor = nc$applyAlpha(ColorUtils.rgba(200, 200, 210, 255), alpha);
        
        for (dev.client.ui.changelog.ChangeLog log : nc$changeLogManager.getChangeLogs()) {
            if (log == null) continue;
            
            ClientApi.text()
                    .text(log.getLogName()).size(9f).font(ClientApi.inter())
                    .color(textColor)
                    .build()
                    .render(matrix, panelX + 5f, logY);
            
            logY += 11f;
        }
        
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        
        // Обработка скролла
        if (MouseClick.isClick(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.mouse != null) {
                double scroll = org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 
                    org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
                // Скролл будет обрабатываться через mouseScrolled
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        BuiltBlur.resetCaptureState();

        nc$texts.clear();
        nc$actions.clear();
        nc$disabled.clear();

        nc$prepareFadeIn();
        nc$alphaInitialized = true;
        nc$transitioning = false;
        nc$pendingAction = null;

        nc$texts.add("Singleplayer");
        nc$actions.add(() -> this.client.setScreen(new SelectWorldScreen((Screen) (Object) this)));
        nc$disabled.add(false);

        Text disabled = onMultiplayerDisabledText();
        nc$texts.add("Multiplayer");
        if (disabled == null) {
            nc$actions.add(() -> {
                Screen screen = this.client.options.skipMultiplayerWarning
                        ? new MultiplayerScreen((Screen) (Object) this)
                        : new MultiplayerWarningScreen((Screen) (Object) this);
                this.client.setScreen(screen);
            });
            nc$disabled.add(false);
        } else {
            nc$actions.add(() -> SystemToast.addWorldAccessFailureToast(this.client, disabled.getString()));
            nc$disabled.add(true);
        }

        nc$texts.add("Altmanager");
        nc$actions.add(() -> nc$startTransition(() -> {
            if (this.client == null) return;
            var alt = Just.getInstance().getAltScreenManager();
            if (alt != null) {
                alt.restartReveal();
            }
            this.client.setScreen(alt);
        }));
        nc$disabled.add(false);

        nc$texts.add("Options");
        nc$actions.add(() -> this.client.setScreen(new OptionsScreen((Screen) (Object) this, this.client.options)));
        nc$disabled.add(false);

        nc$texts.add("Exit");
        nc$actions.add(() -> this.client.scheduleStop());
        nc$disabled.add(false);

        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> ci) {
        if (nc$transitioning) {
            ci.setReturnValue(false);
            ci.cancel();
            return;
        }
        if (button != 0) return;

        final int count = nc$texts.size();
        final float centerX = this.width / 2f;
        final float centerY = this.height / 2f;

        final float bx = centerX - btnWidth / 2f;
        float by = centerY + 4f;

        for (int i = 0; i < count; i++) {
            if (!nc$disabled.get(i) && MouseClick.isClick(mouseX, mouseY, bx, by, btnWidth, btnHeight)) {
                nc$actions.get(i).run();
                ci.setReturnValue(true);
                ci.cancel();
                return;
            }
            by += btnHeight + btnSpacing;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float panelX = 8f;
        float panelY = 23f;
        float panelWidth = (float)this.width / 4f;
        float panelHeight = this.height - 50f;
        
        if (MouseClick.isClick(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            if (verticalAmount < 0) {
                nc$scrollOffset += 28.0;
                if (nc$scrollOffset < 0.0) {
                    nc$scrollOffset = 0.0;
                }
            } else if (verticalAmount > 0) {
                nc$scrollOffset -= 28.0;
                if (nc$scrollOffset < 0.0) {
                    nc$scrollOffset = 0.0;
                }
            }
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Unique
    private void nc$startTransition(Runnable action) {
        if (action == null || nc$transitioning) return;
        nc$pendingAction = action;
        nc$transitioning = true;
    }

    @Unique
    private void nc$renderSimpleParticles(org.joml.Matrix4f matrix, float alpha, int mouseX, int mouseY) {
        // Простые частицы вокруг курсора
        long time = System.currentTimeMillis();
        
        for (int i = 0; i < 15; i++) {
            float seed = i * 45.0f;
            float angle = (float)((time * 0.001f + seed) % (2 * Math.PI));
            float radius = 30f + (float)(Math.sin(time * 0.002f + seed) * 10f);
            
            float x = mouseX + (float)(Math.cos(angle) * radius);
            float y = mouseY + (float)(Math.sin(angle) * radius);
            
            float size = 2f + (float)(Math.sin(time * 0.003f + seed) * 1f);
            
            int particleAlpha = (int)(60 * alpha * (Math.sin(time * 0.004f + seed) * 0.5f + 0.5f));
            int particleColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), particleAlpha);
            
            ClientApi.rectangle()
                    .size(new SizeState(size, size))
                    .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(size / 2f))
                    .color(new QuadColorState(particleColor))
                    .build()
                    .render(matrix, x, y);
        }
    }

    @Unique
    private void nc$renderCelestialParticles(org.joml.Matrix4f matrix, float alpha, int mouseX, int mouseY) {
        try {
            // Используем подход как в оригинальном Celestial Client
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getWindow() == null) return;
            
            double scaledMouseX = mouseX;
            double scaledMouseY = mouseY;
            
            // Вызываем ParticleUtils с правильными координатами
            dev.client.ui.particle.ParticleUtils.drawParticles((float)scaledMouseX, (float)scaledMouseY);
            
        } catch (Exception e) {
            // Если ошибка, рисуем простые статичные частицы
            nc$drawFallbackParticles(matrix, alpha);
        }
    }
    
    @Unique
    private void nc$drawFallbackParticles(org.joml.Matrix4f matrix, float alpha) {
        try {
            // Инициализируем статичные позиции один раз
            if (nc$staticDots == null) {
                nc$staticDots = new float[10][2];
                java.util.Random r = new java.util.Random(777);
                for (int i = 0; i < 10; i++) {
                    nc$staticDots[i][0] = r.nextFloat() * 1600f;
                    nc$staticDots[i][1] = r.nextFloat() * 900f;
                }
            }

            long time = System.currentTimeMillis();
            float timeSeconds = time / 1000.0f;
            int particleColor = nc$applyAlpha(Interface.INSTANCE.getMainColor(), alpha);

            for (int i = 0; i < nc$staticDots.length; i++) {
                float x = nc$staticDots[i][0] + (float)Math.sin(timeSeconds * 0.15f + i * 1.7f) * 50f;
                float y = nc$staticDots[i][1] + (float)Math.cos(timeSeconds * 0.12f + i * 2.3f) * 35f;
                float pulse = (float)(Math.sin(timeSeconds * 0.5f + i) * 0.5f + 0.5f);
                int a = (int)(80 * pulse * alpha);
                if (a < 5) continue;
                ClientApi.rectangle()
                        .size(new SizeState(3f, 3f))
                        .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1.5f))
                        .color(new QuadColorState(ColorUtils.setAlpha(particleColor, a)))
                        .build()
                        .render(matrix, x - 1.5f, y - 1.5f);
            }
        } catch (Exception e) {
        }
    }
    
    @Unique
    private void nc$initCelestialParticles() {
        try {
            nc$particles.clear();
            java.util.Random random = new java.util.Random();
            
            // Создаем 20 частиц по всему экрану (уменьшил количество)
            for (int i = 0; i < 20; i++) {
                ParticleNode particle = new ParticleNode();
                particle.x = random.nextFloat() * this.width;
                particle.y = random.nextFloat() * this.height;
                // ОЧЕНЬ медленная скорость
                particle.vx = (random.nextFloat() - 0.5f) * 0.05f; 
                particle.vy = (random.nextFloat() - 0.5f) * 0.05f;
                nc$particles.add(particle);
            }
        } catch (Exception e) {
            nc$particles.clear();
        }
    }
    
    @Unique
    private void nc$updateCelestialParticles(int mouseX, int mouseY) {
        try {
            for (ParticleNode particle : nc$particles) {
                // ОЧЕНЬ медленное движение
                particle.x += particle.vx;
                particle.y += particle.vy;
                
                // Отражение от границ
                if (particle.x < 10 || particle.x > this.width - 10) {
                    particle.vx = -particle.vx;
                    particle.x = Math.max(10, Math.min(this.width - 10, particle.x));
                }
                if (particle.y < 10 || particle.y > this.height - 10) {
                    particle.vy = -particle.vy;
                    particle.y = Math.max(10, Math.min(this.height - 10, particle.y));
                }
                
                // Минимальная случайность
                particle.vx += (Math.random() - 0.5) * 0.001f;
                particle.vy += (Math.random() - 0.5) * 0.001f;
                
                // Ограничиваем скорость
                float maxSpeed = 0.1f;
                if (Math.abs(particle.vx) > maxSpeed) particle.vx = Math.signum(particle.vx) * maxSpeed;
                if (Math.abs(particle.vy) > maxSpeed) particle.vy = Math.signum(particle.vy) * maxSpeed;
            }
        } catch (Exception e) {
            // Игнорируем ошибки обновления
        }
    }
    
    @Unique
    private int nc$applyAlpha(int color, float factor) {
        float clamped = Math.max(0f, Math.min(1f, factor));
        int baseAlpha = ColorUtils.alpha(color);
        int newAlpha = Math.round(baseAlpha * clamped);
        return ColorUtils.setAlpha(color, newAlpha);
    }

    @Unique
    private void nc$prepareFadeIn() {
        nc$alphaAnimation.setValue(0.0);
        nc$alphaAnimation.setDestinationValue(1.0);
        nc$alphaAnimation.reset();
    }

    @Unique
    private @Nullable Text onMultiplayerDisabledText() {
        if (this.client.isMultiplayerEnabled()) return null;
        else if (this.client.isUsernameBanned()) return Text.translatable("title.multiplayer.disabled.banned.name");
        else {
            BanDetails banDetails = this.client.getMultiplayerBanDetails();
            if (banDetails != null) {
                return banDetails.expires() != null
                        ? Text.translatable("title.multiplayer.disabled.banned.temporary")
                        : Text.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Text.translatable("title.multiplayer.disabled");
            }
        }
    }

    @Override
    public void Just$showInstantly() {
        nc$alphaInitialized = true;
        nc$transitioning = false;
        nc$pendingAction = null;
        nc$alphaAnimation.force(1.0);
    }

    @Unique
    private static final java.util.Map<Character, String[]> NC$PIXEL_PATTERNS = new java.util.HashMap<>();
    static {
        NC$PIXEL_PATTERNS.put('J', new String[]{
            "..#####.",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            ".#.##...",
            ".###....",
            "..#....."
        });
        NC$PIXEL_PATTERNS.put('U', new String[]{
            ".##...##",
            ".##...##",
            ".##...##",
            ".##...##",
            ".##...##",
            ".##...##",
            ".##...##",
            ".##...##",
            "..#####."
        });
        NC$PIXEL_PATTERNS.put('S', new String[]{
            "..#####.",
            ".##...##",
            ".##.....",
            ".##.....",
            "..####..",
            ".....##.",
            ".....##.",
            ".##...##",
            "..#####."
        });
        NC$PIXEL_PATTERNS.put('T', new String[]{
            "########",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "...##..."
        });
    }

    @Unique
    private void nc$renderPixelText(org.joml.Matrix4f matrix, String text, float centerX, float y, float pixelSize, int color) {
        char[] chars = text.toCharArray();
        float totalWidth = 0;
        for (char c : chars) {
            String[] pattern = NC$PIXEL_PATTERNS.get(Character.toUpperCase(c));
            if (pattern != null) {
                totalWidth += pattern[0].length() * pixelSize + pixelSize;
            }
        }
        totalWidth -= pixelSize;

        float cursorX = centerX - totalWidth / 2f;

        for (char c : chars) {
            String[] pattern = NC$PIXEL_PATTERNS.get(Character.toUpperCase(c));
            if (pattern != null) {
                for (int row = 0; row < pattern.length; row++) {
                    for (int col = 0; col < pattern[row].length(); col++) {
                        if (pattern[row].charAt(col) == '#') {
                            ClientApi.rectangle()
                                    .size(new SizeState(pixelSize, pixelSize))
                                    .color(new QuadColorState(color))
                                    .build()
                                    .render(matrix, cursorX + col * pixelSize, y + row * pixelSize);
                        }
                    }
                }
                cursorX += pattern[0].length() * pixelSize + pixelSize;
            }
        }
    }
}
