package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.events.core.game.TickEvent;
import dev.client.api.nullcry.events.core.input.MotionEvent;
import dev.client.api.nullcry.events.core.network.PacketEvent;
import dev.client.api.nullcry.events.core.player.PlayerAttackEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.entity.MovingUtil;
import dev.client.api.nullcry.helper.math.MathUtil;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.*;
import dev.client.api.nullcry.render.ClientTexture;
import dev.client.api.nullcry.render.ColorUtilsExcellent;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Particles extends Module {

    public Particles() {
        super("Particles", ModuleCategory.Visuals, "Рендерит красивые частицы в мире");
    }

    SelectElements type = new SelectElements("Спавнить при", () -> true)
            .set("Бездействии", "Движении", "Ударе", "Броске", "Потере тотема")
            .defaultValue("Бездействии", "Ударе")
            .register(this);

    Collection idleSettings = new Collection("Настройки бездействия", () -> type.isSelected("Бездействии")).register(this);
    Slider countLevel = (Slider) new Slider("Количество", () -> true).set(1, 25, 1).defaultValue(5).collection(idleSettings);
    Slider range = (Slider) new Slider("Дистанция", () -> true).set(4, 32, 1).defaultValue(16).collection(idleSettings);
    Slider strengthLevel = (Slider) new Slider("Сила движения", () -> true).set(1, 25, 1).defaultValue(5).collection(idleSettings);
    CheckBox onlyMove = (CheckBox) new CheckBox("Только в движении", () -> true).defaultValue(false).collection(idleSettings);
    CheckBox ground = (CheckBox) new CheckBox("Спавнить на земле", () -> true).defaultValue(false).collection(idleSettings);

    Collection moveSettings = new Collection("Настройки движения", () -> type.isSelected("Движении")).register(this);
    Slider countMove = (Slider) new Slider("Количество", () -> true).set(1, 25, 1).defaultValue(2).collection(moveSettings);
    Slider strengthParticle = (Slider) new Slider("Сила движения", () -> true).set(1, 25, 1).defaultValue(5).collection(moveSettings);

    Collection attackSettings = new Collection("Настройки атаки", () -> type.isSelected("Ударе")).register(this);
    Slider countAttack = (Slider) new Slider("Количество", () -> true).set(1, 25, 1).defaultValue(5).collection(attackSettings);

    Collection throwSettings = new Collection("Настройки броска", () -> type.isSelected("Броске")).register(this);
    SelectElements typeFollow = (SelectElements) new SelectElements("Детектить бросок", () -> true)
            .set("Эндер-жемчуга", "Трезубца", "Снежка")
            .defaultValue("Эндер-жемчуга", "Трезубца")
            .collection(throwSettings);
    Slider countFollow = (Slider) new Slider("Количество", () -> true).set(1, 10, 1).defaultValue(2).collection(throwSettings);

    Collection totemSettings = new Collection("Настройки тотема", () -> type.isSelected("Потере тотема")).register(this);
    Slider countTotem = (Slider) new Slider("Количество", () -> true).set(1, 25, 1).defaultValue(5).collection(totemSettings);

    Slider duration = new Slider("Время жизни", () -> true).set(500, 5000, 1).defaultValue(2000).register(this);
    CheckBox physic = new CheckBox("Физика", () -> true).defaultValue(true).register(this);

    ModeElement particleMode = new ModeElement("Тип частиц", () -> true)
            .set("Heart", "Crown", "Dollar", "Skull", "Star", "Bloom", "Random")
            .defaultValue("Heart")
            .register(this);

    ModeElement colorMode = new ModeElement("Режим цвета", () -> true)
            .set("Клиентский", "Рандом", "Свой")
            .defaultValue("Клиентский")
            .register(this);
    ColorPicker customColor = new ColorPicker("Цвет", () -> colorMode.isSelected("Свой"))
            .set(-1)
            .defaultValue(-1)
            .register(this);

    private final ArrayList<ParticleBase> particles = new ArrayList<>();
    private static final int MAX_PARTICLES = 50;

    {
        // Регистрируем настройки в коллекции
        countLevel.collection(idleSettings);
        range.collection(idleSettings);
        strengthLevel.collection(idleSettings);
        onlyMove.collection(idleSettings);
        ground.collection(idleSettings);
        
        countMove.collection(moveSettings);
        strengthParticle.collection(moveSettings);
        
        countAttack.collection(attackSettings);
        
        typeFollow.collection(throwSettings);
        countFollow.collection(throwSettings);
        
        countTotem.collection(totemSettings);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity p)) return;

        if (!type.isSelected("Потере тотема")) return;
        for (int j = 0; j < countTotem.getValue(); j++) {
            float posX = (float) p.getX();
            float posY = (float) (p.getY() + 1.5F);
            float posZ = (float) p.getZ();

            if (!isSpawnClear(posX, posY, posZ)) {
                continue;
            }

            float radius = 0.3F;
            float motionX = MathUtil.random(-radius, radius);
            float motionZ = MathUtil.random(-radius, radius);

            float motionYPhysOn = -(float) MathUtil.random(0.10f, 0.30f) * 0.6f;
            float motionYPhysOff = -(float) MathUtil.random(0.10f, 0.30f);

            Color col = getColors();
            ParticleType typeToUse = particleMode.isSelected("Random")
                    ? ParticleType.getRandom()
                    : getParticleType();

            if (physic.getEnabled()) {
                particles.add(new ParticleBase(
                        posX, posY, posZ,
                        motionX, motionYPhysOn, motionZ,
                        col, typeToUse,
                        lifeFromSlider()
                ));
            } else {
                particles.add(new ParticleBase(
                        posX, posY, posZ,
                        motionX, motionYPhysOff, motionZ,
                        col, typeToUse,
                        true,
                        lifeFromSlider()
                ).withSkyTuning(
                        0.0f,
                        0.0020f,
                        0.992f,
                        0.0012f
                ));
            }
        }
    }

    @Subscribe
    public void onPlayerAttack(PlayerAttackEvent event) {
        if (!type.isSelected("Ударе")) return;

        float tAtk = norm(strengthParticle.getValue());
        float sideMin = lerp(0.02f, 0.06f, tAtk);
        float sideMax = lerp(0.05f, 0.14f, tAtk);
        float upMin = lerp(0.04f, 0.10f, tAtk);
        float upMax = lerp(0.10f, 0.22f, tAtk);

        for (int j = 0; j < countAttack.getValue(); j++) {
            float posX = (float) event.getEntity().getX();
            float posY = (float) (event.getEntity().getY() + 1.5F);
            float posZ = (float) event.getEntity().getZ();

            if (!isSpawnClear(posX, posY, posZ)) {
                continue;
            }

            float angle = MathUtil.random(0f, (float) (Math.PI * 2));
            float side = MathUtil.random(sideMin, sideMax);
            float motionX = (float) Math.cos(angle) * side;
            float motionZ = (float) Math.sin(angle) * side;

            float motionYPhysOn = MathUtil.random(upMin, upMax) * (mc.player.isOnGround() ? 1f : 0.6f);
            float motionYPhysOff = -MathUtil.random(upMin * 0.5f, upMax * 0.8f);

            Color col = getColors();
            ParticleType typeToUse = particleMode.isSelected("Random") ? ParticleType.getRandom() : getParticleType();

            if (physic.getEnabled()) {
                particles.add(new ParticleBase(
                        posX, posY, posZ,
                        motionX, motionYPhysOn, motionZ,
                        col, typeToUse,
                        lifeFromSlider()
                ));
            } else {
                particles.add(new ParticleBase(
                        posX, posY, posZ,
                        motionX, motionYPhysOff, motionZ,
                        col, typeToUse,
                        true,
                        lifeFromSlider()
                ).withSkyTuning(
                        0.0f,
                        0.0018f,
                        0.992f,
                        0.0010f
                ));
            }
        }
    }

    @Subscribe
    public void onMotion(MotionEvent event) {
        if (mc == null || mc.world == null || mc.player == null) return;

        Vec3d vel = mc.player.getVelocity();
        double speedSq = vel.x * vel.x + vel.z * vel.z;
        boolean onGround = mc.player.isOnGround();

        boolean randomMode = particleMode.isSelected("Random");
        ParticleType chosen = getParticleType();

        Box bb = mc.player.getBoundingBox();
        float feetX = (float) ((bb.minX + bb.maxX) * 0.5);
        float feetZ = (float) ((bb.minZ + bb.maxZ) * 0.5);
        float feetY = (float) bb.minY;

        if (type.isSelected("Движении") && speedSq >= 1.0E-4) {
            int maxPerTick = Math.max(1, (int) Math.ceil(countMove.getValue() / 8.0));
            int spawn = Math.min(maxPerTick, 1 + (int) Math.ceil(speedSq * 80.0));

            double len = Math.sqrt(Math.max(1.0E-8, speedSq));
            float dirX = (float) (vel.x / len);
            float dirZ = (float) (vel.z / len);

            float tMove = norm(strengthParticle.getValue());
            float sideBase = lerp(0.01f, 0.10f, tMove);
            float back = lerp(0.004f, 0.02f, tMove);
            float upMin = lerp(0.02f, 0.06f, tMove);
            float upMax = lerp(0.06f, 0.16f, tMove);

            float spawnGap = 0.23f;

            for (int i = 0; i < spawn; i++) {
                float baseX = feetX - dirX * 0.10f;
                float baseZ = feetZ - dirZ * 0.10f;

                float baseY = feetY + (onGround ? spawnGap : spawnGap * 0.8f) + 0.20f + MathUtil.random(0.00f, 0.02f);

                float angle = MathUtil.random(0f, (float) (Math.PI * 2));
                float speedFactor = (float) Math.min(2.0, 1.0 + speedSq * 2.0);
                float side = sideBase * speedFactor;

                float randX = (float) Math.cos(angle) * side;
                float randZ = (float) Math.sin(angle) * side;

                float motionX = randX - dirX * back + MathUtil.random(-side * 0.1f, side * 0.1f);
                float motionZ = randZ - dirZ * back + MathUtil.random(-side * 0.1f, side * 0.1f);

                float motionYPhysOn = MathUtil.random(upMin, upMax);
                float motionYPhysOff = -MathUtil.random(0.0025f, 0.0060f);

                Color col = getColors();
                ParticleType typeToUse = randomMode ? ParticleType.getRandom() : chosen;

                if (!isSpawnClear(baseX, baseY, baseZ)) {
                    continue;
                }

                ParticleBase p;
                if (physic.getEnabled()) {
                    p = new ParticleBase(baseX, baseY, baseZ, motionX, motionYPhysOn, motionZ, col, typeToUse, lifeFromSlider());
                } else {
                    p = new ParticleBase(baseX, baseY, baseZ, motionX, motionYPhysOff, motionZ, col, typeToUse, true, lifeFromSlider())
                            .withSkyTuning(
                                    0.0f,
                                    0.0009f,
                                    0.994f,
                                    0.0010f
                            );
                }
                particles.add(p);
            }
        }

        if (type.isSelected("Бездействии")) {
            if (onlyMove.getEnabled() && !MovingUtil.isPlayerMoving()) {
                return;
            }

            int spawn = countLevel.getValue().intValue();
            boolean randomModeIdle = particleMode.isSelected("Random");
            ParticleType fixedType = randomModeIdle ? null : getParticleType();

            float tIdle = norm(strengthLevel.getValue());
            float sideSpeed = lerp(0.002f, 0.015f, tIdle);
            float upMin = lerp(0.001f, 0.006f, tIdle);
            float upMax = lerp(0.005f, 0.018f, tIdle);

            for (int i = 0; i < spawn; i++) {
                double spread = Math.max(2.0, range.getValue());
                float baseX = (float) (mc.player.getX() + RandomUtil.randomValue(-spread, spread));
                float baseZ = (float) (mc.player.getZ() + RandomUtil.randomValue(-spread, spread));

                int delay = (int) (Math.random() * 40);
                int life = (int) (lifeFromSlider() * RandomUtil.randomValue(0.70f, 1.60f));

                if (ground.getEnabled()) {
                    BlockPos top = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(baseX, 0, baseZ));
                    float groundY = top.getY();
                    float baseY = groundY + 0.02f + RandomUtil.randomValue(0.00f, 0.02f);

                    if (!isSpawnClear(baseX, baseY, baseZ)) {
                        continue;
                    }

                    float motionX = RandomUtil.randomValue(-sideSpeed, sideSpeed);
                    float motionZ = RandomUtil.randomValue(-sideSpeed, sideSpeed);
                    float motionY = RandomUtil.randomValue(upMin * 0.6f, upMax * 0.8f);

                    Color col = getColors();
                    ParticleType tp = (fixedType != null) ? fixedType : ParticleType.getRandom();

                    ParticleBase p = new ParticleBase(
                            baseX, baseY, baseZ,
                            motionX, motionY, motionZ,
                            col, tp,
                            true,
                            life
                    ).withSpawnDelayAndPulse(
                            delay,
                            10f + RandomUtil.randomValue(-2f, 2f),
                            RandomUtil.randomValue(0f, (float) (Math.PI * 2)),
                            0.85f, 1.10f,
                            0.25f
                    );

                    if (physic.getEnabled()) {
                        p.withSkyTuning(0.0009f, 0.0004f, 0.996f, 0.0006f);
                    } else {
                        p.withSkyTuning(0f, 0f, 0.992f, 0.0015f);
                    }
                    particles.add(p);

                } else {
                    float skyMin = 5.0f, skyMax = 15.0f;
                    float baseY = (float) (mc.player.getY() + RandomUtil.randomValue(skyMin, skyMax));

                    if (!isSpawnClear(baseX, baseY, baseZ)) {
                        continue;
                    }

                    float motionX = RandomUtil.randomValue(-sideSpeed, sideSpeed);
                    float motionZ = RandomUtil.randomValue(-sideSpeed, sideSpeed);
                    float motionY = RandomUtil.randomValue(-upMax * 0.6f, -upMin * 0.8f);

                    Color col = getColors();
                    ParticleType tp = (fixedType != null) ? fixedType : ParticleType.getRandom();

                    BlockPos top = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(baseX, 0, baseZ));
                    float groundY = top.getY();
                    float safeMargin = 0.8f;
                    float maxDrop = RandomUtil.randomValue(0.8f, 3.0f);
                    boolean allowNearGround = Math.random() < 0.05;
                    float targetFloor = allowNearGround ? (groundY + 0.1f) : (groundY + safeMargin);
                    float stopY = Math.max(targetFloor, baseY - maxDrop);

                    ParticleBase p = new ParticleBase(
                            baseX, baseY, baseZ,
                            motionX, motionY, motionZ,
                            col, tp,
                            stopY,
                            true,
                            life
                    ).withSpawnDelayAndPulse(
                            delay,
                            10f + RandomUtil.randomValue(-2f, 2f),
                            RandomUtil.randomValue(0f, (float) (Math.PI * 2)),
                            0.85f, 1.10f,
                            0.25f
                    );

                    if (physic.getEnabled()) {
                        p.withSkyTuning(0.0003f, 0.0007f, 0.996f, 0.0006f);
                    } else {
                        p.withSkyTuning(0f, 0f, 0.992f, 0.0015f);
                    }
                    particles.add(p);
                }
            }
        }
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (mc == null || mc.player == null || mc.world == null) {
            particles.clear();
            return;
        }

        if (particles.size() > MAX_PARTICLES) {
            particles.subList(0, particles.size() - MAX_PARTICLES).clear();
        }

        if (type.isSelected("Броске")) {
            boolean randomMode = particleMode.isSelected("Random");
            ParticleType chosen = getParticleType();

            int processed = 0, maxEntitiesPerTick = 12;

            for (var ent : mc.world.getEntities()) {
                Vec3d iv = ent.getVelocity();
                double ivSq = iv.lengthSquared();
                if (!isSelectedProjectile(ent)) continue;
                if (!isProjectileFlying(ent)) continue;

                double len = Math.sqrt(ivSq);
                float dirX = (float) (iv.x / len);
                float dirY = (float) (iv.y / len);
                float dirZ = (float) (iv.z / len);

                float lag = (float) MathHelper.clamp(len * 0.08, 0.04, 0.18);
                float ex = (float) (ent.getX() - dirX * lag);
                float ey = (float) (ent.getY() - dirY * lag);
                float ez = (float) (ent.getZ() - dirZ * lag);

                float ux = 0, uy = 1, uz = 0;
                if (Math.abs(dirY) > 0.95f) {
                    ux = 1;
                    uy = 0;
                    uz = 0;
                }
                float rx = dirY * uz - dirZ * uy, ry = dirZ * ux - dirX * uz, rz = dirX * uy - dirY * ux;
                float rlen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
                if (rlen < 1e-6f) continue;
                rx /= rlen;
                ry /= rlen;
                rz /= rlen;
                float qx = dirY * rz - dirZ * ry, qy = dirZ * rx - dirX * rz, qz = dirX * ry - dirY * rx;
                float qlen = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
                if (qlen < 1e-6f) continue;
                qx /= qlen;
                qy /= qlen;
                qz /= qlen;

                float t = MathHelper.clamp((float) len, 0f, 2.0f);
                float sideRadius = MathHelper.lerp(t, 0.06f, 0.24f);
                float radialPush = sideRadius * (1.10f + MathUtil.random(0.2f, 0.8f));
                float carryFwd = MathHelper.lerp(t, 0.02f, 0.085f);
                float upMin = 0.010f, upMax = 0.030f;

                float speedLift = (float) len * 0.02f;

                int perEntity = Math.min(Math.max(1, countFollow.getValue().intValue()), 8);

                for (int j = 0; j < perEntity; j++) {
                    float theta = MathUtil.random(0f, (float) (Math.PI * 2));
                    float r = (float) Math.sqrt(MathUtil.random(0f, 1f)) * sideRadius;

                    float cx = (float) Math.cos(theta), sx = (float) Math.sin(theta);

                    float offX = rx * cx * r + qx * sx * r;
                    float offY = ry * cx * r + qy * sx * r;
                    float offZ = rz * cx * r + qz * sx * r;

                    float baseX = ex + offX;
                    float baseY = ey + offY;
                    float baseZ = ez + offZ;

                    if (!isSpawnClear(baseX, baseY, baseZ)) {
                        continue;
                    }

                    float motionX = dirX * carryFwd + offX * radialPush;
                    float motionY = offY * (radialPush * 0.6f) + MathUtil.random(upMin, upMax) + speedLift;
                    float motionZ = dirZ * carryFwd + offZ * radialPush;

                    Color col = getColors();
                    ParticleType typeToUse = randomMode ? ParticleType.getRandom() : chosen;

                    particles.add(new ParticleBase(baseX, baseY, baseZ, motionX, motionY, motionZ, col, typeToUse, true, lifeFromSlider()));
                }

                if (++processed >= maxEntitiesPerTick) break;
            }
        }

        particles.removeIf(ParticleBase::tick);
    }

    @Subscribe
    public void onRender3D(RenderEvent.Draw3D event) {
        if (particles.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        boolean isRandom = particleMode.isSelected("Random");

        if (!isRandom) {
            ParticleType chosen = getParticleType();
            RenderSystem.setShaderTexture(0, chosen.texture);
            BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            int rendered = 0;
            for (ParticleBase p : particles) {
                if (p.render(bb)) rendered++;
            }
            if (rendered > 0) {
                BufferRenderer.drawWithGlobalProgram(bb.end());
            } else {
            }
        } else {
            java.util.Map<Identifier, java.util.List<ParticleBase>> byTex = new java.util.HashMap<>();
            for (ParticleBase p : particles) {
                byTex.computeIfAbsent(p.type.texture(), k -> new java.util.ArrayList<>()).add(p);
            }
            for (var entry : byTex.entrySet()) {
                RenderSystem.setShaderTexture(0, entry.getKey());
                BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                int rendered = 0;
                for (ParticleBase p : entry.getValue()) {
                    if (p.render(bb)) rendered++;
                }
                if (rendered > 0) {
                    BufferRenderer.drawWithGlobalProgram(bb.end());
                } else {
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    public static class ParticleBase {
        protected final ParticleType type;
        protected float prevposX, prevposY, prevposZ, posX, posY, posZ, motionX, motionY, motionZ;
        protected int age;
        protected final int maxAge;
        protected Color first, second;

        private boolean hasStopY = false;
        private float stopY;

        private boolean skyMode = false;
        private float windJitter = 0.003f;
        private float skyDamping = 0.996f;
        private float skyGravity = 0.0f;

        private final boolean constantAlpha;
        private final int alphaMaxAge;

        private boolean useInOutAlpha = false;
        private int fadeInTicks = 0;
        private int fadeOutTicks = 0;
        private boolean landed = false;
        private float skyLift = 0.0f;

        private int spawnDelayTicks = 0;
        private boolean pulse = false;
        private float pulseSpeed = 0f;
        private float pulsePhase = 0f;
        private float pulseMinScale = 1.0f;
        private float pulseMaxScale = 1.0f;
        private float pulseAlphaAmp = 0.0f;

        private int groundContactTicks = 0;
        private int stuckTicks = 0;

        public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, Color color, ParticleType type, int lifeTicks) {
            this.type = type;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevposX = posX;
            this.prevposY = posY;
            this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;

            this.age = Math.max(1, lifeTicks);
            this.maxAge = this.age;
            this.first = color;
            this.second = color;

            this.constantAlpha = false;
            this.alphaMaxAge = this.maxAge;
            this.skyMode = false;

            this.useInOutAlpha = true;
            this.fadeInTicks = Math.max(10, maxAge / 6);
            this.fadeOutTicks = Math.max(16, maxAge / 5);
        }

        public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, Color color, ParticleType type, float stopY, boolean skyMode, int lifeTicks) {
            this.type = type;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevposX = posX;
            this.prevposY = posY;
            this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;

            this.age = Math.max(1, lifeTicks);
            this.maxAge = this.age;
            this.first = color;
            this.second = color;

            this.hasStopY = true;
            this.stopY = stopY;
            this.skyMode = skyMode;

            this.constantAlpha = false;
            this.alphaMaxAge = this.maxAge;

            this.useInOutAlpha = true;
            this.fadeInTicks = Math.max(20, maxAge / 5);
            this.fadeOutTicks = Math.max(36, maxAge / 3);
        }

        @Deprecated
        public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, Color first, Color second, ParticleType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            age = (int) MathUtil.random(100, 300);
            maxAge = age;
            this.first = first;
            this.second = second;
            this.type = type;

            this.constantAlpha = false;
            this.alphaMaxAge = this.maxAge;
            this.useInOutAlpha = false;
        }

        public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, Color color, ParticleType type, boolean trailMode, int lifeTicks) {
            this.type = type;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevposX = posX;
            this.prevposY = posY;
            this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;

            this.age = Math.max(1, lifeTicks);
            this.maxAge = this.age;
            this.first = color;
            this.second = color;

            this.skyMode = trailMode;
            this.hasStopY = false;
            this.skyGravity = 0.0f;
            this.skyLift = 0.0025f;
            this.skyDamping = 0.992f;
            this.windJitter = 0.0015f;

            this.constantAlpha = false;
            this.alphaMaxAge = this.maxAge;

            this.useInOutAlpha = true;
            this.fadeInTicks = Math.max(10, maxAge / 6);
            this.fadeOutTicks = Math.max(16, maxAge / 5);
        }

        public ParticleBase withSpawnDelayAndPulse(int delayTicks,
                                                   float pulseSpeed,
                                                   float pulsePhase,
                                                   float minScale,
                                                   float maxScale,
                                                   float alphaAmp) {
            this.spawnDelayTicks = Math.max(0, delayTicks);
            this.pulse = true;
            this.pulseSpeed = pulseSpeed;
            this.pulsePhase = pulsePhase;
            this.pulseMinScale = Math.max(0.2f, minScale);
            this.pulseMaxScale = Math.max(this.pulseMinScale, maxScale);
            this.pulseAlphaAmp = MathHelper.clamp(alphaAmp, 0f, 1f);
            return this;
        }

        public ParticleBase withSkyTuning(float lift, float gravity, float damping, float wind) {
            this.skyLift = lift;
            this.skyGravity = gravity;
            this.skyDamping = damping;
            this.windJitter = wind;
            return this;
        }

        public boolean tick() {
            if (spawnDelayTicks > 0) {
                spawnDelayTicks--;
                return false;
            }

            if (isInsideSolid(posX, posY, posZ) || isInsideFluid(posX, posY, posZ)) {
                return true;
            }

            age -= 1;
            if (age < 0) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            if (skyMode) {
                motionX += MathUtil.random(-windJitter, windJitter);
                motionZ += MathUtil.random(-windJitter, windJitter);
                motionX *= skyDamping;
                motionZ *= skyDamping;

                motionY += skyLift;
                motionY -= skyGravity;

                posX += motionX;
                posZ += motionZ;
                float nextY = posY + motionY;

                if (hasStopY && nextY <= stopY) {
                    posY = stopY;
                    motionY = 0f;
                    landed = true;

                    if (fadeOutTicks > 0) {
                        age = Math.min(age, fadeOutTicks);
                    }

                    return false;
                }

                if (!landed) posY = nextY;
                return false;
            }

            final float bounceFactor = 0.6f;
            final float friction = 0.7f;
            final float minBounce = 0.02f;
            final float airResistance = 0.98f;
            final float radius = 0.2f;

            boolean collidedX = false, collidedY = false, collidedZ = false;
            boolean floorLocked = false;

            float prevMotionY = motionY;

            Box boxX = new Box(posX + motionX - radius, posY - radius, posZ - radius,
                    posX + motionX + radius, posY + radius, posZ + radius);
            outerX:
            for (int x = (int) Math.floor(boxX.minX); x <= (int) Math.floor(boxX.maxX); x++)
                for (int y = (int) Math.floor(boxX.minY); y <= (int) Math.floor(boxX.maxY); y++)
                    for (int z = (int) Math.floor(boxX.minZ); z <= (int) Math.floor(boxX.maxZ); z++)
                        if (collidesWithBlock(x, y, z, boxX)) {
                            motionX *= -bounceFactor;
                            collidedX = true;
                            break outerX;
                        }
            if (!collidedX) posX += motionX;

            float nextY = posY + motionY;
            if (hasStopY && nextY <= stopY) {
                posY = stopY;
                motionY = 0f;
                collidedY = true;
                floorLocked = true;
            } else {
                Box boxY = new Box(posX - radius, posY + motionY - radius, posZ - radius,
                        posX + radius, posY + motionY + radius, posZ + radius);
                outerY:
                for (int x = (int) Math.floor(boxY.minX); x <= (int) Math.floor(boxY.maxX); x++)
                    for (int y = (int) Math.floor(boxY.minY); y <= (int) Math.floor(boxY.maxY); y++)
                        for (int z = (int) Math.floor(boxY.minZ); z <= (int) Math.floor(boxY.maxZ); z++)
                            if (collidesWithBlock(x, y, z, boxY)) {
                                motionY *= -bounceFactor;
                                collidedY = true;
                                if (Math.abs(motionY) < minBounce) motionY = 0f;
                                break outerY;
                            }
                if (!collidedY) posY += motionY;
            }

            Box boxZ = new Box(posX - radius, posY - radius, posZ + motionZ - radius,
                    posX + radius, posY + radius, posZ + motionZ + radius);
            outerZ:
            for (int x = (int) Math.floor(boxZ.minX); x <= (int) Math.floor(boxZ.maxX); x++)
                for (int y = (int) Math.floor(boxZ.minY); y <= (int) Math.floor(boxZ.maxY); y++)
                    for (int z = (int) Math.floor(boxZ.maxZ); z >= (int) Math.floor(boxZ.minZ); z--)
                        if (collidesWithBlock(x, y, z, boxZ)) {
                            motionZ *= -bounceFactor;
                            collidedZ = true;
                            break outerZ;
                        }
            if (!collidedZ) posZ += motionZ;

            handleGroundContact(collidedY, prevMotionY, floorLocked);

            float frictionFactor = airResistance;
            if (collidedY) {
                float t = MathHelper.clamp(groundContactTicks / 6.0f, 0.0f, 1.0f);
                frictionFactor = MathHelper.lerp(t, friction, 0.88f);
            }

            motionX *= frictionFactor;
            motionZ *= frictionFactor;

            resolvePenetration(radius);

            if (!(hasStopY && posY <= stopY + 1e-3f && motionY <= 0f)) motionY -= 0.04f;

            return false;
        }

        public boolean render(BufferBuilder bufferBuilder) {
            if (spawnDelayTicks > 0) return false;

            Camera camera = mc.gameRenderer.getCamera();
            Color color1 = first;
            Color color2 = second;

            Vec3d pos = interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);
            MatrixStack matrices = new MatrixStack();

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float scale = 1.0f;
            float alphaPulseMul = 1.0f;
            if (pulse) {
                float livedNorm = 1.0f - (age / (float) Math.max(1, maxAge));
                float s = (float) Math.sin(pulsePhase + livedNorm * pulseSpeed);
                float t = 0.5f * (s + 1.0f);
                scale = MathHelper.lerp(t, pulseMinScale, pulseMaxScale);
                alphaPulseMul = 1.0f + pulseAlphaAmp * (t - 0.5f) * 2.0f;
            }

            matrices.scale(scale, scale, scale);

            final float sz = 0.25F;

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            int alpha;
            if (constantAlpha) {
                alpha = 255;
            } else if (useInOutAlpha) {
                int lived = Math.max(0, alphaMaxAge - age);
                float aIn = (fadeInTicks > 0) ? Math.min(1f, lived / (float) fadeInTicks) : 1f;
                float aOut = (fadeOutTicks > 0) ? Math.min(1f, age / (float) fadeOutTicks) : 1f;
                float a = Math.min(aIn, aOut);
                alpha = (int) (255f * MathHelper.clamp(a, 0f, 1f));
            } else {
                int denom = Math.max(1, alphaMaxAge);
                alpha = (int) (255f * (age / (float) denom));
            }
            alpha = (int) (alpha * MathHelper.clamp(alphaPulseMul, 0f, 1.5f));
            alpha = MathHelper.clamp(alpha, 0, 255);
            bufferBuilder.vertex(matrix, 0, -sz, 0).texture(0f, 1f).color(injectAlpha(color2, alpha).getRGB());
            bufferBuilder.vertex(matrix, -sz, -sz, 0).texture(1f, 1f).color(injectAlpha(color1, alpha).getRGB());
            bufferBuilder.vertex(matrix, -sz, 0, 0).texture(1f, 0).color(injectAlpha(color2, alpha).getRGB());
            bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(injectAlpha(color1, alpha).getRGB());
            return true;
        }

        boolean collidesWithBlock(int x, int y, int z, Box box) {
            BlockPos blockPos = new BlockPos(x, y, z);
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.isAir()) return false;

            VoxelShape shape = state.getCollisionShape(mc.world, blockPos);
            if (shape.isEmpty()) return false;

            for (Box voxelBox : shape.getBoundingBoxes()) {
                Box shiftedBox = voxelBox.offset(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (shiftedBox.intersects(box)) return true;
            }
            return false;
        }

        private void handleGroundContact(boolean collidedY, float prevMotionY, boolean floorLocked) {
            if (!collidedY) {
                groundContactTicks = 0;
                return;
            }

            if (floorLocked) {
                groundContactTicks = Math.min(groundContactTicks + 1, 12);
                return;
            }

            if (prevMotionY < -0.001f) {
                float bounce = MathHelper.clamp(Math.abs(prevMotionY) * 0.55f, 0.025f, 0.22f);
                float extra = 0.01f * Math.min(groundContactTicks, 3);
                motionY = Math.max(motionY, bounce + extra);
                groundContactTicks = 0;
            } else {
                groundContactTicks = Math.min(groundContactTicks + 1, 12);
                if (motionY <= 0.001f) {
                    float gentle = 0.018f + 0.006f * groundContactTicks;
                    motionY = MathHelper.clamp(gentle, 0.018f, 0.055f);
                }
            }

            if (motionY > 0f && !floorLocked) {
                float sway = MathHelper.clamp(motionY * 0.25f, 0.003f, 0.02f);
                motionX += MathUtil.random(-sway, sway);
                motionZ += MathUtil.random(-sway, sway);
            }
        }

        private void resolvePenetration(float radius) {
            Box particleBox = new Box(posX - radius, posY - radius, posZ - radius,
                    posX + radius, posY + radius, posZ + radius);

            if (mc.world.isSpaceEmpty(particleBox)) {
                stuckTicks = Math.max(0, stuckTicks - 1);
                return;
            }

            Vec3d totalPush = Vec3d.ZERO;
            boolean adjusted = false;

            int minX = MathHelper.floor(particleBox.minX);
            int maxX = MathHelper.floor(particleBox.maxX);
            int minY = MathHelper.floor(particleBox.minY);
            int maxY = MathHelper.floor(particleBox.maxY);
            int minZ = MathHelper.floor(particleBox.minZ);
            int maxZ = MathHelper.floor(particleBox.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(blockPos);
                        if (state.isAir()) continue;

                        VoxelShape shape = state.getCollisionShape(mc.world, blockPos);
                        if (shape.isEmpty()) continue;

                        for (Box voxelBox : shape.getBoundingBoxes()) {
                            Box shiftedBox = voxelBox.offset(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                            if (!shiftedBox.intersects(particleBox)) continue;

                            Vec3d push = minimalPush(particleBox, shiftedBox);
                            if (push.lengthSquared() <= 0) continue;

                            totalPush = totalPush.add(push);
                            particleBox = particleBox.offset(push);
                            adjusted = true;
                        }
                    }
                }
            }

            if (adjusted) {
                posX += (float) totalPush.x;
                posY += (float) totalPush.y;
                posZ += (float) totalPush.z;

                double len = totalPush.lengthSquared();
                if (len > 1.0e-8) {
                    Vec3d dir = totalPush.normalize();
                    double vAlong = motionX * dir.x + motionY * dir.y + motionZ * dir.z;
                    if (vAlong < 0) {
                        motionX -= (float) (dir.x * vAlong);
                        motionY -= (float) (dir.y * vAlong);
                        motionZ -= (float) (dir.z * vAlong);
                    }
                }

                stuckTicks = 0;
            } else {
                stuckTicks = Math.min(stuckTicks + 1, 20);
            }

            if (stuckTicks > 4) {
                float jitter = 0.01f + 0.004f * stuckTicks;
                motionX += MathUtil.random(-jitter, jitter);
                motionZ += MathUtil.random(-jitter, jitter);
            }
        }

        private Vec3d minimalPush(Box particleBox, Box obstacle) {
            double pushXPos = obstacle.maxX - particleBox.minX;
            double pushXNeg = particleBox.maxX - obstacle.minX;
            double pushYPos = obstacle.maxY - particleBox.minY;
            double pushYNeg = particleBox.maxY - obstacle.minY;
            double pushZPos = obstacle.maxZ - particleBox.minZ;
            double pushZNeg = particleBox.maxZ - obstacle.minZ;

            double min = Double.MAX_VALUE;
            Vec3d result = Vec3d.ZERO;

            if (pushXPos > 0 && pushXPos < min) {
                min = pushXPos;
                result = new Vec3d(pushXPos + 1.0e-3, 0, 0);
            }
            if (pushXNeg > 0 && pushXNeg < min) {
                min = pushXNeg;
                result = new Vec3d(-(pushXNeg + 1.0e-3), 0, 0);
            }
            if (pushYPos > 0 && pushYPos < min) {
                min = pushYPos;
                result = new Vec3d(0, pushYPos + 1.0e-3, 0);
            }
            if (pushYNeg > 0 && pushYNeg < min) {
                min = pushYNeg;
                result = new Vec3d(0, -(pushYNeg + 1.0e-3), 0);
            }
            if (pushZPos > 0 && pushZPos < min) {
                min = pushZPos;
                result = new Vec3d(0, 0, pushZPos + 1.0e-3);
            }
            if (pushZNeg > 0 && pushZNeg < min) {
                min = pushZNeg;
                result = new Vec3d(0, 0, -(pushZNeg + 1.0e-3));
            }

            return result;
        }

        public static Vec3d interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
            double x = prevposX + ((posX - prevposX) * MathUtil.getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double y = prevposY + ((posY - prevposY) * MathUtil.getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double z = prevposZ + ((posZ - prevposZ) * MathUtil.getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            return new Vec3d(x, y, z);
        }

        public static Color injectAlpha(final Color color, final int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
        }
    }

    private static boolean isSpawnClear(float x, float y, float z) {
        if (mc == null || mc.world == null) {
            return false;
        }

        Box box = new Box(
                x - 0.08f,
                y - 0.02f,
                z - 0.08f,
                x + 0.08f,
                y + 0.08f,
                z + 0.08f
        );

        if (!mc.world.isSpaceEmpty(box)) {
            return false;
        }

        return !isInsideSolid(x, y, z) && !isInsideFluid(x, y, z);
    }

    private static boolean isInsideSolid(float x, float y, float z) {
        if (mc == null || mc.world == null) {
            return false;
        }

        BlockPos pos = BlockPos.ofFloored(x, y, z);
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        VoxelShape shape = state.getCollisionShape(mc.world, pos);
        if (shape.isEmpty()) {
            return false;
        }

        double relX = x - pos.getX();
        double relY = y - pos.getY();
        double relZ = z - pos.getZ();

        for (Box box : shape.getBoundingBoxes()) {
            if (box.contains(relX, relY, relZ)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInsideFluid(float x, float y, float z) {
        if (mc == null || mc.world == null) {
            return false;
        }

        BlockPos pos = BlockPos.ofFloored(x, y, z);
        FluidState fluidState = mc.world.getFluidState(pos);
        if (fluidState.isEmpty()) {
            return false;
        }

        double relY = y - pos.getY();
        double height = fluidState.getHeight(mc.world, pos);
        return relY <= height + 1.0e-3;
    }

    private boolean isSelectedProjectile(Entity ent) {
        boolean wantPearl = typeFollow.get("Эндер-жемчуга").getEnabled();
        boolean wantTrident = typeFollow.get("Трезубца").getEnabled();
        boolean wantSnowball = typeFollow.get("Снежка").getEnabled();
        if (wantTrident && ent instanceof net.minecraft.entity.projectile.TridentEntity) return true;
        if (wantPearl && ent instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity) return true;
        return wantSnowball && ent instanceof net.minecraft.entity.projectile.thrown.SnowballEntity;
    }

    private boolean isProjectileFlying(Entity ent) {
        final double EPS_VEL_SQ = 5.0E-5;

        if (ent.isRemoved()) return false;

        double v2 = ent.getVelocity().lengthSquared();
        if (v2 < EPS_VEL_SQ) return false;

        if (ent.isOnGround()) return false;
        if (ent.horizontalCollision) return false;

        if (ent instanceof net.minecraft.entity.projectile.PersistentProjectileEntity ppe) {
            if (ppe.isNoClip()) return false;
        }
        return true;
    }

    private ParticleType getParticleType() {
        String m = particleMode.getValue();
        return switch (m) {
            case "Heart" -> ParticleType.HEART;
            case "Crown" -> ParticleType.CROWN;
            case "Dollar" -> ParticleType.DOLLAR;
            case "Skull" -> ParticleType.SKULL;
            case "Star" -> ParticleType.STAR;
            case "Bloom" -> ParticleType.BLOOM;
            case "Random" -> ParticleType.getRandom();
            default -> ParticleType.HEART;
        };
    }

    private Color getColors() {
        int c = switch (this.colorMode.getValue()) {
            case "Клиентский" -> ColorUtilsExcellent.fade(4, particles.size(), Interface.INSTANCE.getMainColor(), Interface.INSTANCE.getMainColor());
            case "Рандом" -> ColorUtilsExcellent.randomColor();
            case "Свой" -> ColorUtilsExcellent.fade(4, particles.size() * 100, this.customColor.getColorRGBA(), ColorUtilsExcellent.multDark(this.customColor.getColorRGBA(), 0.5F));
            default -> -1;
        };
        return new Color(c, true);
    }

    private float norm(float v) {
        return MathHelper.clamp((v - 1f) / 24f, 0f, 1f);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int lifeFromSlider() {
        int ms = duration.getValue().intValue();
        return Math.max(10, ms / 50);
    }


    @Getter
    @Accessors(fluent = true)
    public enum ParticleType {
        CROWN("crown", false),
        DOLLAR("dollar", false),
        HEART("heart", false),
        SKULL("skull", false),
        STAR("star", true),
        BLOOM("bloom", false);

        private final Identifier texture;
        private final boolean rotatable;

        ParticleType(String name, boolean rotatable) {
            texture = ClientTexture.id("particle/" + name + ".png");
            this.rotatable = rotatable;
        }

        public static ParticleType getRandom() {
            ParticleType[] values = ParticleType.values();
            return values[new Random().nextInt(values.length)];
        }
    }

    public static final class RandomUtil {

        private RandomUtil() {}

        private static void validateRange(double min, double max) {
            if (max < min) {
                throw new IllegalArgumentException("max не может быть меньше min.");
            }
        }

        public static double randomValue(double min, double max) {
            validateRange(min, max);
            return min + ThreadLocalRandom.current().nextDouble() * (max - min);
        }

        public static float randomValue(float min, float max) {
            validateRange(min, max);
            return (float) (min + ThreadLocalRandom.current().nextDouble() * (max - min));
        }
    }
}
