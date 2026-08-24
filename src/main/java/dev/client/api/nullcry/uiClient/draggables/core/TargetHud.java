package dev.client.api.nullcry.uiClient.draggables.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.events.core.player.PlayerAttackEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.client.PlayerHelper;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.uiClient.draggables.GlassShadow;
import dev.client.api.nullcry.render.ClientTexture;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.DrawUtil;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingProvider;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.api.nullcry.uiClient.draggables.IHelper;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import dev.client.api.nullcry.uiClient.draggables.settings.PanelAlphaProvider;
import dev.client.modules.core.misc.NameProtect;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static dev.client.api.nullcry.render.VertexUtils.IMAGE;
import static dev.client.api.nullcry.render.VertexUtils.drawImageQuad2;

public class TargetHud implements IHelper, SettingProvider, PanelAlphaProvider {
    final DraggableHandler draggableHandler;
    private final List<Setting> settings = new ArrayList<>();

    public final ModeElement colorBar = new ModeElement("Цвет BossBar", () -> true)
            .set("Клиентский", "Состояние здоровья")
            .defaultValue("Клиентский")
            .register(this);
    public final CheckBox particleTargetHud = new CheckBox("Частицы", () -> true)
            .defaultValue(true)
            .register(this);
    public final CheckBox armorStack = new CheckBox("Отображать броню", () -> true)
            .defaultValue(true)
            .register(this);
    public final CheckBox rayTrace = new CheckBox("RayTrace", () -> true)
            .defaultValue(true)
            .register(this);
    public final Slider particleValue = new Slider("Кол-во частиц", particleTargetHud::getEnabled)
            .set(1, 20, 1)
            .defaultValue(10)
            .register(this);

    public final ColorPicker bgColor = new ColorPicker("Цвет фона", () -> true)
            .defaultValue(0x00000000)
            .register(this);

    public final Slider bgAlpha = new Slider("Прозрачность фона", () -> true)
            .set(0f, 255f, 1f)
            .applyDefault(180f)
            .register(this);

    public final Slider borderRadius = new Slider("Скругление углов", () -> true)
            .set(0f, 20f, 0.5f)
            .applyDefault(4f)
            .register(this);

    public final CheckBox glass = new CheckBox("Стекло", () -> true)
            .defaultValue(false)
            .register(this);

    public final CompactAnimation alphaAnimation = new CompactAnimation(Easing.EASE_OUT_CUBIC, 400);
    final CopyOnWriteArrayList<HeadParticle> headParticles = new CopyOnWriteArrayList<>();
    private final CompactAnimation copyHoverAnimation = new CompactAnimation(Easing.EASE_OUT_CUBIC, 200);

    private static final String COPY_LABEL = "Скопировать";

    private float copyAreaX;
    private float copyAreaY;
    private float copyAreaW;
    private float copyAreaH;
    private String copyRealName = "";
    private boolean canCopyName = false;

    public TargetHud() {
        this.draggableHandler = addDraggable("Target Hud", 418, 359);
        this.draggableHandler.setActiveCondition(() -> {
            return dev.client.modules.core.hud.HudModuleHelper.isTargetHudEnabled();
        });
        this.draggableHandler.setClickHandler((mouseX, mouseY, button, self) -> {
            if (button != 0) return false;
            if (!canCopyName) return false;
            if (!mc.inGameHud.getChatHud().isChatFocused()) return false;
            if (!MouseClick.isClick(mouseX, mouseY, copyAreaX, copyAreaY, copyAreaW, copyAreaH)) return false;
            if (copyRealName.isEmpty()) return false;
            if (mc.keyboard != null) {
                mc.keyboard.setClipboard(copyRealName);
            }
            return true;
        });
        this.draggableHandler.setSettingsPanel(new DraggableSettingsPanel(draggableHandler, this));
    }

    float width;
    float height;

    float animationValue;
    float headHitAnim = 0f;

    float hpTextValue = 0f;
    boolean hpTextInit = false;
    
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0L;
    private static final long TARGET_TIMEOUT_MS = 5000L;

