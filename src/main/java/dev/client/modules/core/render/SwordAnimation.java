package dev.client.modules.core.render;

import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class SwordAnimation extends Module {
    public static SwordAnimation INSTANCE;

    private float glowAlpha = 0f;
    private long lastSwingTime = 0;
    private float smoothSwing = 0f;

    public SwordAnimation() {
        super("SwordAnimation", ModuleCategory.Visuals, "Анимации предметов в руках");
        INSTANCE = this;
    }

    public final ModeElement mode = new ModeElement("Анимация", () -> true)
            .set("None", "Swipe", "Spin", "Slash", "Snap", "Wave", "Flick")
            .defaultValue("None").register(this);
    public final Slider power = new Slider("Сила", () -> !mode.isSelected("None")).set(1.0f, 10.0f, 1).defaultValue(5.0f).register(this);
    public final Slider speed = new Slider("Скорость", () -> !mode.isSelected("None")).set(3.0f, 10.0f, 1).defaultValue(10.0f).register(this);
    public final Slider scale = new Slider("Масштаб", () -> true).set(0.5f, 1.5f, 0.1f).defaultValue(1.0f).register(this);

    public void animationProcess(MatrixStack matrices, float swingProgress, Arm handSide) {
        float t = swingProgress;
        float anim = (float) Math.sin(t * Math.PI);
        float easeOut = 1f - (1f - t) * (1f - t);
        float easeIn = t * t;

        float scaleVal = scale.getValue();
        float powerVal = power.getValue();
        float speedVal = speed.getValue();
        int handSign = handSide == Arm.RIGHT ? 1 : -1;

        switch (mode.getValue()) {
            case "None":
                matrices.scale(scaleVal, scaleVal, scaleVal);
                break;

            case "Swipe": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                matrices.translate(handSign * 0.4f, 0.1f, -0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSign * 90));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * -60));
                float swingAngle = -90 - (powerVal * 10) * anim;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingAngle));
                break;
            }

            case "Spin": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                matrices.translate(0.0f, 0.1f, 0.0f);
                if (t > 0.001f) {
                    float eased = MathHelper.sin(t * (float) Math.PI * 0.5f);
                    float rot = eased * 360.0f;
                    if (rot > 360.0f) rot = 360.0f;
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(rot));
                }
                break;
            }

            case "Slash": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                matrices.translate(handSign * 0.4f, 0, -0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSign * 90));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * -30));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90 - (powerVal * 10) * anim));

                float slashWave = (float) Math.sin(t * Math.PI * 3) * (1f - t) * 5f;
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * slashWave));
                break;
            }

            case "Snap": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                float snapForward = easeOut * 0.3f * powerVal / 10f;
                matrices.translate(handSign * snapForward, -snapForward * 0.5f, -snapForward);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(easeOut * -40f * powerVal / 10f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * easeIn * -25f * powerVal / 10f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSign * 45f));
                break;
            }

            case "Wave": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                float waveX = (float) Math.sin(t * Math.PI * 2) * powerVal / 10f * 15f;
                float waveY = (float) Math.sin(t * Math.PI) * powerVal / 10f * 20f;
                matrices.translate(handSign * waveX * 0.01f, -waveY * 0.005f, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90 - waveY));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSign * (45 + waveX)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * MathHelper.sin(t * (float) Math.PI) * -20f));
                break;
            }

            case "Flick": {
                applyEquipOffset(matrices, handSide, 0.0F);
                matrices.scale(scaleVal, scaleVal, scaleVal);
                float flickT = MathHelper.clamp(t * 2f, 0f, 1f);
                float flickBack = MathHelper.clamp((t - 0.5f) * 2f, 0f, 1f);
                matrices.translate(handSign * flickBack * 0.3f, -flickT * 0.2f * powerVal / 10f, -flickT * 0.4f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90 - flickT * 60f * powerVal / 10f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSign * (45 + flickBack * 30f)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSign * -flickT * 15f));
                break;
            }

            default:
                break;
        }
    }

    public void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!player.isUsingSpyglass()) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            matrices.push();
            if (item.isOf(Items.CROSSBOW)) {
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == Arm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));
                    float f = (float) item.getMaxUseTime(player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / (float) CrossbowItem.getPullTime(item, player);
                    if (g > 1.0F) g = 1.0F;
                    if (g > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
                } else {
                    float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                    float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate((float) i * fx, gx, h);
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    this.applySwingOffset(matrices, arm, swingProgress);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
                    }
                }
                this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                boolean bl2 = arm == Arm.RIGHT;
                ViewModel viewModel = ViewModel.INSTANCE;
                if (viewModel.isEnabled()) {
                    if (bl2) {
                        matrices.translate(viewModel.right_x.getValue(), viewModel.right_y.getValue(), viewModel.right_z.getValue());
                    } else {
                        matrices.translate(viewModel.left_x.getValue(), viewModel.left_y.getValue(), viewModel.left_z.getValue());
                    }
                }

                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    int l = bl2 ? 1 : -1;
                    switch (item.getUseAction()) {
                        case NONE, BLOCK:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case EAT:
                        case DRINK:
                            this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case BOW:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float mx = (float) item.getMaxUseTime(player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fxx = mx / 20.0F;
                            fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                            if (fxx > 1.0F) fxx = 1.0F;
                            if (fxx > 0.1F) {
                                float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                                float h = fxx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case SPEAR:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float m = (float) item.getMaxUseTime(player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fx = m / 10.0F;
                            if (fx > 1.0F) fx = 1.0F;
                            if (fx > 0.1F) {
                                float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                                float h = fx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(0.0F, 0.0F, fx * 0.2F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case BRUSH:
                            this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                    }
                } else if (player.isUsingRiptide()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    int l = bl2 ? 1 : -1;
                    matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
                } else {
                    boolean isDominant = (arm == player.getMainArm());
                    boolean canCustom = this.isEnabled() && !mode.isSelected("None");
                    boolean useCustom = canCustom && isDominant;

                    if (useCustom) {
                        animationProcess(matrices, swingProgress, arm);
                    } else {
                        float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                        float fxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                        int o = arm == Arm.RIGHT ? 1 : -1;
                        matrices.translate((float) o * n, mxx, fxx);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    }
                }

                boolean scaleAllowed = this.isEnabled() && (arm == player.getMainArm());
                if (scaleAllowed) {
                    float s = scale.getValue();
                    if (s != 1.0f) matrices.scale(s, s, s);
                }

                this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
            }
            matrices.pop();
        }
    }

    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) (mc.player.getItemUseTimeLeft() % 10);
        float g = f - tickDelta + 1.0F;
        float h = 1.0F - g / 10.0F;
        float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
        if (arm != Arm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float) stack.getMaxUseTime(mc.player);
        if (g < 0.8F) {
            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, h, 0.0F);
        }
        float h = 1.0F - (float) Math.pow(g, 27.0);
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            boolean shouldGlow = false;
            SwordHelper swordHelper = SwordHelper.INSTANCE;
            if (swordHelper != null && swordHelper.isEnabled() && swordHelper.glowEnabled.getEnabled() && mc.player != null) {
                shouldGlow = true;
            }
            if (shouldGlow) {
                glowAlpha = Math.min(1.0f, glowAlpha + 0.15f);
                if (glowAlpha > 0.01f) renderGlow(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, light);
            } else {
                glowAlpha = Math.max(0.0f, glowAlpha - 0.15f);
                if (glowAlpha > 0.01f) renderGlow(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, light);
            }
            mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }

    private void renderGlow(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        SwordHelper swordHelper = SwordHelper.INSTANCE;
        int r = swordHelper.glowR.getValue().intValue();
        int g = swordHelper.glowG.getValue().intValue();
        int b = swordHelper.glowB.getValue().intValue();
        int a = (int) (swordHelper.glowAlpha.getValue() * glowAlpha);
        final int glowARGB = (a << 24) | ((r << 16) & 0xFF0000) | ((g << 8) & 0xFF00) | (b & 0xFF);

        VertexConsumerProvider glowProvider = (renderLayer) -> {
            VertexConsumer original = vertexConsumers.getBuffer(renderLayer);
            if (original == null) return vertexConsumers.getBuffer(renderLayer);
            return new VertexConsumer() {
                private Matrix4f mat;
                private float px, py, pz, tu, tv, nx, ny, nz;
                private int ov = OverlayTexture.DEFAULT_UV;
                private int lt = light;

                public VertexConsumer vertex(Matrix4f m, float x, float y, float z) { this.mat = m; this.px = x; this.py = y; this.pz = z; return this; }
                public VertexConsumer vertex(float x, float y, float z) { return this; }
                public VertexConsumer texture(float u, float v) { this.tu = u; this.tv = v; return this; }
                public VertexConsumer overlay(int u, int v) { this.ov = (v << 16) | u; return this; }
                public VertexConsumer overlay(int o) { this.ov = o; return this; }
                public VertexConsumer light(int bl, int sl) { this.lt = (bl << 20) | (sl << 4); return this; }
                public VertexConsumer light(int l) { this.lt = l; return this; }
                public VertexConsumer normal(float x, float y, float z) { this.nx = x; this.ny = y; this.nz = z; return this; }

                private void flush() {
                    if (mat != null) {
                        original.vertex(mat, px, py, pz).texture(tu, tv).overlay(ov).light(lt).normal(nx, ny, nz).color(glowARGB);
                    }
                }

                public VertexConsumer color(int color) { flush(); return this; }
                public VertexConsumer color(int red, int green, int blue, int alpha) { flush(); return this; }
                public void next() {}
            };
        };

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        matrices.push();
        matrices.scale(1.08f, 1.08f, 1.08f);
        mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, glowProvider, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.drawCurrentLayer();
        }
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
