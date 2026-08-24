package dev.client.api.nullcry.uiClient.clickGui.newgui.render;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.DrawUtil;
import dev.client.api.nullcry.render.VertexUtils;
import net.minecraft.client.MinecraftClient;
import dev.client.api.nullcry.uiClient.clickGui.newgui.GuiState;
import dev.client.api.nullcry.uiClient.clickGui.newgui.FriendsStorage;
import dev.client.api.nullcry.uiClient.clickGui.newgui.LocationStorage;
import dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Identifier;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

/**
 * Главный рендерер GUI
 */
public class GuiRenderer {
    private static final ModuleCategory[] CACHED_CATEGORIES = ModuleCategory.values();
    private static int[] cachedGradient = null;
    private static int cachedGradientBaseColor = 0;
    private static float cachedGradientAlpha = 0f;

    private static final Identifier LOGO = Identifier.of("just", "images/guilogo.png");

    public static String friendInput = "";
    public static boolean friendInputFocused = false;
    private static final float[] removeHoverAnim = new float[64];
    private static int lastHoverIndex = -1;
    public static String locationInput = "";
    public static boolean locationInputFocused = false;
    public static String locationXInput = "";
    public static boolean locationXFocused = false;
    public static String locationYInput = "";
    public static boolean locationYFocused = false;
    public static String locationZInput = "";
    public static boolean locationZFocused = false;
    public static int locationColorPick = 0xFF00FF00;
    private static final float[] locationRemoveHoverAnim = new float[64];
    public static int bindingWaypointIndex = -1;
    
    public static void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float mainAlpha = GuiState.mainAnimation.getOutput().floatValue();
        
        if (mainAlpha <= 0.001f) return;
        
        // Обновляем позицию (центрирование)
        float scaledWidth = GuiState.mc.getWindow().getScaledWidth();
        float scaledHeight = GuiState.mc.getWindow().getScaledHeight();
        GuiState.setX(scaledWidth / 2f - GuiState.getWidth() / 2f);
        GuiState.setY(scaledHeight / 2f - GuiState.getHeight() / 2f);
        
        // Затемнение фона
        int bgDarkness = ColorUtils.rgba(0, 0, 0, (int)(140 * mainAlpha));
        RenderHelper.drawRect(context, 0, 0, scaledWidth, scaledHeight, bgDarkness);
        