    @Override
    public void onPlayerAttack(PlayerAttackEvent event) {
        if (!(event.getEntity() instanceof LivingEntity attacked)) return;
        if (attacked instanceof ArmorStandEntity) return;

        currentTarget = attacked;
        lastAttackTime = System.currentTimeMillis();

        if (!particleTargetHud.getEnabled()) return;

        final float headSize = 32f;
        final float panelH = 40f;
        Vec3d center = getHeadCenterForMode(panelH, headSize);

        int particleCount = (int) particleValue.getValue().floatValue();
        for (int i = 0; i < particleCount; ++i) {
            headParticles.add(new HeadParticle(center));
        }
    }

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        LivingEntity target = resolveTarget();
        boolean chatOpen = mc.inGameHud.getChatHud().isChatFocused();
        boolean shouldRender = target != null || chatOpen;

        alphaAnimation.run(shouldRender ? 1.0 : 0.0);
        alphaAnimation.update();
        float a = (float) alphaAnimation.getValue();
        if (a <= 0f) {
            resetCopyInteraction();
            return;
        }

        if (target == null) target = mc.player;
        if (target == null) {
            resetCopyInteraction();
            return;
        }

        int mouseX = 0;
        int mouseY = 0;
        Window window = mc.getWindow();
        if (window != null && window.getWidth() > 0 && window.getHeight() > 0) {
            mouseX = (int) (mc.mouse.getX() * window.getScaledWidth() / (double) window.getWidth());
            mouseY = (int) (mc.mouse.getY() * window.getScaledHeight() / (double) window.getHeight());
        }

        float health = PlayerHelper.getHealth(target);
        float maxHealth = Math.max(1f, target.getMaxHealth());
        animationValue = MathHelper.lerp(0.1f, animationValue, MathHelper.clamp(health / maxHealth, 0f, 1f));

        if (!hpTextInit) {
            hpTextValue = health;
            hpTextInit = true;
        }
        float dtHp = Math.max(0.001f, mc.getRenderTickCounter().getLastFrameDuration());
        float speed = 8.0f * dtHp;
        hpTextValue = MathHelper.lerp(MathHelper.clamp(speed, 0f, 1f), hpTextValue, health);

