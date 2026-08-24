package dev.client.api.nullcry.helper.other;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.IScreen;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.cmdHelper.managers.dragHandler.DraggableManager;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.render.core.builders.Builder;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import dev.other.vector.Vec2i;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Supplier;

public class DraggableHandler implements IScreen {
    @Expose
    @SerializedName("name")
    @Getter
    private final String name;
    @Getter
    public float initialPosX;
    @Getter
    public float initialPosY;

    @Expose
    @SerializedName("x")
    @Getter
    @Setter
    private float x;
    @Expose
    @SerializedName("y")
    @Getter
    @Setter
    private float y;

    private float offsetX, offsetY;
    @Getter public boolean isDragging;

    @Getter
    @Setter
    private float width, height;
    @Setter private ClickHandler clickHandler;
    @Getter @Setter private DraggableSettingsPanel settingsPanel;

    private Supplier<Boolean> activeCondition = () -> true;

    private transient CompactAnimation alphaAnimation;
    boolean freeMoveMode = false;
    boolean ctrlAltLatch = false;

    public DraggableHandler(String name, float initialPosX, float initialPosY) {
        this.name = name;
        this.x = initialPosX;
        this.y = initialPosY;
        this.initialPosX = initialPosX;
        this.initialPosY = initialPosY;
    }

    public void setActiveCondition(Supplier<Boolean> condition) {
        this.activeCondition = Objects.requireNonNullElse(condition, () -> true);
    }

    public boolean isActive() {
        return activeCondition == null || Boolean.TRUE.equals(activeCondition.get());
    }

    private static float snap(float v) {
        return (float) Math.floor(v + 0.5f);
    }

    public final void drawElement(DrawContext drawContext, Matrix4f matrix4f, int mouseX, int mouseY, Window res) {
        if (!isActive()) {
            isDragging = false;
            if (settingsPanel != null) {
                settingsPanel.close();
            }
            return;
        }

        Vec2i adjustedMouse = getMouseInteger(mouseX, mouseY);
        mouseX = adjustedMouse.getX();
        mouseY = adjustedMouse.getY();

        CompactAnimation alphaAnim = ensureAlphaAnimation();
        if (isDragging) alphaAnim.run(1.0);
        else alphaAnim.run(0.0);
        alphaAnim.update();

        double alphaProgress = alphaAnim.getValue();
        int alpha = (int) (255 * alphaProgress);

        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean altHeld = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        boolean ctrlHeld = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftHeld = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (isDragging && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            isDragging = false;
        }

        if (ctrlHeld && altHeld) {
            if (!ctrlAltLatch) {
                freeMoveMode = !freeMoveMode;
                ctrlAltLatch = true;
            }
        } else {
            ctrlAltLatch = false;
        }

        if (isDragging) {
            float effX = getEffectiveX();
            float effY = getEffectiveY();

            float newEffX = mouseX - offsetX;
            float newEffY = mouseY - offsetY;

            if (altHeld) newEffX = effX;
            if (shiftHeld) newEffY = effY;

            float screenWidth = res.getScaledWidth();
            float screenHeight = res.getScaledHeight();

            if (!freeMoveMode) {
                float gridSizeX = screenWidth / 10f;
                float gridSizeY = screenHeight / 10f;

                float centerX = newEffX + width / 2f;
                float centerY = newEffY + height / 2f;

                float nearestX = Math.round(centerX / gridSizeX) * gridSizeX - width / 2f;
                float nearestY = Math.round(centerY / gridSizeY) * gridSizeY - height / 2f;

                float snapThreshold = 6f;

                if (Math.abs(centerX - (nearestX + width / 2f)) < snapThreshold) newEffX = nearestX;
                if (Math.abs(centerY - (nearestY + height / 2f)) < snapThreshold) newEffY = nearestY;
            }

            if (newEffX + width > screenWidth) newEffX = screenWidth - width;
            if (newEffY + height > screenHeight) newEffY = screenHeight - height;
            if (newEffX < 0) newEffX = 0;
            if (newEffY < 0) newEffY = 0;

            setFromEffective(newEffX, newEffY);

            if (alpha > 0 && !freeMoveMode) {
                drawSetochka(matrix4f);
            }
        }

        if (alpha > 0) {
            String tipMain = "Ctrl + Alt свободное перемещение";
            String tipLock = "ALT — блок X, SHIFT — блок Y";

            float renderX = isPlayerInfoElement() ? getEffectiveXSafe() : x;
            float renderY = isPlayerInfoElement() ? getEffectiveYSafe() : y;

            float twMain = Builder.INTER.get().getWidth(tipMain, 8);
            float thMain = Builder.INTER.get().getHeight(tipMain, 8);
            float twLock = Builder.INTER.get().getWidth(tipLock, 8);
            float thLock = Builder.INTER.get().getHeight(tipLock, 8);

            int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
            float elemCenterY = renderY + Math.max(0f, height) / 2f;
            boolean placeBelow = elemCenterY < (screenH / 2f);
            float baseY = placeBelow ? (renderY + height + 12f) : (renderY - 6f - thMain);

            float textXMain = snap(renderX + (width - twMain) / 2f);
            float textYMain = snap(baseY);
            float textXLock = snap(renderX + (width - twLock) / 2f);
            float textYLock = snap(baseY - thLock - 2f);

            Builder.TEXT_BUILDER
                    .text(tipMain)
                    .size(8)
                    .color(ColorUtils.setAlpha(-1, alpha))
                    .font(Builder.INTER.get())
                    .build()
                    .render(drawContext.getMatrices().peek().getPositionMatrix(), textXMain, textYMain);

            Builder.TEXT_BUILDER
                    .text(tipLock)
                    .size(8)
                    .color(ColorUtils.setAlpha(-1, alpha))
                    .font(Builder.INTER.get())
                    .build()
                    .render(drawContext.getMatrices().peek().getPositionMatrix(), textXLock, textYLock);
        }
    }

