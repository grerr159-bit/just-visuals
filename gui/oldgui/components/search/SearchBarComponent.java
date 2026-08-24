package dev.client.api.nullcry.uiClient.clickGui.components.search;

import dev.client.Lumi;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.component.Component;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.modules.core.render.Interface;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class SearchBarComponent extends Component {
    private final StringBuilder searchText = new StringBuilder();

    int cursorPosition = 0;
    boolean isFocused = false;
    boolean isSelectedAll = false;
    long lastBlink = System.currentTimeMillis();
    boolean showCursor = true;

    long lastDotTick = System.currentTimeMillis();
    int dotsCount = 0;

    @Setter
    private SearchUpdateListener listener;
    private float revealProgress = 1f;

    public void setRevealProgress(float revealProgress) {
        this.revealProgress = MathHelper.clamp(revealProgress, 0f, 1f);
    }

    @Override
    public SearchBarComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        float padding = 5f;

        if (isFocused && System.currentTimeMillis() - lastBlink > 500) {
            showCursor = !showCursor;
            lastBlink = System.currentTimeMillis();
        }

        if (!isFocused && searchText.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastDotTick >= 1000) {
                dotsCount = (dotsCount % 3) + 1;
                lastDotTick = now;
            }
        } else {
            dotsCount = Math.min(dotsCount, 3);
        }

        HelperElements.rectGui(context, getX(), getY(), getWidth(), getHeight(), revealProgress);

        String baseText = searchText.toString();
        int safeCursor = Math.max(0, Math.min(cursorPosition, baseText.length()));
        String leftText = baseText.substring(0, safeCursor);
        String rightText = baseText.substring(safeCursor);

        String displayText;
        if (isFocused) {
            displayText = leftText + (showCursor ? "_" : "") + rightText;
            if (displayText.isEmpty()) displayText = " ";
        } else {
            if (baseText.isEmpty()) {
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < dotsCount; i++) dots.append('.');
                displayText = "Search" + dots;
            } else {
                displayText = baseText;
            }
        }

        float contentPaddingX = getX() + padding + 2f;
        float textY = getY() + 7;
        float textWidth = ClientApi.inter().getWidth(leftText, 8f);
        float iconWidth = ClientApi.otherIcons().getWidth("F", 10f);
        boolean hideDecorations = isFocused || !baseText.isEmpty();
        float contentStartX = contentPaddingX;

        if (!hideDecorations) {
            ClientApi.text()
                    .text("F")
                    .size(10f)
                    .font(ClientApi.otherIcons())
                    .color(ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (150 * revealProgress)))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), contentPaddingX, textY);

            float spacingAfterIcon = MathHelper.clamp(iconWidth * 0.5f, 3f, 4f);
            float separatorX = Math.round(contentPaddingX + iconWidth + spacingAfterIcon);
            float separatorHeight = 9f;
            float separatorY = getY() + (getHeight() - separatorHeight) / 2f + 0.5f;

            int separatorColor = ColorUtils.setAlpha(
                    Interface.INSTANCE.getMainColor(),
                    (int) (255 * revealProgress)
            );

            ClientApi.rectangle()
                    .size(new SizeState(1f, separatorHeight))
                    .color(new QuadColorState(separatorColor))
                    .radius(new QuadRadiusState(1f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), separatorX, separatorY);

            contentStartX = separatorX + 3;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Vector3f topLeft = matrix.transformPosition(getX() + 1f, getY() + 1f, 0f, new Vector3f());
        Vector3f bottomRight = matrix.transformPosition(getX() + getWidth() - 2f, getY() + getHeight() - 1f, 0f, new Vector3f());

        float scissorX = Math.min(topLeft.x, bottomRight.x);
        float scissorY = Math.min(topLeft.y, bottomRight.y);
        float scissorWidth = Math.abs(bottomRight.x - topLeft.x);
        float scissorHeight = Math.abs(bottomRight.y - topLeft.y);

        ScissorUtil.enable(scissorX, scissorY, scissorWidth, scissorHeight);

        float textRenderX = contentStartX + (hideDecorations ? 0f : 4f);

        if (isFocused && isSelectedAll && !baseText.isEmpty()) {
            ClientApi.rectangle()
                    .size(new SizeState(textWidth + 3, getHeight() - 8))
                    .color(new QuadColorState(ColorUtils.rgba(50, 100, 200, (int) (100 * revealProgress))))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), textRenderX - 0.5f, textY - 2);
        }

        int textColor = isFocused
                ? ColorUtils.setAlpha(-1, (int) (200 * revealProgress))
                : ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (150 * revealProgress));

        ClientApi.text()
                .text(displayText)
                .size(8f)
                .font(ClientApi.inter())
                .color(textColor)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), textRenderX, textY);

        ScissorUtil.disable();
        cursorPosition = Math.max(0, Math.min(cursorPosition, searchText.length()));
        return this;
    }

    public void setListening(boolean listening) {
        this.isFocused = listening;
        Lumi.getInstance().setFocusedRender(listening);
        if (!listening) {
            this.isSelectedAll = false;
            this.cursorPosition = searchText.length();
        }
    }

    public String getSearchText() {
        return searchText.toString().toLowerCase();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean focused = MouseClick.isClick(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        setListening(focused);

        if (isFocused) {
            isSelectedAll = false;
            cursorPosition = searchText.length();
        }
        return isFocused || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!isFocused) return super.charTyped(chr, modifiers);

        if (Character.isLetterOrDigit(chr) || Character.isWhitespace(chr)) {
            if (isSelectedAll) {
                searchText.setLength(0);
                isSelectedAll = false;
                cursorPosition = 0;
            }

            int safeCursor = Math.max(0, Math.min(cursorPosition, searchText.length()));
            searchText.insert(safeCursor, chr);
            cursorPosition = safeCursor + 1;

            if (listener != null) listener.onSearchUpdate(getSearchText());
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_F) {
            setListening(true);
            isSelectedAll = false;
            return true;
        }

        if (isFocused && ctrl && keyCode == GLFW.GLFW_KEY_A) {
            isSelectedAll = true;
            return true;
        }

        if (isFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    if (cursorPosition > 0) cursorPosition--;
                    isSelectedAll = false;
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (cursorPosition < searchText.length()) cursorPosition++;
                    isSelectedAll = false;
                    return true;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (isSelectedAll) {
                        searchText.setLength(0);
                        cursorPosition = 0;
                        isSelectedAll = false;
                    } else if (cursorPosition > 0 && !searchText.isEmpty()) {
                        searchText.deleteCharAt(cursorPosition - 1);
                        cursorPosition--;
                    }
                    if (listener != null) listener.onSearchUpdate(getSearchText());
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    cursorPosition = 0;
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    cursorPosition = searchText.length();
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