        renderTX(event, target, a, chatOpen, mouseX, mouseY);
        renderParticles(event, a);
    }

    private void renderTX(RenderEvent.Draw2D event, LivingEntity target, float a, boolean chatOpen, int mouseX, int mouseY) {
        float x = draggableHandler.getX();
        float y = draggableHandler.getY();

        float panelW = 110f;
        float panelH = 39f;
        this.width = panelW;
        this.height = panelH;
        draggableHandler.setWidth(panelW);
        draggableHandler.setHeight(panelH);

        float headSize = 32f;
        float headSpacing = 3f;
        float rightPad = 5f;

        float headX = x + headSpacing;
        float headY = y + (panelH - headSize) * 0.5f;
        float dt = Math.max(0.001f, mc.getRenderTickCounter().getLastFrameDuration());

        renderPanelBackground(event, x, y, panelW, panelH, a, headX, headY, headSize);

        ScissorUtil.enable(x, y, panelW - 2, panelH);
        renderHead(event, target, headX, headY, headSize, 5, a, dt);
        renderBarsAndText(event, target, x, y, panelW, panelH, a, hpTextValue, headX, headSize, rightPad, chatOpen, mouseX, mouseY);
        ScissorUtil.disable();

        renderArmorRow(event, target, x, y, panelW, a);
    }

    private void renderHead(RenderEvent.Draw2D event, LivingEntity target, float headX, float headY, float headSize, float radius, float a, float dt) {
        if (target.hurtTime > 0) headHitAnim = 1f;
        else headHitAnim = Math.max(0f, headHitAnim - 0.28f * dt);

        float e = 1f - (float) Math.cos(Math.min(1f, headHitAnim) * (float) Math.PI * 0.5f);
        int baseColor = 0xFFFFFFFF;
        int hitColor = 0xFFFF0000;
        int mixed = ColorUtils.interpolate(hitColor, baseColor, e);
        int mixedWithAlpha = ColorUtils.setAlpha(mixed, (int) (255 * a));

        if (target instanceof PlayerEntity) {
            DrawUtil.renderHeadTexture(
                    event.getContext().getMatrices(),
                    headX, headY,
                    headSize, headSize,
                    radius,
                    0.125f, 0.125f,
                    0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    new Color(mixedWithAlpha, true)
            );
        } else {
            var vcp = mc.getBufferBuilders().getEntityVertexConsumers();
            var vc = vcp.getBuffer(IMAGE);

            RenderSystem.setShaderTexture(0, ClientTexture.of("images/world/entity.png").getGlId());

            drawImageQuad2(
                    vc,
                    event.getContext().getMatrices().peek().getPositionMatrix(),
                    headX + headSize / 2f + 2,
                    headY + headSize / 2f,
                    0f,
                    12f,
                    mixedWithAlpha
            );

            vcp.drawCurrentLayer();
        }
    }

    private void renderBarsAndText(RenderEvent.Draw2D event, LivingEntity target, float x, float y, float panelW, float panelH, float a, float health, float headX, float headSize, float rightPad, boolean chatOpen, int mouseX, int mouseY) {
        float textLeftX = headX + headSize + 2;
        float nameX = textLeftX + 1f;
        float nameY = y + 5f;

        renderName(event, target, nameX, nameY, 8f, a, chatOpen, mouseX, mouseY);

        float barY = y + panelH - 12;
        float barW = Math.max(8f, panelW - (textLeftX - x) - rightPad);
        float barH = 7.5f;
        float filledW = barW * animationValue;

        ClientApi.rectangle()
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(getBossBarColor(false, a, target)))
                .size(new SizeState(barW, barH + 0.5f))
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), textLeftX, barY);

        ClientApi.rectangle()
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(getBossBarColor(true, a, target)))
                .size(new SizeState(filledW, barH))
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), textLeftX, barY);

        String hpValue = String.format(Locale.ROOT, "%.1f", health);
        String hpText = "HP: " + hpValue;

        float hpSize = 6.5f;
        float hpY = nameY + 12.5f;

        ClientApi.text()
                .font(ClientApi.inter())
                .text(hpText)
                .color(ColorUtils.setAlpha(-1, (int) (255 * a)))
                .size(hpSize)
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameX, hpY);
    }

    private void renderName(RenderEvent.Draw2D event, LivingEntity target, float nameX, float nameY, float size, float a, boolean chatOpen, int mouseX, int mouseY) {
        String displayName = NameProtect.getDisplayName(target);
        String realName = target instanceof PlayerEntity player
                ? player.getGameProfile().getName()
                : target.getName().getString();

        float displayWidth = ClientApi.inter().getWidth(displayName, size);
        float copyWidth = ClientApi.inter().getWidth(COPY_LABEL, size);
        float areaW = Math.max(Math.max(displayWidth, copyWidth), 1f);
        float areaH = ClientApi.inter().getHeight(COPY_LABEL, size);

        copyAreaX = nameX;
        copyAreaY = nameY;
        copyAreaW = areaW;
        copyAreaH = areaH;

        boolean eligible = target instanceof PlayerEntity;
        copyRealName = eligible ? realName : "";
        canCopyName = eligible && !realName.isEmpty();

        boolean hovering = chatOpen && canCopyName && MouseClick.isClick(mouseX, mouseY, copyAreaX, copyAreaY, copyAreaW, copyAreaH);

        copyHoverAnimation.run(hovering ? 1.0 : 0.0);
        copyHoverAnimation.update();

        float progress = (float) copyHoverAnimation.getValue();
        int themeColor = Interface.INSTANCE.getMainColor();
        int displayAlpha = MathHelper.clamp((int) Math.round(255f * a * (1f - progress)), 0, 255);
        int copyAlpha = MathHelper.clamp((int) Math.round(255f * a * progress), 0, 255);

        if (displayAlpha > 0) {
            // Градиентный текст для имени
            int[] gradientColors = HelperElements.createThemeGradient(themeColor);
            float currentX = nameX;
            for (int i = 0; i < displayName.length(); i++) {
                String character = String.valueOf(displayName.charAt(i));
                float charWidth = ClientApi.inter().getWidth(character, size);
                int color = ColorUtils.gradient(30, i * 5, gradientColors);
                color = ColorUtils.setAlpha(color, displayAlpha);
                
                ClientApi.text()
                        .font(ClientApi.inter())
                        .text(character)
                        .color(color)
                        .size(size)
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), currentX, nameY);
                
                currentX += charWidth;
            }
        }

        if (copyAlpha > 0) {
            ClientApi.text()
                    .font(ClientApi.inter())
                    .text(COPY_LABEL)
                    .color(ColorUtils.setAlpha(-1, copyAlpha))
                    .size(size)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameX, nameY);
        }
    }

    private void resetCopyInteraction() {
        canCopyName = false;
        copyRealName = "";
        copyAreaX = copyAreaY = copyAreaW = copyAreaH = 0f;
        copyHoverAnimation.run(0.0);
        copyHoverAnimation.update();
    }

    private void renderArmorRow(RenderEvent.Draw2D event, LivingEntity target, float x, float y, float panelW, float a) {
        if (!armorStack.getEnabled()) return;

        ArrayList<ItemStack> armor = new ArrayList<>(4);
        armor.add(target.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD));
        armor.add(target.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST));
        armor.add(target.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS));
        armor.add(target.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET));
        armor.removeIf(ItemStack::isEmpty);

        if (armor.isEmpty()) return;

        float baseScale = 0.66f;
        float slot = 17f * baseScale;
        float spacing = 1f;
        float pad = 4f;
        float rowH = 14f;
        float gap = 1f;
        float radius = 5f;

        float contentW = armor.size() * slot + Math.max(0, armor.size() - 1) * spacing;
        float bgW = contentW + pad * 2f;

        float bx = x + (panelW - bgW) * 0.5f;
        float by = y - gap - rowH - 3;

        // Рисуем фон в стиле основной панели
        HelperElements.rectElements(event.getContext(), bx, by, bgW, rowH, a * 0.85f);

        float ix = bx + pad;
        float iy = by + (rowH - slot) * 0.5f + 0.5f;

        double animValue = alphaAnimation.getValue();
        float pulse = (float) (0.95f + 0.05f * Math.sin(animValue * Math.PI));
        float itemScale = baseScale * (float) animValue * pulse;

        for (ItemStack st : armor) {
            var ms = event.getContext().getMatrices();
            ms.push();
            ms.translate(ix + 8f * baseScale, iy + 8f * baseScale, 0);
            ms.scale(itemScale, itemScale, itemScale);
            ms.translate(-8f, -8f, 0);
            event.getContext().drawItem(st, 0, 0);
            ms.pop();

            ix += slot + spacing;
        }
    }

    private LivingEntity resolveTarget() {
        long now = System.currentTimeMillis();
        
        if (currentTarget != null) {
            if (now - lastAttackTime > TARGET_TIMEOUT_MS || !currentTarget.isAlive() || currentTarget.isRemoved()) {
                currentTarget = null;
                lastAttackTime = 0L;
            }
        }

        LivingEntity rayTraced = null;
        if (rayTrace.getEnabled()) {
            HitResult crosshairTarget = mc.crosshairTarget;
            if (crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerEntity playerEntity) {
                rayTraced = playerEntity;
            }
        }

        LivingEntity target = currentTarget;
        if (target == null && rayTraced != null) {
            target = rayTraced;
        }
        if (target instanceof ArmorStandEntity) {
            target = null;
        }
        return target;
    }

    private void renderParticles(RenderEvent.Draw2D event, float a) {
        if (!particleTargetHud.getEnabled()) return;

        final long lifetimeMs = 2000L;
        long now = System.currentTimeMillis();
        headParticles.removeIf(p -> now - p.time > lifetimeMs);

        int clientColor = Interface.INSTANCE.getMainColor(4);

        for (HeadParticle p : headParticles) {
            p.update();
            float lifeK = 1.0f - (float) (now - p.time) / (float) lifetimeMs;
            if (lifeK < 0f) lifeK = 0f;

            float alphaEase = p.alpha * lifeK * lifeK * a;
            int col = ColorUtils.setAlpha(clientColor, (int) (255.0f * alphaEase));

            DrawUtil.drawFilledCircle(
                    event.getContext().getMatrices(),
                    (float) p.pos.x, (float) p.pos.y,
                    1.5f,
                    col
            );
        }
    }

    private Vec3d getHeadCenterForMode(float panelH, float headSize) {
        final float headSpacing = 5f;
        final float cx = draggableHandler.getX() + headSpacing + headSize * 0.5f;
        final float cy = draggableHandler.getY() + (panelH - headSize) * 0.5f + headSize * 0.5f;
        return new Vec3d(cx, cy, 0.0);
    }

    private int getBossBarColor(boolean filled, float a, LivingEntity target) {
        int base;
        if (colorBar.isSelected("Состояние здоровья")) {
            base = PlayerHelper.getHealthColorHSV(target);
        } else {
            base = Interface.INSTANCE.getMainColor();
        }

        int alpha = filled ? (int) (255 * a) : (int) (50 * a);
        alpha = Math.max(0, Math.min(255, alpha));
        return ColorUtils.setAlpha(base, alpha);
    }

    private void renderPanelBackground(RenderEvent.Draw2D event, float x, float y, float panelW, float panelH, float showProgress,
                                       float headX, float headY, float headSize) {
        float show = MathHelper.clamp(showProgress, 0f, 1f);
        Matrix4f matrix = event.getContext().getMatrices().peek().getPositionMatrix();

        float radius = borderRadius.getValue();
        int bgColorInt = bgColor.getColorRGBA();
        boolean glassEnabled = glass.getEnabled();
        int bgA = glassEnabled ? 0 : bgAlpha.getValue().intValue();

        if (glassEnabled) {
            float outlineExpand = 2f;
            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius + outlineExpand))
                    .size(new SizeState(panelW + outlineExpand * 2, panelH + outlineExpand * 2))
                    .alpha(show * 0.6f)
                    .build()
                    .render(matrix, x - outlineExpand, y - outlineExpand);

            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius))
                    .size(new SizeState(panelW, panelH))
                    .alpha(show)
                    .build()
                    .render(matrix, x, y);

            GlassShadow.render(matrix, x, y, panelW, panelH, radius, show);
        }

        if (bgA > 0) {
            int normalizedAlpha = (int) Math.min(1f, bgA / 255f * 255f * show);
            int outlineColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), normalizedAlpha);

            ClientApi.rectangle()
                    .size(new SizeState(panelW, panelH))
                    .color(new QuadColorState(outlineColor))
                    .radius(new QuadRadiusState(radius))
                    .build()
                    .render(matrix, x, y);

            float innerPad = 1f;
            int fillColor = ColorUtils.setAlpha(bgColorInt, (int) (bgA * show));

            ClientApi.rectangle()
                    .size(new SizeState(panelW - innerPad * 2, panelH - innerPad * 2))
                    .color(new QuadColorState(fillColor))
                    .radius(new QuadRadiusState(Math.max(0f, radius - innerPad)))
                    .build()
                    .render(matrix, x + innerPad, y + innerPad);
        } else {
            HelperElements.rectWatermarkStyle(event.getContext(), x, y, panelW, panelH, show);
        }
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }

    @Override
    public float getPanelAlpha() {
        return MathHelper.clamp((float) alphaAnimation.getValue(), 0f, 1f);
    }

    public static class HeadParticle {
        public Vec3d pos;
        public final Vec3d end;
        public final long time;
        public float alpha;

        public HeadParticle(Vec3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();

            double radius;
            double heightOffset;
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);

            radius = ThreadLocalRandom.current().nextDouble(60.0, 100.0);
            heightOffset = ThreadLocalRandom.current().nextDouble(-50.0, 50.0);

            double offsetX = radius * Math.cos(angle);
            double offsetY = radius * Math.sin(angle);
            this.end = pos.add(offsetX, offsetY, heightOffset);
        }

        public void update() {
            this.alpha = MathUtil.linear(this.alpha, 1.0F, 10.0F);
            this.pos = MathUtil.fast(this.pos, this.end, 0.5F);
        }
    }
}