        // Рендерим компоненты
        renderBackground(context, mainAlpha);
        renderTopPanel(context, mainAlpha);
        renderLeftPanel(context, mainAlpha);
        renderMainPanel(context, mouseX, mouseY, mainAlpha);
        renderUserInfo(context, mainAlpha);
    }
    
    /**
     * Рендер фона GUI
     */
    private static void renderBackground(DrawContext context, float alpha) {
        float x = GuiState.getX();
        float y = GuiState.getY();
        float width = GuiState.getWidth();
        float height = GuiState.getHeight();
        
        int mainColor = Interface.INSTANCE.getMainColor();
        int outlineColor = ColorUtils.setAlpha(mainColor, (int)(20 * alpha));
        int backgroundColor = ColorUtils.rgba(20, 20, 25, (int)(178 * alpha));
        
        RenderHelper.drawRoundedRectOutline(context, x, y, width, height, 6.5f, 0.5f, outlineColor);
        RenderHelper.drawRoundedRect(context, x, y, width, height, 6.5f, backgroundColor);
    }
    
    /**
     * Рендер верхней панели с логотипом и названием
     */
    private static void renderTopPanel(DrawContext context, float alpha) {
        float x = GuiState.getX();
        float y = GuiState.getY();
        float width = GuiState.getWidth();
        float topHeight = 33.7f;
        
        // Логотип
        float logoSize = 56f;
        float logoX = x + 10f;
        float logoY = y + (topHeight - logoSize) / 2f;
        
        int mainColor = Interface.INSTANCE.getMainColor();
        int logoColor = ColorUtils.setAlpha(mainColor, (int)(255 * alpha));
        
        // Рендерим текстуру с цветом через кастомный RenderLayer
        RenderSystem.setShaderTexture(0, LOGO);
        RenderSystem.enableBlend();
        
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        VertexConsumer vertices = GuiState.mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(VertexUtils.IMAGE);
        
        float x1 = logoX;
        float y1 = logoY;
        float x2 = logoX + logoSize;
        float y2 = logoY + logoSize;
        
        vertices.vertex(matrix, x1, y1, 0).texture(0f, 0f).color(logoColor);
        vertices.vertex(matrix, x1, y2, 0).texture(0f, 1f).color(logoColor);
        vertices.vertex(matrix, x2, y2, 0).texture(1f, 1f).color(logoColor);
        vertices.vertex(matrix, x2, y1, 0).texture(1f, 0f).color(logoColor);
        
        GuiState.mc.getBufferBuilders().getEntityVertexConsumers().draw();
        
        // Текст "Just Visuals" с градиентом
        String text = "Just Visuals";
        float textSize = 12f;
        float textX = logoX + logoSize - 2f;
        float textY = y + (topHeight - ClientApi.inter().getHeight(text, textSize)) / 2f;
        
        renderGradientText(context, text, textX, textY, textSize, alpha);
    }
    
    /**
     * Рендер текста с градиентом (как в watermark)
     */
    private static void renderGradientText(DrawContext context, String text, float x, float y, float size, float alpha) {
        float currentX = x;
        int mainColor = Interface.INSTANCE.getMainColor();
        
        int[] gradientColors = createThemeGradient(mainColor, alpha);
        
        int charIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            float charWidth = ClientApi.inter().getWidth(character, size);
            
            int color = ColorUtils.gradient(30, charIndex * 5, gradientColors);
            
            ClientApi.text()
                .font(ClientApi.inter())
                .text(character)
                .color(color)
                .size(size)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), currentX, y);
            
            currentX += charWidth;
            charIndex++;
        }
    }
    
    /**
     * Создание градиента на основе цвета темы
     */
    private static int[] createThemeGradient(int baseColor, float alpha) {
        if (cachedGradient != null && cachedGradientBaseColor == baseColor && Math.abs(cachedGradientAlpha - alpha) < 0.01f) {
            return cachedGradient;
        }
        int r = ColorUtils.red(baseColor);
        int g = ColorUtils.green(baseColor);
        int b = ColorUtils.blue(baseColor);
        int a = (int)(255 * alpha);
        
        int color1 = ColorUtils.rgba(Math.min(255, (int)(r * 1.5)), Math.min(255, (int)(g * 1.5)), Math.min(255, (int)(b * 1.5)), a);
        int color2 = ColorUtils.rgba(Math.min(255, (int)(r * 1.35)), Math.min(255, (int)(g * 1.35)), Math.min(255, (int)(b * 1.35)), a);
        int color3 = ColorUtils.rgba(Math.min(255, (int)(r * 1.2)), Math.min(255, (int)(g * 1.2)), Math.min(255, (int)(b * 1.2)), a);
        int color4 = ColorUtils.rgba(Math.min(255, (int)(r * 1.1)), Math.min(255, (int)(g * 1.1)), Math.min(255, (int)(b * 1.1)), a);
        int color5 = ColorUtils.rgba(r, g, b, a);
        int color6 = ColorUtils.rgba((int)(r * 0.9), (int)(g * 0.9), (int)(b * 0.9), a);
        int color7 = ColorUtils.rgba((int)(r * 0.8), (int)(g * 0.8), (int)(b * 0.8), a);
        int color8 = ColorUtils.rgba((int)(r * 0.7), (int)(g * 0.7), (int)(b * 0.7), a);
        int color9 = ColorUtils.rgba((int)(r * 0.6), (int)(g * 0.6), (int)(b * 0.6), a);
        
        cachedGradientBaseColor = baseColor;
        cachedGradientAlpha = alpha;
        cachedGradient = new int[]{color1, color2, color3, color4, color5, color6, color7, color8, color9, color8, color7, color6, color5, color4, color3, color2};
        return cachedGradient;
    }
    
    /**
     * Рендер левой панели с категориями
     */
    private static void renderLeftPanel(DrawContext context, float alpha) {
        float x = GuiState.getX();
        float y = GuiState.getY() + 33.7f;
        float width = GuiState.getWidth();
        float panelHeight = GuiState.getHeight() - 33.7f;
        
        int mainColor = Interface.INSTANCE.getMainColor();
        int outlineColor = ColorUtils.setAlpha(mainColor, (int)(20 * alpha));
        int panelBg = ColorUtils.rgba(25, 25, 30, (int)(178 * alpha));
        int mainColor40 = ColorUtils.setAlpha(mainColor, (int)(102 * alpha));
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int categoryBg = ColorUtils.rgba(25, 25, 30, (int)(178 * alpha));
        
        RenderHelper.drawRoundedRect(context, x, y, width, panelHeight, 6.5f, panelBg);
        RenderHelper.drawRoundedRectOutline(context, x, y, width, panelHeight, 6.5f, 0.5f, outlineColor);
        
        // Рендер категорий
        float downY = 0f;
        for (ModuleCategory category : CACHED_CATEGORIES) {
            boolean selected = category == GuiState.getSelectedCategory();
            
            if (selected) {
                RenderHelper.drawRoundedRect(context, x, y + 9.665f + downY, width, 21.325f, 0f, categoryBg);
                RenderHelper.drawRect(context, x, y + 9.665f + downY + 0.5f, width, 0.5f, outlineColor);
                RenderHelper.drawRect(context, x, y + 9.665f + downY + 21.325f, width, 0.5f, outlineColor);
                RenderHelper.drawRect(context, x, y + 9.665f + downY, 1f, 21.825f, mainColor);
            }
            
            String icon = getCategoryIcon(category);
            int iconColor = selected ? mainColor : mainColor40;

            if (category == ModuleCategory.Friends) {
                float ix = x + 12.145f;
                float iy = y + 15f + downY;
                drawTwoPeopleIcon(context, ix, iy, iconColor);
            } else if (category == ModuleCategory.Location) {
                float ix = x + 12.145f;
                float iy = y + 15f + downY;
                drawLocationIcon(context, ix, iy, iconColor);
            } else if (category == ModuleCategory.Configs) {
                float ix = x + 12.145f;
                float iy = y + 15f + downY;
                drawCloudIcon(context, ix, iy, iconColor);
            } else {
                ClientApi.text()
                    .font(ClientApi.icons())
                    .text(icon)
                    .size(14f)
                    .color(iconColor)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x + 12.145f, y + 15f + downY);
            }
            
            int nameColor = selected ? textColor : mainColor40;
            ClientApi.text()
                .font(ClientApi.inter())
                .text(category.name())
                .size(12f)
                .color(nameColor)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + 34.995f, y + 15.8f + downY);
            
            downY += 24f;
        }
    }
    
    /**
     * Рендер главной панели с модулями
     */
    private static void renderMainPanel(DrawContext context, int mouseX, int mouseY, float alpha) {
        float leftWidth = GuiState.getWidth() * 0.28f;
        float x = GuiState.getX() + leftWidth + 0.5f;
        float y = GuiState.getY() + 34.025f;
        float width = GuiState.getWidth() - leftWidth - 0.5f;
        float height = GuiState.getHeight() - 34.025f;

        int mainColor = Interface.INSTANCE.getMainColor();
        int outlineColor = ColorUtils.setAlpha(mainColor, (int)(20 * alpha));
        int panelBg = ColorUtils.rgba(15, 15, 20, (int)(178 * alpha));

        RenderHelper.drawRoundedRect(context, x, y, width, height, 6.5f, panelBg);

        if (GuiState.getSelectedCategory() == ModuleCategory.Friends) {
            renderFriendsPanel(context, x, y, width, height, alpha, mouseX, mouseY);
            return;
        }

        if (GuiState.getSelectedCategory() == ModuleCategory.Location) {
            renderLocationPanel(context, x, y, width, height, alpha, mouseX, mouseY);
            return;
        }

        if (GuiState.getSelectedCategory() == ModuleCategory.Configs) {
            renderConfigsPanel(context, x, y, width, height, alpha);
            return;
        }
        int moduleBg = ColorUtils.setAlpha(mainColor, (int)(10 * alpha));
        int mainColor40 = ColorUtils.setAlpha(mainColor, (int)(102 * alpha));
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int mainColor6 = ColorUtils.setAlpha(mainColor, (int)(15 * alpha));
        
        RenderHelper.drawRoundedRect(context, x, y, width, height, 6.5f, panelBg);
        
        // Включаем clipping для скролла
        float clipX = x + 5f;
        float clipY = y + 5f;
        float clipWidth = width - 10f;
        float clipHeight = height - 10f;
        
        context.enableScissor((int)clipX, (int)clipY, (int)(clipX + clipWidth), (int)(clipY + clipHeight));
        
        // Вычисляем общую высоту контента
        float totalHeight = 0f;
        int moduleCount = GuiState.getModules().size();
        
        // Считаем высоту с учетом открытых настроек
        float column1Height = 0f;
        float column2Height = 0f;
        int heightIndex = 1;
        
        for (Module module : GuiState.getModules()) {
            float moduleHeight = 21.325f;
            
            // Добавляем высоту настроек если открыты
            if (GuiState.openSettingsModules.contains(module) && !module.getSettings().isEmpty()) {
                for (var setting : module.getSettings()) {
                    if (setting.getShown().get()) {
                        moduleHeight += SettingsRenderer.calculateSettingHeight(setting, (GuiState.getWidth() - 21f) / 2f) + 2f;
                    }
                }
            }
            
            boolean isColumn1 = (heightIndex % 2 == 1);
            if (isColumn1) {
                column1Height += moduleHeight + 9f;
            } else {
                column2Height += moduleHeight + 9f;
            }
            
            heightIndex++;
        }
        
        totalHeight = Math.max(column1Height, column2Height);
        
        // Обновляем скролл
        GuiState.scrollUtil.setMax(totalHeight, clipHeight);
        GuiState.scrollUtil.update();
        
        float scrollOffset = GuiState.scrollUtil.getScroll();
        
        // Рендер модулей в две колонки
        float downYColumn1 = -scrollOffset;
        float downYColumn2 = -scrollOffset;
        int index = 1;
        
        for (Module module : GuiState.getModules()) {
            boolean isColumn1 = (index % 2 == 1);
            float moduleX = isColumn1 ? x + 7.15f : x + width / 2f + 3f;
            float moduleY = y + 9.34f + (isColumn1 ? downYColumn1 : downYColumn2);
            float moduleWidth = (width - 21f) / 2f;
            float moduleHeight = 21.325f;
            
            // Вычисляем полную высоту модуля с настройками
            float totalModuleHeight = moduleHeight;
            boolean hasOpenSettings = GuiState.openSettingsModules.contains(module) && !module.getSettings().isEmpty();
            
            if (hasOpenSettings) {
                for (var setting : module.getSettings()) {
                    if (setting.getShown().get()) {
                        totalModuleHeight += SettingsRenderer.calculateSettingHeight(setting, moduleWidth) + 2f;
                    }
                }
            }
            
            // Проверяем, виден ли модуль
            if (moduleY + totalModuleHeight >= clipY && moduleY <= clipY + clipHeight) {
                // Фон модуля (расширенный если настройки открыты)
                RenderHelper.drawRoundedRectOutline(context, moduleX, moduleY, moduleWidth, totalModuleHeight, 6.5f, 0.1f, outlineColor);
                RenderHelper.drawRoundedRect(context, moduleX, moduleY, moduleWidth, totalModuleHeight, 6.5f, moduleBg);
                
                float enableAnim = module.isEnabled() ? 1f : 0f;
                int nameColor = ColorUtils.interpolate(mainColor40, textColor, enableAnim);
                
                // Название модуля
                float textY = moduleY + 4f;
                
                ClientApi.text()
                    .font(ClientApi.inter())
                    .text(module.getName())
                    .size(11f)
                    .color(nameColor)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), moduleX + 7f, textY);
                
                // Тоггл
                float toggleX = moduleX + moduleWidth - 11.555f;
                float toggleY = moduleY + 8.14f;
                int toggleOutline = ColorUtils.setAlpha(mainColor, (int)(50 * alpha));
                RenderHelper.drawRoundedRectOutline(context, toggleX - 1.5f, toggleY - 1.5f, 6f, 6f, 3f, 0.08f, toggleOutline);
                RenderHelper.drawRoundedRect(context, toggleX - 1.5f, toggleY - 1.5f, 6f, 6f, 3f, mainColor6);
                
                int dotColor = ColorUtils.interpolate(mainColor40, mainColor, enableAnim);
                RenderHelper.drawRoundedRect(context, toggleX - 0.75f, toggleY - 0.78f, 3f, 3f, 1.5f, dotColor);
                
                // Иконка настроек если есть
                if (!module.getSettings().isEmpty()) {
                    float settingsIconX = moduleX + moduleWidth - 25f;
                    float settingsIconY = moduleY + 6f;
                    boolean settingsOpen = hasOpenSettings;
                    int iconColor = settingsOpen ? mainColor : mainColor40;
                    
                    ClientApi.text()
                        .font(ClientApi.otherIcons())
                        .text("G")
                        .size(8f)
                        .color(iconColor)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), settingsIconX, settingsIconY);
                }
                
                // Рендер настроек внутри капсулы
                if (hasOpenSettings) {
                    float settingsY = moduleY + moduleHeight + 2f;
                    
                    for (var setting : module.getSettings()) {
                        if (setting.getShown().get()) {
                            float settingHeight = SettingsRenderer.renderSetting(
                                context, setting, moduleX + 3f, settingsY, moduleWidth - 6f, alpha,
                                GuiState.getCurrentMouseX(), GuiState.getCurrentMouseY()
                            );
                            settingsY += settingHeight + 2f;
                        }
                    }
                }
            }
            
            if (isColumn1) {
                downYColumn1 += totalModuleHeight + 9f;
            } else {
                downYColumn2 += totalModuleHeight + 9f;
            }
            
            index++;
        }
        
        context.disableScissor();
        
        // Рендер скроллбара
        if (totalHeight > clipHeight) {
            GuiState.scrollUtil.render(context, x + width - 7f, y + 5f, 2f, clipHeight, alpha);
        }
    }
    
    private static void renderUserInfo(DrawContext context, float alpha) {
        float x = GuiState.getX();
        float y = GuiState.getY();
        float height = GuiState.getHeight();

        String username = GuiState.mc.player != null ? GuiState.mc.player.getName().getString() : "Player";
        String role = "user";

        float panelW = 110f;
        float panelH = 30f;
        float panelX = x + 8f;
        float panelY = y + height - panelH - 8f;

        int bgColor = ColorUtils.rgba(30, 30, 35, (int)(160 * alpha));
        RenderHelper.drawRoundedRect(context, panelX, panelY, panelW, panelH, 5f, bgColor);

        int mainColor = Interface.INSTANCE.getMainColor();
        int dotColor = ColorUtils.setAlpha(mainColor, (int)(200 * alpha));
        float dotX = panelX + 7f;
        float dotY = panelY + panelH / 2f - 2f;
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(4f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(dotColor))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), dotX, dotY);

        float textAlpha = 255 * alpha;
        ClientApi.text()
                .text(username)
                .font(ClientApi.inter())
                .size(8f)
                .color(ColorUtils.setAlpha(-1, (int) textAlpha))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), panelX + 14f, panelY + 4f);

        ClientApi.text()
                .text(role)
                .font(ClientApi.inter())
                .size(6f)
                .color(ColorUtils.setAlpha(0xAAAAAA, (int) textAlpha))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), panelX + 14f, panelY + 16f);
    }

    private static void renderFriendsPanel(DrawContext context, float x, float y, float width, float height, float alpha, int mouseX, int mouseY) {
        int mainColor = Interface.INSTANCE.getMainColor();
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int dimTextColor = ColorUtils.rgba(150, 150, 150, (int)(200 * alpha));
        int inputBg = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        int inputOutline = ColorUtils.setAlpha(mainColor, (int)(40 * alpha));
        int mainColor40 = ColorUtils.setAlpha(mainColor, (int)(102 * alpha));

        float pad = 10f;
        float inputX = x + pad;
        float inputY = y + pad;
        float inputW = width - pad * 2 - 30f;
        float inputH = 18f;

        RenderHelper.drawRoundedRect(context, inputX, inputY, inputW, inputH, 4f, inputBg);
        RenderHelper.drawRoundedRectOutline(context, inputX, inputY, inputW, inputH, 4f, 0.5f,
                friendInputFocused ? mainColor : inputOutline);

        String displayText = friendInput.isEmpty() ? "" : friendInput;
        if (friendInputFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            displayText += "|";
        }
        if (displayText.isEmpty()) {
            ClientApi.text()
                    .text("Введите ник...")
                    .font(ClientApi.inter())
                    .size(7f)
                    .color(dimTextColor)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), inputX + 5f, inputY + 5f);
        } else {
            ClientApi.text()
                    .text(displayText)
                    .font(ClientApi.inter())
                    .size(7f)
                    .color(textColor)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), inputX + 5f, inputY + 5f);
        }

        float btnX = inputX + inputW + 5f;
        float btnW = 25f;
        float btnH = inputH;
        int btnColor = ColorUtils.setAlpha(mainColor, (int)(180 * alpha));
        RenderHelper.drawRoundedRect(context, btnX, inputY, btnW, btnH, 4f, btnColor);
        ClientApi.text()
                .text("+")
                .font(ClientApi.inter())
                .size(10f)
                .color(-1)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), btnX + 9f, inputY + 3f);

        float listY = inputY + inputH + 8f;
        float listW = width - pad * 2;
        float listH = height - inputH - pad - 10f;

        context.enableScissor((int)(x + pad), (int)listY, (int)(x + pad + listW), (int)(listY + listH));

        var friends = FriendsStorage.getFriends();
        float itemH = 22f;
        float itemPad = 2f;
        float headSize = 16f;

        for (int i = 0; i < friends.size(); i++) {
            String friend = friends.get(i);
            float itemY = listY + i * (itemH + itemPad);

            if (itemY + itemH > listY && itemY < listY + listH) {
                int itemBg = ColorUtils.rgba(25, 25, 30, (int)(150 * alpha));
                RenderHelper.drawRoundedRect(context, x + pad, itemY, listW, itemH, 3f, itemBg);

                var uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + friend).getBytes(StandardCharsets.UTF_8));
                var profile = new GameProfile(uuid, friend);
                Identifier skin = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile).texture();
                Color headColor = new Color(255, 255, 255, Math.round(255 * alpha));

                DrawUtil.renderHeadTexture(
                        context.getMatrices(),
                        x + pad + 3f,
                        itemY + (itemH - headSize) / 2f,
                        headSize,
                        headSize,
                        3f,
                        0.125f, 0.125f,
                        0.125f, 0.125f,
                        skin,
                        headColor
                );
                DrawUtil.renderHeadTexture(
                        context.getMatrices(),
                        x + pad + 3f,
                        itemY + (itemH - headSize) / 2f,
                        headSize,
                        headSize,
                        3f,
                        0.625f, 0.125f,
                        0.125f, 0.125f,
                        skin,
                        headColor
                );

                ClientApi.text()
                        .text(friend)
                        .font(ClientApi.inter())
                        .size(7f)
                        .color(textColor)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), x + pad + headSize + 8f, itemY + (itemH - 7f) / 2f);

                float removeX = x + pad + listW - 22f;
                float removeY = itemY + (itemH - 14f) / 2f;
                float removeW = 14f;
                float removeH = 14f;
                boolean removeHovered = mouseX >= removeX && mouseX <= removeX + removeW
                        && mouseY >= removeY && mouseY <= removeY + removeH;
                float targetHover = removeHovered ? 1f : 0f;
                removeHoverAnim[i] += (targetHover - removeHoverAnim[i]) * 0.35f;
                if (Math.abs(removeHoverAnim[i] - targetHover) < 0.01f) removeHoverAnim[i] = targetHover;
                float hv = removeHoverAnim[i];

                int bgR = (int)(35 + (60 - 35) * hv);
                int bgG = (int)(35 + (25 - 35) * hv);
                int bgB = (int)(40 + (25 - 40) * hv);
                int bgColor = ColorUtils.rgba(bgR, bgG, bgB, (int)(180 * alpha));
                RenderHelper.drawRoundedRect(context, removeX, removeY, removeW, removeH, 3f, bgColor);

                int textR = (int)(160 + (255 - 160) * hv);
                int textG = (int)(160 + (70 - 160) * hv);
                int textB = (int)(160 + (70 - 160) * hv);
                int xColor = ColorUtils.rgba(textR, textG, textB, (int)(220 * alpha));
                ClientApi.text()
                        .text("X")
                        .font(ClientApi.inter())
                        .size(6f)
                        .color(xColor)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), removeX + 4f, removeY + 3f);
            }
        }

        if (friends.isEmpty()) {
            ClientApi.text()
                    .text("Список пуст")
                    .font(ClientApi.inter())
                    .size(8f)
                    .color(dimTextColor)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x + pad + listW / 2f - 25f, listY + 10f);
        }

        context.disableScissor();
    }

    private static void drawTwoPeopleIcon(DrawContext context, float x, float y, int color) {
        var matrices = context.getMatrices().peek().getPositionMatrix();

        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(4f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x, y);

        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(6f, 5f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x - 1f, y + 5f);

        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(4f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x + 8f, y);

        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(6f, 5f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x + 7f, y + 5f);
    }

    private static String getCategoryIcon(ModuleCategory category) {
        return switch (category) {
            case Visuals -> "V";
            case HUD -> "H";
            case Utils -> "U";
            case Location -> "L";
            case Friends -> "R";
            case Configs -> "C";
        };
    }

    private static void drawLocationIcon(DrawContext context, float x, float y, int color) {
        var matrices = context.getMatrices().peek().getPositionMatrix();

        // Teardrop/pin shape - circle on top
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(6f, 6f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(3f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x + 2f, y);

        // Pin body pointing down
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(2f, 5f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build()
                .render(matrices, x + 4f, y + 5f);

        // Inner dot
        int innerColor = (color & 0x00FFFFFF) | (((color >> 24) & 0xFF) / 2 << 24);
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(2f, 2f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(innerColor))
                .build()
                .render(matrices, x + 4f, y + 2f);
    }

    private static void renderLocationPanel(DrawContext context, float x, float y, float width, float height, float alpha, int mouseX, int mouseY) {
        int mainColor = Interface.INSTANCE.getMainColor();
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int dimTextColor = ColorUtils.rgba(150, 150, 150, (int)(200 * alpha));
        int inputBg = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        int inputOutline = ColorUtils.setAlpha(mainColor, (int)(40 * alpha));
        int mainColor40 = ColorUtils.setAlpha(mainColor, (int)(102 * alpha));

        float pad = 10f;
        float inputY = y + pad;
        float inputH = 14f;
        float gap = 3f;

        float nameW = width - pad * 2;
        float nameX = x + pad;
        RenderHelper.drawRoundedRect(context, nameX, inputY, nameW, inputH, 3f, inputBg);
        RenderHelper.drawRoundedRectOutline(context, nameX, inputY, nameW, inputH, 3f, 0.5f,
                locationInputFocused ? mainColor : inputOutline);
        String nameText = locationInput.isEmpty() ? "" : locationInput;
        if (locationInputFocused && (System.currentTimeMillis() / 500) % 2 == 0) nameText += "|";
        if (nameText.isEmpty()) {
            ClientApi.text().text("Название...").font(ClientApi.inter()).size(6f).color(dimTextColor).build()
                    .render(context.getMatrices().peek().getPositionMatrix(), nameX + 4f, inputY + 4f);
        } else {
            ClientApi.text().text(nameText).font(ClientApi.inter()).size(6f).color(textColor).build()
                    .render(context.getMatrices().peek().getPositionMatrix(), nameX + 4f, inputY + 4f);
        }

        float coordY = inputY + inputH + gap;
        float coordH = 14f;
        float coordW = (nameW - gap * 2) / 3f;

        String[] coordLabels = {"X", "Y", "Z"};
        String[] coordValues = {locationXInput, locationYInput, locationZInput};
        boolean[] coordFocused = {locationXFocused, locationYFocused, locationZFocused};

        for (int i = 0; i < 3; i++) {
            float cx = nameX + i * (coordW + gap);
            RenderHelper.drawRoundedRect(context, cx, coordY, coordW, coordH, 3f, inputBg);
            RenderHelper.drawRoundedRectOutline(context, cx, coordY, coordW, coordH, 3f, 0.5f,
                    coordFocused[i] ? mainColor : inputOutline);

            String ct = coordValues[i].isEmpty() ? "" : coordValues[i];
            if (coordFocused[i] && (System.currentTimeMillis() / 500) % 2 == 0) ct += "|";

            ClientApi.text().text(coordLabels[i]).font(ClientApi.inter()).size(5f)
                    .color(ColorUtils.setAlpha(mainColor, (int)(150 * alpha))).build()
                    .render(context.getMatrices().peek().getPositionMatrix(), cx + 3f, coordY + 5f);

            if (ct.isEmpty()) {
                ClientApi.text().text("0").font(ClientApi.inter()).size(6f).color(dimTextColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), cx + 12f, coordY + 3f);
            } else {
                ClientApi.text().text(ct).font(ClientApi.inter()).size(6f).color(textColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), cx + 12f, coordY + 3f);
            }
        }

        float btnY = coordY + coordH + gap;
        float btnW = nameW;
        float btnH = 16f;
        int btnColor = ColorUtils.setAlpha(mainColor, (int)(180 * alpha));
        RenderHelper.drawRoundedRect(context, nameX, btnY, btnW, btnH, 4f, btnColor);
        ClientApi.text().text("+").font(ClientApi.inter()).size(10f).color(-1).build()
                .render(context.getMatrices().peek().getPositionMatrix(), nameX + btnW / 2f - 3f, btnY + 3f);

        float listY = btnY + btnH + 8f;
        float listW = width - pad * 2;
        float listH = height - (listY - y) - pad;

        context.enableScissor((int)(x + pad), (int)listY, (int)(x + pad + listW), (int)(listY + listH));

        var waypoints = LocationStorage.getWaypoints();
        float itemH = 22f;
        float itemPad = 2f;

        for (int i = 0; i < waypoints.size(); i++) {
            LocationStorage.Waypoint wp = waypoints.get(i);
            float itemY = listY + i * (itemH + itemPad);

            if (itemY + itemH > listY && itemY < listY + listH) {
                int itemBg = ColorUtils.rgba(25, 25, 30, (int)(150 * alpha));
                RenderHelper.drawRoundedRect(context, x + pad, itemY, listW, itemH, 3f, itemBg);

                int wpColor = ColorUtils.setAlpha(wp.color, (int)(220 * alpha));
                RenderHelper.drawRoundedRect(context, x + pad + 3f, itemY + (itemH - 8f) / 2f, 3f, 8f, 1.5f, wpColor);

                ClientApi.text().text(wp.name).font(ClientApi.inter()).size(7f).color(textColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), x + pad + 10f, itemY + 4f);

                String coordText = String.format("%d / %d / %d", wp.x, wp.y, wp.z);
                ClientApi.text().text(coordText).font(ClientApi.inter()).size(6f).color(dimTextColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), x + pad + 10f, itemY + 13f);

                float statusX = x + pad + listW - 36f;
                float statusY = itemY + (itemH - 10f) / 2f;
                float statusW = 22f;
                float statusH = 10f;
                int statusBg = wp.enabled ? ColorUtils.setAlpha(mainColor, (int)(180 * alpha)) : ColorUtils.rgba(50, 50, 55, (int)(180 * alpha));
                RenderHelper.drawRoundedRect(context, statusX, statusY, statusW, statusH, 5f, statusBg);
                ClientApi.text().text(wp.enabled ? "ON" : "OFF").font(ClientApi.inter()).size(5f).color(-1).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), statusX + 5f, statusY + 2f);

                float kbX = statusX - 28f;
                float kbY2 = statusY;
                float kbW = 24f;
                float kbH = 10f;
                boolean isBinding = (bindingWaypointIndex == i);
                int kbBg = isBinding ? ColorUtils.setAlpha(0xFF5555, (int)(200 * alpha)) : ColorUtils.rgba(40, 40, 45, (int)(180 * alpha));
                RenderHelper.drawRoundedRect(context, kbX, kbY2, kbW, kbH, 3f, kbBg);
                String kbText = isBinding ? "..." : (wp.keyBind == 0 ? "None" : org.lwjgl.glfw.GLFW.glfwGetKeyName(wp.keyBind, 0));
                if (kbText == null) kbText = "Key";
                ClientApi.text().text(kbText).font(ClientApi.inter()).size(5f).color(isBinding ? -1 : dimTextColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), kbX + 3f, kbY2 + 2f);

                float removeX = x + pad + listW - 14f;
                float removeY = itemY + (itemH - 14f) / 2f;
                float removeW = 14f;
                float removeH = 14f;
                boolean removeHovered = mouseX >= removeX && mouseX <= removeX + removeW
                        && mouseY >= removeY && mouseY <= removeY + removeH;
                float targetHover = removeHovered ? 1f : 0f;
                locationRemoveHoverAnim[i] += (targetHover - locationRemoveHoverAnim[i]) * 0.35f;
                if (Math.abs(locationRemoveHoverAnim[i] - targetHover) < 0.01f) locationRemoveHoverAnim[i] = targetHover;
                float hv = locationRemoveHoverAnim[i];

                int bgR = (int)(35 + (60 - 35) * hv);
                int bgG = (int)(35 + (25 - 35) * hv);
                int bgB = (int)(40 + (25 - 40) * hv);
                int bgColor = ColorUtils.rgba(bgR, bgG, bgB, (int)(180 * alpha));
                RenderHelper.drawRoundedRect(context, removeX, removeY, removeW, removeH, 3f, bgColor);

                int textR = (int)(160 + (255 - 160) * hv);
                int textG = (int)(160 + (70 - 160) * hv);
                int textB = (int)(160 + (70 - 160) * hv);
                int xColor = ColorUtils.rgba(textR, textG, textB, (int)(220 * alpha));
                ClientApi.text().text("X").font(ClientApi.inter()).size(6f).color(xColor).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), removeX + 4f, removeY + 3f);
            }
        }

        if (waypoints.isEmpty()) {
            ClientApi.text().text("Список пуст").font(ClientApi.inter()).size(8f).color(dimTextColor).build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x + pad + listW / 2f - 25f, listY + 10f);
        }

        context.disableScissor();
    }

    private static void drawCloudIcon(DrawContext context, float x, float y, int color) {
        var m = context.getMatrices().peek().getPositionMatrix();

        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(10f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build().render(m, x + 1f, y);
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(12f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build().render(m, x, y + 3f);
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(14f, 4f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(2f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build().render(m, x, y + 6f);
        ClientApi.rectangle()
                .size(new dev.client.api.nullcry.render.core.builders.states.SizeState(12f, 3f))
                .radius(new dev.client.api.nullcry.render.core.builders.states.QuadRadiusState(1.5f))
                .color(new dev.client.api.nullcry.render.core.builders.states.QuadColorState(color))
                .build().render(m, x + 1f, y + 9f);
    }

    private static void renderConfigsPanel(DrawContext context, float x, float y, float width, float height, float alpha) {
        int mainColor = Interface.INSTANCE.getMainColor();
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int dimTextColor = ColorUtils.rgba(150, 150, 150, (int)(200 * alpha));
        int inputBg = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        int mainColor40 = ColorUtils.setAlpha(mainColor, (int)(102 * alpha));
        float pad = 10f;

        String title = "Configs";
        ClientApi.text().font(ClientApi.inter()).text(title).size(12f).color(textColor).build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + pad, y + pad);

        String desc = "Скоро...";
        ClientApi.text().font(ClientApi.inter()).text(desc).size(8f).color(dimTextColor).build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + pad, y + pad + 18f);

        float cardW = width - pad * 2;
        float cardH = 36f;
        float cardY = y + pad + 40f;
        RenderHelper.drawRoundedRect(context, x + pad, cardY, cardW, cardH, 5f, inputBg);
        RenderHelper.drawRoundedRectOutline(context, x + pad, cardY, cardW, cardH, 5f, 0.5f, mainColor40);

        drawCloudIcon(context, x + pad + 8f, cardY + 10f, mainColor40);

        ClientApi.text().font(ClientApi.inter()).text("Сохранение конфигов").size(8f).color(textColor).build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + pad + 26f, cardY + 8f);
        ClientApi.text().font(ClientApi.inter()).text("Раздел в разработке").size(6f).color(dimTextColor).build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + pad + 26f, cardY + 20f);
    }
}
