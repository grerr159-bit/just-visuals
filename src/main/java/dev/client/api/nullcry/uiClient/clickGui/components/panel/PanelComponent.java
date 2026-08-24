package dev.client.api.nullcry.uiClient.clickGui.components.panel;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.client.Animation;
import dev.client.api.nullcry.render.core.animations.client.Direction;
import dev.client.api.nullcry.render.core.animations.client.implement.DecelerateAnimation;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.BackgroundComponent;
import dev.client.api.nullcry.uiClient.clickGui.api.Helper;
import dev.client.api.nullcry.uiClient.clickGui.api.component.Component;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.api.nullcry.uiClient.clickGui.components.settings.ModuleComponent;
import dev.client.modules.core.render.Interface;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PanelComponent extends Component {
    List<ModuleComponent> componentsList = new ArrayList<>();
    BackgroundComponent backgroundComponent;

    @NonFinal String currentFilter = "";
    @NonFinal float scroll, animationScroll = 0f;
    @NonFinal final ModuleCategory category;
    @NonFinal float revealProgress = 1f;
    @NonFinal boolean collapsed;
    @NonFinal float collapseProgress = 1f;
    @NonFinal float collapseTarget = 1f;

    private static final float SCROLLBAR_MIN_HEIGHT = 16f;
    private static final float SCROLLBAR_WIDTH = 2.5f;
    private static final float SCROLLBAR_MARGIN = 3.5f;
    private static final float SCROLLBAR_INTERACTION_PADDING = 2.5f;

    @NonFinal boolean scrollbarVisible = false;
    @NonFinal boolean scrollbarDragging = false;
    @NonFinal float scrollbarX = 0f;
    @NonFinal float scrollbarY = 0f;
    @NonFinal float scrollbarHeight = 0f;
    @NonFinal float scrollbarTrackY = 0f;
    @NonFinal float scrollbarTrackHeight = 0f;
    @NonFinal float scrollbarMaxScroll = 0f;
    @NonFinal float scrollbarGrabOffset = 0f;

    private static final float COLLAPSED_HEADER_HEIGHT = 22f;
    private static final float EXPANDED_HEADER_HEIGHT = 28f;
    private static final float PADDING_TOP = 1f;
    private static final float PADDING_SIDE = 2.5f;
    private static final float ROW_GAP = 2.5f;
    private static final float BASE_MODULE_WIDTH = 136f;
    private final Animation descriptionAnimation = new DecelerateAnimation().setMs(220).setValue(1f);
    public static String hoveredDescription = null;

    private static final Map<ModuleCategory, Float> SCROLL_CACHE = new EnumMap<>(ModuleCategory.class);
    private static final Map<ModuleCategory, Boolean> COLLAPSE_CACHE = new EnumMap<>(ModuleCategory.class);

    public PanelComponent(ModuleCategory moduleCategory) {
        this.category = moduleCategory;
        backgroundComponent = new BackgroundComponent();
        componentsList.addAll(Helper.moduleLayers(moduleCategory, (e) -> true));
        float savedScroll = SCROLL_CACHE.getOrDefault(moduleCategory, 0f);
        this.scroll = savedScroll;
        this.animationScroll = savedScroll;
        this.collapsed = COLLAPSE_CACHE.getOrDefault(moduleCategory, Boolean.FALSE);
        this.collapseProgress = this.collapseTarget = collapsed ? 0f : 1f;
        if (collapsed) {
            this.scroll = 0f;
            this.animationScroll = 0f;
        }
    }

    public PanelComponent revealProgress(float progress) {
        this.revealProgress = MathHelper.clamp(progress, 0f, 1f);
        return this;
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        collapseTarget = collapsed ? 0f : 1f;
        scrollbarDragging = false;
        scrollbarGrabOffset = 0f;
        COLLAPSE_CACHE.put(category, collapsed);
        if (collapsed) {
            SCROLL_CACHE.put(category, scroll);
            scroll = 0f;
            animationScroll = 0f;
        } else {
            float savedScroll = SCROLL_CACHE.getOrDefault(category, 0f);
            scroll = savedScroll;
            animationScroll = savedScroll;
        }
    }

    @Override
    public PanelComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        collapseProgress = MathHelper.lerp(0.18f, collapseProgress, collapseTarget);
        if (Math.abs(collapseProgress - collapseTarget) <= 0.001f) {
            collapseProgress = collapseTarget;
        }

        animationScroll = MathHelper.lerp(0.2f, animationScroll, scroll);

        float headerVisibility = revealProgress;
        float contentVisibility = revealProgress * collapseProgress;
        var matrix = context.getMatrices().peek().getPositionMatrix();

        componentsList.forEach(e -> {
            if (e instanceof ModuleComponent module) {
                module.setGlobalAlpha(contentVisibility);
            }
        });

        hoveredDescription = null;
        if (collapseProgress > 0.01f) {
            for (ModuleComponent moduleComponent : componentsList) {
                if (MouseClick.isClick(mouseX, mouseY, moduleComponent.getX(), moduleComponent.getY(), moduleComponent.getWidth(), 20)) {
                    String description = moduleComponent.getModule().getDescription();
                    if (description != null && !description.isEmpty()) {
                        hoveredDescription = description;
                        break;
                    }
                }
            }
        }

        if (hoveredDescription != null && collapseProgress > 0.01f) {
            if (descriptionAnimation.getDirection() != Direction.FORWARDS) {
                descriptionAnimation.setDirection(Direction.FORWARDS);
                descriptionAnimation.reset();
            }
        } else {
            if (descriptionAnimation.getDirection() != Direction.BACKWARDS) {
                descriptionAnimation.setDirection(Direction.BACKWARDS);
                descriptionAnimation.reset();
            }
        }

        float animProgress = descriptionAnimation.getOutput().floatValue();
        if (animProgress > 0.001f && contentVisibility > 0.001f) {
            float screenWidth = context.getScaledWindowWidth();
            float tooltipWidth = ClientApi.inter().getWidth(hoveredDescription != null ? hoveredDescription : "", 12f) + 14f;
            float tooltipHeight = 14f;
            float tooltipX = (screenWidth - tooltipWidth) / 2f;
            float tooltipY = getY() - tooltipHeight - 30f;

            int alpha = (int) (255 * animProgress * contentVisibility);
            int color = ColorUtils.setAlpha(-1, alpha);

            float dropOffset = MathHelper.lerp(animProgress, 6f, 0f);
            float scale = MathHelper.lerp(animProgress, 0.95f, 1f);

            context.getMatrices().push();
            context.getMatrices().translate(tooltipX + tooltipWidth / 2f, tooltipY + tooltipHeight / 2f, 0f);
            context.getMatrices().scale(scale, scale, 1f);
            context.getMatrices().translate(-(tooltipX + tooltipWidth / 2f), -(tooltipY + tooltipHeight / 2f), 0f);
            context.getMatrices().translate(0f, dropOffset, 0f);

            if (hoveredDescription != null) {
                ClientApi.text()
                        .size(12f)
                        .font(ClientApi.inter())
                        .color(color)
                        .text(hoveredDescription)
                        .build()
                        .render(matrix, tooltipX + 5f, tooltipY + 2.5f);
            }

            context.getMatrices().pop();
        }

        float headerHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, EXPANDED_HEADER_HEIGHT);
        float totalHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, getHeight());

        backgroundComponent.position(getX(), getY()).size(getWidth(), totalHeight);
        backgroundComponent.setRevealProgress(revealProgress);
        backgroundComponent.render(context, mouseX, mouseY, delta);

        String categoryName = category.name();
        char iconChar = category.getIcon();

        float titleFontSize = 9f;
        float iconFontSize = 10f;

        float titleHeight = ClientApi.inter().getHeight(categoryName, titleFontSize);
        float iconHeight = iconChar != 0 ? ClientApi.otherIcons().getHeight(String.valueOf(iconChar), iconFontSize) : 0f;

        float headerCenterY = getY() + headerHeight / 2f;
        float startX = getX() + 8f;

        if (iconChar != 0 && headerVisibility > 0.001f) {
            String iconText = String.valueOf(iconChar);
            int iconColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (headerVisibility * 255f));

            ClientApi.text()
                    .size(iconFontSize)
                    .font(ClientApi.otherIcons())
                    .color(iconColor)
                    .text(iconText)
                    .build()
                    .render(matrix, startX, headerCenterY - iconHeight / 2f + 0.5f);
        }

        float iconWidth = iconChar != 0 ? ClientApi.otherIcons().getWidth(String.valueOf(iconChar), iconFontSize) : 0f;
        float spacingAfterIcon = MathHelper.clamp(iconWidth * 0.9f, 4f, 6f);

        float separatorX = Math.round(startX + iconWidth + spacingAfterIcon);
        float separatorHeight = 9f;
        float separatorY = Math.round(headerCenterY - separatorHeight / 2f);

        int separatorBaseColor = Interface.INSTANCE.getMainColor();
        int separatorColor = ColorUtils.setAlpha(separatorBaseColor, (int) (headerVisibility * 255f));

        ClientApi.rectangle()
                .size(new SizeState(1f, separatorHeight))
                .color(new QuadColorState(separatorColor))
                .radius(new QuadRadiusState(0f))
                .build()
                .render(matrix, separatorX, separatorY);

        float titleX = separatorX + 6.5f;
        float titleY = headerCenterY - titleHeight / 2f - 1;
        int titleColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (headerVisibility * 255f));
        if (headerVisibility > 0.001f) {
            ClientApi.text()
                    .size(titleFontSize)
                    .font(ClientApi.inter())
                    .color(titleColor)
                    .text(categoryName)
                    .build()
                    .render(matrix, titleX, titleY);
        }

        char arrowChar = collapsed ? 'H' : 'I';
        String arrowText = String.valueOf(arrowChar);
        float arrowFontSize = 9f;
        float arrowWidth = ClientApi.otherIcons().getWidth(arrowText, arrowFontSize);
        float arrowHeight = ClientApi.otherIcons().getHeight(arrowText, arrowFontSize);
        float arrowPadding = 4f;
        float arrowX = getX() + getWidth() - arrowWidth - 8f;
        float arrowY = headerCenterY - arrowHeight / 2f;
        boolean arrowHovered = headerVisibility > 0.001f && MouseClick.isClick(mouseX, mouseY, arrowX - arrowPadding / 2f, getY(), arrowWidth + arrowPadding, headerHeight);
        if (arrowHovered) {
            CursorsUtil.setCursor(CursorsUtil.HAND);
        }

        int arrowColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (headerVisibility * 255f));
        if (headerVisibility > 0.001f) {
            ClientApi.text()
                    .size(arrowFontSize)
                    .font(ClientApi.otherIcons())
                    .color(arrowColor)
                    .text(arrowText)
                    .build()
                    .render(matrix, arrowX, arrowY);
        }

        float availableWidth = getWidth() - PADDING_SIDE * 2f;
        float innerW = Math.min(availableWidth, BASE_MODULE_WIDTH);
        float innerX = getX() + (getWidth() - innerW) / 2f;
        float listTopY = getY() + headerHeight - 1f;
        float scissorHeight = Math.max(0f, totalHeight - (listTopY - getY()) - 3f);

        boolean scissorActive = false;
        if (contentVisibility > 0.001f && collapseProgress > 0.01f && scissorHeight > 0.5f) {
            ScissorUtil.enable(innerX, listTopY, innerW, scissorHeight);
            scissorActive = true;

            final AtomicReference<Float> offset = new AtomicReference<>(PADDING_TOP);
            componentsList.forEach(e -> {
                float height = e.getFullHeight();
                e.getComponents().forEach(SettingComponent::init);

                e.position(innerX, listTopY + offset.get() + animationScroll)
                        .size(innerW, height)
                        .render(context, mouseX, mouseY, delta);
                offset.set(offset.get() + height + ROW_GAP);
            });
        }

        if (scissorActive) {
            ScissorUtil.disable();
        }

        float contentHeight = PADDING_TOP + (componentsList.isEmpty() ? 0f : componentsList.stream().map(ModuleComponent::getFullHeight).reduce(0f, Float::sum) + ROW_GAP * (componentsList.size() - 1));
        float headerOffset = headerHeight - 1f;
        float viewableHeight = Math.max(0f, totalHeight - headerOffset - 3f - PADDING_TOP);
        float maxScroll = Math.max(0f, contentHeight - viewableHeight);
        float trackTop = listTopY + PADDING_TOP;
        float trackHeight = Math.max(0f, viewableHeight);
        boolean allowScroll = collapseProgress > 0.01f && contentVisibility > 0.001f;

        if (allowScroll && maxScroll > 0.5f && trackHeight > 0.5f) {
            float computedHeight = Math.max(SCROLLBAR_MIN_HEIGHT, trackHeight * (trackHeight / (contentHeight + 0.0001f)));
            computedHeight = Math.min(computedHeight, trackHeight);
            float trackRange = Math.max(0f, trackHeight - computedHeight);

            if (scrollbarDragging) {
                if (trackRange <= 0.0001f || maxScroll <= 0.0001f) {
                    scrollbarDragging = false;
                    scrollbarGrabOffset = 0f;
                } else {
                    float dragY = (float) mouseY - scrollbarGrabOffset;
                    float clampedY = MathHelper.clamp(dragY, trackTop, trackTop + trackRange);
                    float progress = (clampedY - trackTop) / trackRange;
                    scroll = -progress * maxScroll;
                    animationScroll = scroll;
                }
            }

            scroll = MathHelper.clamp(scroll, -maxScroll, 0f);

            float scrollProgress = maxScroll <= 0.0001f ? 0f : (-scroll) / maxScroll;
            scrollProgress = MathHelper.clamp(scrollProgress, 0f, 1f);

            float scrollbarYPos = trackTop + trackRange * scrollProgress;
            float scrollbarXPos = getX() + getWidth() - SCROLLBAR_MARGIN - SCROLLBAR_WIDTH;

            boolean hovered = mouseX >= scrollbarXPos - SCROLLBAR_INTERACTION_PADDING
                    && mouseX <= scrollbarXPos + SCROLLBAR_WIDTH + SCROLLBAR_INTERACTION_PADDING
                    && mouseY >= scrollbarYPos - SCROLLBAR_INTERACTION_PADDING
                    && mouseY <= scrollbarYPos + computedHeight + SCROLLBAR_INTERACTION_PADDING;

            if (hovered || scrollbarDragging) {
                CursorsUtil.setCursor(CursorsUtil.HAND);
            }

            int alphaBase = hovered || scrollbarDragging ? 200 : 160;
            int color = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), (int) (alphaBase * contentVisibility));

            ClientApi.rectangle()
                    .size(new SizeState(SCROLLBAR_WIDTH, computedHeight))
                    .color(new QuadColorState(color))
                    .radius(new QuadRadiusState(1f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), scrollbarXPos, scrollbarYPos);

            scrollbarVisible = true;
            scrollbarX = scrollbarXPos;
            scrollbarY = scrollbarYPos;
            scrollbarHeight = computedHeight;
            scrollbarTrackY = trackTop;
            scrollbarTrackHeight = trackHeight;
            scrollbarMaxScroll = maxScroll;
        } else {
            scroll = MathHelper.clamp(scroll, -maxScroll, 0f);
            if (scrollbarDragging) {
                scrollbarDragging = false;
                scrollbarGrabOffset = 0f;
            }
            scrollbarVisible = false;
            scrollbarHeight = 0f;
            scrollbarTrackY = trackTop;
            scrollbarTrackHeight = trackHeight;
            scrollbarMaxScroll = maxScroll;
        }

        if (collapseTarget <= 0.001f) {
            scroll = 0f;
            animationScroll = 0f;
        } else {
            SCROLL_CACHE.put(category, scroll);
        }
        return null;
    }

    public void updateFilter(String searchText) {
        this.currentFilter = searchText == null ? "" : searchText.toLowerCase();
        this.scroll = 0;
        this.animationScroll = 0;
        SCROLL_CACHE.put(category, 0f);

        List<ModuleComponent> filteredModules = Helper.moduleLayers(category, module -> {
            String name = module.getName();
            return name != null && name.toLowerCase().contains(currentFilter);
        });

        componentsList.clear();
        this.componentsList.addAll(filteredModules);

        filteredModules.forEach(moduleComponent -> {
            moduleComponent.getComponents().forEach(SettingComponent::init);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float headerHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, EXPANDED_HEADER_HEIGHT);
        String arrowText = String.valueOf(collapsed ? 'G' : 'H');
        float arrowFontSize = 9f;
        float arrowWidth = ClientApi.otherIcons().getWidth(arrowText, arrowFontSize);
        float arrowPadding = 4f;
        float arrowX = getX() + getWidth() - arrowWidth - 8f;

        if (button == 0 && scrollbarVisible && scrollbarHeight > 0.001f && isPointInScrollbar(mouseX, mouseY)) {
            scrollbarDragging = true;
            scrollbarGrabOffset = MathHelper.clamp((float) (mouseY - scrollbarY), 0f, scrollbarHeight);
            animationScroll = scroll;
            return true;
        }

        if (MouseClick.isClick(mouseX, mouseY, arrowX - arrowPadding / 2f, getY(), arrowWidth + arrowPadding, headerHeight)) {
            if (button == 0 || button == 1) {
                toggleCollapse();
            }
            return true;
        }

        float panelHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, getHeight());
        if (collapseProgress > 0.01f && MouseClick.isClick(mouseX, mouseY, getX(), getY(), getWidth(), panelHeight)) {
            componentsList.forEach(e -> e.mouseClicked(mouseX, mouseY, button));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (collapseProgress > 0.01f) {
            componentsList.forEach(e -> e.mouseReleased(mouseX, mouseY, button));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (collapseProgress <= 0.01f) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        float headerHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, EXPANDED_HEADER_HEIGHT);
        float scissorX = getX();
        float headerOffset = headerHeight - 1f;
        float scissorY = getY() + headerOffset;
        float scissorW = getWidth();
        float scissorH = Math.max(0f, MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, getHeight()) - headerOffset - 3f);

        if (MouseClick.isClick(mouseX, mouseY, scissorX, scissorY, scissorW, scissorH)) {
            scroll += (float) (verticalAmount * 20f);

            float contentHeight = PADDING_TOP + (componentsList.isEmpty() ? 0f : componentsList.stream().map(ModuleComponent::getFullHeight).reduce(0f, Float::sum) + ROW_GAP * (componentsList.size() - 1));
            float totalHeight = MathHelper.lerp(collapseProgress, COLLAPSED_HEADER_HEIGHT, getHeight());
            float viewableHeight = Math.max(0f, totalHeight - headerOffset - 3f - PADDING_TOP);
            float maxScroll = Math.max(0f, contentHeight - viewableHeight);
            scroll = MathHelper.clamp(scroll, -maxScroll, 0f);
            SCROLL_CACHE.put(category, scroll);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean isPointInScrollbar(double mouseX, double mouseY) {
        if (!scrollbarVisible || scrollbarHeight <= 0f) {
            return false;
        }
        float pad = SCROLLBAR_INTERACTION_PADDING;
        float width = SCROLLBAR_WIDTH;
        return mouseX >= scrollbarX - pad && mouseX <= scrollbarX + width + pad
                && mouseY >= scrollbarY - pad && mouseY <= scrollbarY + scrollbarHeight + pad;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        componentsList.forEach(e -> e.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        componentsList.forEach(e -> e.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        componentsList.forEach(e -> e.keyReleased(keyCode, scanCode, modifiers));
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