    public final boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!isActive()) {
            if (settingsPanel != null) {
                settingsPanel.close();
            }
            return false;
        }

        boolean debug = MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud();

        boolean allowClickDebug = "Target Hud".equalsIgnoreCase(this.name)
                || "ServerItems".equalsIgnoreCase(this.name)
                || "MusicPlayer".equalsIgnoreCase(this.name);

        if (debug && !allowClickDebug) return false;

        Vec2i adjustedMouse = getMouseInteger((int) mouseX, (int) mouseY);
        mouseX = adjustedMouse.getX();
        mouseY = adjustedMouse.getY();

        float effX = getEffectiveXSafe();
        float effY = getEffectiveYSafe();

        if (MouseClick.isClick((float) mouseX, (float) mouseY, effX, effY, width, height)) {
            if (button == 0) {
                if (settingsPanel != null && settingsPanel.isOpen()) {
                    settingsPanel.closePreservingPosition();
                }
                isDragging = true;
                offsetX = (float) (mouseX - effX);
                offsetY = (float) (mouseY - effY);
                if (clickHandler != null && clickHandler.onClick((float) mouseX, (float) mouseY, button, this)) {
                    isDragging = false;
                }
                return true;
            } else if (button == 1 && settingsPanel != null && settingsPanel.hasSettings()) {
                DraggableManager.closeOtherPanels(this);
                settingsPanel.toggleAt((float) mouseX, (float) mouseY);
                return true;
            } else {
                if (settingsPanel != null && settingsPanel.isOpen()) {
                    if (settingsPanel.mouseClicked(mouseX, mouseY, button)) return true;
                }
                if (clickHandler != null) {
                    return clickHandler.onClick((float) mouseX, (float) mouseY, button, this);
                }
                return false;
            }
        }
        return false;
    }

    public final void onMouseRelease(int button) {
        if (button == 0) isDragging = false;
    }

    private void drawSetochka(Matrix4f matrix4f) {
        int gridSize = 10;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        double alphaProgress = ensureAlphaAnimation().getValue();
        float opacity = (float) (alphaProgress * 85f);

        for (int i = 1; i < gridSize; i++) {
            float x = i * (screenWidth / (float) gridSize);
            renderLine(matrix4f, x - 0.75f, 0, 1.5f, screenHeight, opacity);
        }

        for (int j = 1; j < gridSize; j++) {
            float y = j * (screenHeight / (float) gridSize);
            renderLine(matrix4f, 0, y - 0.75f, screenWidth, 1.5f, opacity);
        }
    }

    void renderLine(Matrix4f matrix4f, float x, float y, float width, float height, float opacity) {
        ClientApi.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(ColorUtils.rgba(255, 255, 255, (int) opacity)))
                .build()
                .render(matrix4f, x, y);
    }

    private CompactAnimation ensureAlphaAnimation() {
        if (alphaAnimation == null) {
            alphaAnimation = new CompactAnimation(Easing.EASE_OUT_QUAD, 300);
        }
        return alphaAnimation;
    }

    private boolean isPlayerInfoElement() {
        return "PlayerInfo".equalsIgnoreCase(this.name);
    }

    private boolean piAlignRightByStored() {
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return this.x > (screenW / 2.0f);
    }

    private boolean piAlignBottomByStored() {
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        return this.y > (screenH / 2.0f);
    }

    private boolean piAlignRightByEff(float effX) {
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return effX + width * 0.5f > (screenW * 0.5f);
    }

    private boolean piAlignBottomByEff(float effY) {
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        return effY + height * 0.5f > (screenH * 0.5f);
    }

    public float getRenderX() {
        return getEffectiveXSafe();
    }

    public float getRenderY() {
        return getEffectiveYSafe();
    }

    public float getRenderRight() {
        return getRenderX() + width;
    }

    public float getRenderBottom() {
        return getRenderY() + height;
    }

    private float getEffectiveXSafe() {
        if (!isPlayerInfoElement() || width <= 0f) return x;
        return piAlignRightByStored() ? (x - width) : x;
    }

    private float getEffectiveYSafe() {
        if (!isPlayerInfoElement() || height <= 0f) return y;
        return piAlignBottomByStored() ? (y - height) : y;
    }

    private float getEffectiveX() {
        if (!isPlayerInfoElement()) return x;
        return piAlignRightByStored() ? (x - width) : x;
    }

    private float getEffectiveY() {
        if (!isPlayerInfoElement()) return y;
        return piAlignBottomByStored() ? (y - height) : y;
    }

    private void setFromEffective(float effX, float effY) {
        if (!isPlayerInfoElement()) {
            this.x = effX;
            this.y = effY;
            return;
        }
        boolean right = piAlignRightByEff(effX);
        boolean bottom = piAlignBottomByEff(effY);
        this.x = right ? (effX + width) : effX;
        this.y = bottom ? (effY + height) : effY;
    }

    public void resetPosition() {
        this.x = this.initialPosX;
        this.y = this.initialPosY;
    }

    @FunctionalInterface
    public interface ClickHandler {
        boolean onClick(float mouseX, float mouseY, int button, DraggableHandler self);
    }
}
