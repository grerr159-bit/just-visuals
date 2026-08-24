package dev.client.api.nullcry.uiClient.clickGui;

import com.google.common.eventbus.Subscribe;
import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.client.Animation;
import dev.client.api.nullcry.render.core.animations.client.Direction;
import dev.client.api.nullcry.render.core.animations.client.implement.DecelerateAnimation;
import dev.client.api.nullcry.uiClient.clickGui.api.component.Component;
import dev.client.api.nullcry.uiClient.clickGui.components.panel.PanelsLayer;
import dev.client.api.nullcry.uiClient.clickGui.components.search.SearchBarComponent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClickGuiScreen extends Screen implements ClientApi {
    List<Component> componentsList = new ArrayList<>();
    SearchBarComponent searchBar = new SearchBarComponent();
    @Getter PanelsLayer panelsLayer = new PanelsLayer();

    Animation revealAnimation = new DecelerateAnimation().setMs(420).setValue(1f);
    Animation fadeAnimation = new DecelerateAnimation().setMs(320).setValue(1f);
    public static boolean closing = false;

    final float width = 650f;
    final float height = 550f / 2;

    Supplier<Float> x = () -> (mc.getWindow().getScaledWidth() - width) / 2;
    Supplier<Float> y = () -> (mc.getWindow().getScaledHeight() - height) / 2;

    public ClickGuiScreen() {
        super(Text.of("Just.click_gui"));

        componentsList.addAll(List.of(
                panelsLayer,
                searchBar
        ));

        Just.getInstance().getEventBus().register(this);
    }

    @Override
    protected void init() {
        componentsList.forEach(Component::init);
        panelsLayer.position(x.get(), y.get()).size(width, height);
        searchBar.position((mc.getWindow().getScaledWidth() - 125) / 2f, y.get() + height + 8f).size(125, 23f);

        closing = false;
        revealAnimation.setDirection(Direction.FORWARDS);
        fadeAnimation.setDirection(Direction.FORWARDS);
        revealAnimation.reset();
        fadeAnimation.reset();

        super.init();
    }

    @Override
    public void close() {
        if (!closing) {
            startClosing();
            return;
        }

        finishClose();
    }

    private void startClosing() {
        closing = true;
        revealAnimation.setDirection(Direction.BACKWARDS);
        fadeAnimation.setDirection(Direction.BACKWARDS);
        revealAnimation.reset();
        fadeAnimation.reset();
    }

    private void finishClose() {
        closing = false;
        mc.setScreen(null);
        searchBar.setListening(false);
        CursorsUtil.resetState();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CursorsUtil.beginFrame();

        float revealProgress = revealAnimation.getOutput().floatValue();
        float fadeProgress = fadeAnimation.getOutput().floatValue();
        float combinedProgress = MathHelper.clamp(revealProgress * fadeProgress, 0f, 1f);

        panelsLayer.setRevealProgress(combinedProgress);
        searchBar.setRevealProgress(combinedProgress);
        searchBar.setListener(panelsLayer::updateFilter);

        float scale = MathHelper.lerp(combinedProgress, 1.06f, 1f);
        float centerX = x.get() + width / 2f;
        float centerY = y.get() + height / 2f;

        ScissorUtil.setTransform(scale, centerX, centerY);

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0f);

        if (combinedProgress > 0.01f) {
            componentsList.forEach(e -> e.render(context, mouseX, mouseY, delta));
        }

        context.getMatrices().pop();

        ScissorUtil.resetTransform();

        super.render(context, mouseX, mouseY, delta);
        CursorsUtil.applyFrameCursor();
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && revealAnimation.isFinished(Direction.BACKWARDS)) {
            finishClose();
        }
    }

    @Subscribe
    public void keyListener(KeyBindEvent event) {
        if (Objects.isNull(mc.currentScreen) && event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT)
            mc.setScreen(Just.getInstance().getClickGuiScreen());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        componentsList.forEach(e -> e.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (closing) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        componentsList.forEach(e -> e.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        componentsList.forEach(e -> e.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (closing) {
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        componentsList.forEach(e -> e.keyReleased(keyCode, scanCode, modifiers));
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing) {
            return super.charTyped(chr, modifiers);
        }
        componentsList.forEach(e -> e.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        componentsList.forEach(e -> e.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
