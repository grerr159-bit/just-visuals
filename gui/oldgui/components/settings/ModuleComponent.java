package dev.client.api.nullcry.uiClient.clickGui.components.settings;

import dev.client.Lumi;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.helper.client.keyboard.KeyboardStorage;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.Helper;
import dev.client.api.nullcry.uiClient.clickGui.api.component.Component;
import dev.client.api.nullcry.uiClient.clickGui.api.component.ComponentLayer;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleComponent extends Component {
    Module module;
    List<SettingComponent> components = new ArrayList<>();
    @NonFinal
    private float globalAlpha = 1f;

    @NonFinal
    private float bindVisibility = 0f;
    @NonFinal
    private float bindVisibilityTarget = 0f;
    @NonFinal
    private boolean bindHovered = false;
    @NonFinal
    private boolean nameHovered = false;
    @NonFinal
    private float bindX = 0f;
    @NonFinal
    private float bindY = 0f;
    @NonFinal
    private float bindWidth = 0f;
    @NonFinal
    private float bindHeight = 0f;
    @NonFinal
    private float nameClipX = 0f;
    @NonFinal
    private float nameClipWidth = 0f;

    public void setGlobalAlpha(float alpha) {
        this.globalAlpha = MathHelper.clamp(alpha, 0f, 1f);
    }

    @NonFinal
    @Getter
    @Setter
    private boolean settingsOpened = false;

    public ModuleComponent(Module module) {
        this.module = module;
        this.components.addAll(Helper.settingComponents(module));
    }

    @Override
    public ComponentLayer render(DrawContext context, int mouseX, int mouseY, float delta) {
        final float headerHeight = 22f;
        float animation = module.getAnimation().getOutput().floatValue();

        ClientApi.outline()
                .size(new SizeState(getWidth() - 6, getHeight()))
                .radius(new QuadRadiusState(4f))
                .color(new QuadColorState(ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (75 * globalAlpha))))
                .thickness(-0.1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), getX() + 3, getY());

        String moduleName = module.getName();
        int keyCode = module.getKey();
        String keyText = KeyboardStorage.getKey(keyCode);
        String resolvedBindText = (keyCode != -1 && keyText != null && !keyText.isEmpty()) ? keyText : "N/A";
        String displayBindText = module.isBinding() ? "Press" : resolvedBindText;

        int textColor;
        if (module.isEnabled()) {
            textColor = ColorUtils.setAlpha(
                    -1,
                    (int) (globalAlpha * (255 * (50 + 50 * animation) / 100f))
            );
        } else {
            textColor = ColorUtils.setAlpha(ColorUtils.rgb(180, 180, 180), (int) (globalAlpha * (255 * (50 + 50 * (1 - animation)) / 100f)));
        }

        float textSize = 8f;
        float textHeight = ClientApi.inter().getHeight(moduleName, textSize);
        float textX = getX() + 10f;
        float textY = getY() + (headerHeight - textHeight) / 2f;

        float iconSize = 9f;
        boolean hasSettings = !components.isEmpty();
        float iconX = getX() + getWidth() - iconSize - 10f;
        float iconY = getY() + (headerHeight - iconSize) / 2f + 0.5f;

        boolean shouldShowBind = Screen.hasAltDown() || settingsOpened || module.isBinding();
        bindVisibilityTarget = shouldShowBind ? 1f : 0f;
        bindVisibility = MathHelper.lerp(0.25f, bindVisibility, bindVisibilityTarget);
        if (Math.abs(bindVisibility - bindVisibilityTarget) <= 0.01f) {
            bindVisibility = bindVisibilityTarget;
        }

        bindWidth = ClientApi.inter().getWidth(displayBindText, 6f) + 7f;
        bindHeight = ClientApi.inter().getHeight(displayBindText, 6f) + 6f;
        float bindAnchor = hasSettings ? iconX : getX() + getWidth() - 10f;
        float bindGap = hasSettings ? 6f : -1f;
        bindX = bindAnchor - bindWidth - bindGap;
        bindY = getY() + (headerHeight - bindHeight) / 2f + 0.5f;

        float rightLimit = getX() + getWidth() - 8f;
        if (hasSettings) {
            rightLimit = Math.min(rightLimit, iconX - 4f);
        }
        if (bindVisibility > 0.001f) {
            rightLimit = Math.min(rightLimit, bindX - 4f);
        }
        float clipWidth = Math.max(0f, rightLimit - textX);

        nameClipX = textX;
        nameClipWidth = clipWidth;
        nameHovered = clipWidth > 0f && MouseClick.isClick(mouseX, mouseY, textX, getY(), clipWidth, headerHeight);

        if (clipWidth > 0f) {
            ScissorUtil.enable(textX, getY(), clipWidth, headerHeight);
            ClientApi.text()
                    .size(textSize)
                    .font(ClientApi.inter())
                    .color(textColor)
                    .text(moduleName)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), textX, textY);
            ScissorUtil.disable();
        }

        if (hasSettings) {
            float iconCenterX = iconX + iconSize / 2f;
            float iconCenterY = iconY + iconSize / 2f;
            Matrix4f iconMatrix = new Matrix4f(context.getMatrices().peek().getPositionMatrix());
            if (settingsOpened) {
                iconMatrix.translate(iconCenterX, iconCenterY, 0f);
                iconMatrix.rotateZ((float) Math.toRadians(90f));
                iconMatrix.translate(-iconCenterX, -iconCenterY, 0f);
            }

            ClientApi.text()
                    .text("G")
                    .size(iconSize)
                    .font(ClientApi.otherIcons())
                    .color(ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (100 * globalAlpha)))
                    .build()
                    .render(iconMatrix, iconX, iconY);
        }

        bindHovered = false;
        if (bindVisibility > 0.01f) {
            float blurAlpha = globalAlpha * bindVisibility;
            if (Interface.INSTANCE.blurStrength.getValue() > 0) {
                ClientApi.blur()
                        .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                        .radius(new QuadRadiusState(2f))
                        .size(new SizeState(bindWidth, bindHeight))
                        .alpha(blurAlpha)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), bindX, bindY);
            }

            float backgroundAlphaBase = module.isBinding() ? 175f : 150f;
            float alphaFactor = bindVisibility * globalAlpha;
            int topLeft = ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), (int) (backgroundAlphaBase * alphaFactor));
            int topRight = ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), (int) (backgroundAlphaBase * alphaFactor));
            int bottomLeft = ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), (int) (backgroundAlphaBase * alphaFactor));
            int bottomRight = ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), (int) (backgroundAlphaBase * alphaFactor));

            ClientApi.rectangle()
                    .size(new SizeState(bindWidth, bindHeight))
                    .color(new QuadColorState(topLeft, topRight, bottomLeft, bottomRight))
                    .radius(new QuadRadiusState(2f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), bindX, bindY);

            int bindColor;
            if (module.isBinding()) {
                bindColor = ColorUtils.setAlpha(ColorUtils.rgb(220, 220, 220), (int) (255 * alphaFactor));
            } else if (keyCode == -1) {
                bindColor = ColorUtils.setAlpha(ColorUtils.rgb(130, 130, 130), (int) (255 * alphaFactor));
            } else {
                bindColor = ColorUtils.setAlpha(-1, (int) (255 * alphaFactor));
            }

            float bindTextWidth = ClientApi.inter().getWidth(displayBindText, 6f);
            ClientApi.text()
                    .size(6f)
                    .font(ClientApi.inter())
                    .color(bindColor)
                    .text(displayBindText)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), bindX + (bindWidth - bindTextWidth) / 2f, bindY + 2.5f);

            bindHovered = MouseClick.isClick(mouseX, mouseY, bindX, bindY, bindWidth, bindHeight);
            if (bindHovered) {
                CursorsUtil.setCursor(CursorsUtil.HAND);
            }
        }

        if (settingsOpened) {
            AtomicReference<Float> offset = new AtomicReference<>(0f);
            float settingsWidth = Math.max(0f, getWidth() - 18f);
            float settingsX = getX() + 11f;
            components.stream()
                    .filter(e -> e.getSetting().getShown().get())
                    .forEach(e -> {
                        e.setGlobalAlpha(globalAlpha);
                        e.size(settingsWidth, e.getHeight());
                        e.position(settingsX, getY() + headerHeight + 1f + offset.get()).render(context, mouseX, mouseY, delta);
                        offset.set(offset.get() + e.getHeight() + 5f);
                    });
        }

        boolean headerHovered = MouseClick.isClick(mouseX, mouseY, getX() + 2f, getY(), getWidth() - 4f, headerHeight);
        if (headerHovered) {
            CursorsUtil.setCursor(CursorsUtil.HAND);
        }

        return null;
    }

    public float getFullHeight() {
        float baseHeight = 22f;

        if (settingsOpened) {
            for (SettingComponent component : components) {
                if (component.getSetting().getShown().get()) {
                    baseHeight += component.getHeight() + 5f;
                }
            }
        }

        return baseHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean hasSettings = !components.isEmpty();
        float headerHeight = 22f;

        boolean headerHovered = MouseClick.isClick(mouseX, mouseY, getX(), getY(), getWidth(), headerHeight);
        boolean bindAreaHovered = bindVisibility > 0.01f && MouseClick.isClick(mouseX, mouseY, bindX, bindY, bindWidth, bindHeight);

        // --- если уже идёт биндинг ---
        if (module.isBinding()) {
            if (bindAreaHovered) {
                if (button != 0) {
                    module.setKey(-100 + button);
                    module.setBinding(false);
                } else {
                    module.setBinding(false);
                }
                return true;
            }

            if (button == 0) {
                module.setBinding(false);
                return true;
            }
        }

        if (!settingsOpened && headerHovered && button == 2) {
            startBinding();
            bindVisibilityTarget = 1f;
            return true;
        }

        if (bindAreaHovered) {
            if (button == 0 || button == 2) {
                startBinding();
                return true;
            }
        }

        if (settingsOpened) {
            for (SettingComponent component : components) {
                if (component.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        if (headerHovered) {
            if (button == 0) {
                if (!bindAreaHovered) {
                    module.toggle();
                    return true;
                }
            }

            if (button == 1) {
                if (hasSettings) {
                    settingsOpened = !settingsOpened;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startBinding() {
        Lumi.getInstance().getModuleManager().getModules().forEach(other -> {
            if (other != module && other.isBinding()) {
                other.setBinding(false);
            }
        });
        module.setBinding(true);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(e -> e.mouseReleased(mouseX, mouseY, button));

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (module.isBinding()) {
            module.setKey(keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
            module.setBinding(false);

            return true;
        }

        components.forEach(e -> e.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        components.forEach(e -> e.keyReleased(keyCode, scanCode, modifiers));
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        components.forEach(e -> e.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
}
