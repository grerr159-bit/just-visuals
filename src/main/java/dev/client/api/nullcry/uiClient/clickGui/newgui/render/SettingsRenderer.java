package dev.client.api.nullcry.uiClient.clickGui.newgui.render;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.modules.settings.*;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;

public class SettingsRenderer {
    
    public static float renderSetting(DrawContext context, Setting setting, float x, float y, float width, float alpha, int mouseX, int mouseY) {
        if (!setting.getShown().get()) {
            return 0f;
        }
        
        int bgColor = ColorUtils.rgba(20, 20, 25, (int)(150 * alpha));
        int textColor = ColorUtils.rgba(200, 200, 200, (int)(255 * alpha));
        
        float textSize = 7f;
        float maxTextWidth = width - 50f;
        float lineSpacing = 2f;
        
        java.util.List<String> lines = dev.client.api.nullcry.uiClient.clickGui.newgui.util.TextWrapUtil.wrapText(
            setting.getName(), maxTextWidth, ClientApi.inter(), textSize
        );
        
        float lineHeight = ClientApi.inter().getHeight("A", textSize);
        float textHeight = lines.size() * lineHeight + (lines.size() - 1) * lineSpacing;
        float height = Math.max(20f, textHeight + 10f);
        
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(context, x, y, width, height, 4f, bgColor);
        
        // Центрируем текст вертикально
        float totalTextHeight = lines.size() * lineHeight + (lines.size() - 1) * lineSpacing;
        float textStartY = y + (height - totalTextHeight) / 2f;
        
        for (String line : lines) {
            ClientApi.text()
                .font(ClientApi.inter())
                .text(line)
                .size(textSize)
                .color(textColor)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + 5f, textStartY);
            textStartY += lineHeight + lineSpacing;
        }
        
        if (setting instanceof CheckBox checkBox) {
            renderCheckBox(context, checkBox, x, y, width, height, alpha);
        } else if (setting instanceof Slider slider) {
            renderSlider(context, slider, x, y, width, height, alpha);
        } else if (setting instanceof ModeElement mode) {
            renderModeElement(context, mode, x, y, width, height, alpha);
        } else if (setting instanceof ColorPicker colorPicker) {
            renderColorPicker(context, colorPicker, x, y, width, height, alpha);
        } else if (setting instanceof KeyBind keyBind) {
            renderKeyBind(context, keyBind, x, y, width, height, alpha);
        } else if (setting instanceof Input input) {
            renderInput(context, input, x, y, width, height, alpha);
        } else if (setting instanceof SelectElements selectElements) {
            height = renderSelectElements(context, selectElements, x, y, width, alpha, lines.size());
        } else if (setting instanceof Collection collection) {
            height = renderCollection(context, collection, x, y, width, alpha, lines.size());
        }
        
        return height;
    }
    
    public static float calculateSettingHeight(Setting setting, float width) {
        if (!setting.getShown().get()) {
            return 0f;
        }
        
        float textSize = 7f;
        float maxTextWidth = width - 50f;
        float lineSpacing = 2f;
        
        java.util.List<String> lines = dev.client.api.nullcry.uiClient.clickGui.newgui.util.TextWrapUtil.wrapText(
            setting.getName(), maxTextWidth, ClientApi.inter(), textSize
        );
        
        float textHeight = lines.size() * ClientApi.inter().getHeight(setting.getName(), textSize) 
                         + (lines.size() - 1) * lineSpacing;
        
        if (setting instanceof SelectElements selectElements) {
            // Точный расчет высоты для SelectElements
            float chipHeight = 12f;
            float chipGapX = 2f;
            float chipGapY = 2f;
            float padX = 5f;
            float chipTextSize = 7f;
            
            float nameHeight = lines.size() * ClientApi.inter().getHeight("A", 10f) + (lines.size() - 1) * 2f;
            float availableWidth = width - 10f; // 5f padding с каждой стороны
            float currentX = 0f;
            float currentY = 0f;
            
            for (var checkbox : selectElements.getValues()) {
                String text = checkbox.getName();
                float textWidth = ClientApi.inter().getWidth(text, chipTextSize);
                float chipWidth = textWidth + padX * 2f;
                
                // Если чип не помещается в текущую строку, переходим на новую
                if (currentX + chipWidth > availableWidth && currentX > 0f) {
                    currentX = 0f;
                    currentY += chipHeight + chipGapY;
                }
                
                currentX += chipWidth + chipGapX;
            }
            
            // Добавляем высоту последней строки
            float totalChipsHeight = currentY + chipHeight;
            return Math.max(20f, nameHeight + 10f + totalChipsHeight);
        } else if (setting instanceof Collection collection) {
            float totalHeight = textHeight + 10f;
            for (var child : collection.getSettings()) {
                if (child.getShown().get()) {
                    totalHeight += calculateSettingHeight(child, width - 10f) + 2f;
                }
            }
            return totalHeight;
        }
        
        return Math.max(20f, textHeight + 10f);
    }

    
    private static void renderCheckBox(DrawContext context, CheckBox checkBox, float x, float y, float width, float height, float alpha) {
        int mainColor = Interface.INSTANCE.getMainColor();
        float toggleSize = 10f;
        float toggleX = x + width - toggleSize - 5f;
        float toggleY = y + (height - toggleSize) / 2f;
        
        int bgColor = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        int enabledColor = ColorUtils.setAlpha(mainColor, (int)(255 * alpha));
        
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
            context, toggleX, toggleY, toggleSize, toggleSize, 2f, bgColor
        );
        
        if (checkBox.getEnabled()) {
            dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
                context, toggleX + 2f, toggleY + 2f, toggleSize - 4f, toggleSize - 4f, 1f, enabledColor
            );
        }
    }
    
    private static void renderSlider(DrawContext context, Slider slider, float x, float y, float width, float height, float alpha) {
        int mainColor = Interface.INSTANCE.getMainColor();
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        
        String valueText = String.format("%.2f", slider.getValue());
        float valueWidth = ClientApi.inter().getWidth(valueText, 8f);
        float valueHeight = ClientApi.inter().getHeight(valueText, 8f);
        
        // Поднимаем текст выше
        float valueY = y + 3f;
        
        ClientApi.text()
            .font(ClientApi.inter())
            .text(valueText)
            .size(8f)
            .color(textColor)
            .build()
            .render(context.getMatrices().peek().getPositionMatrix(), x + width - valueWidth - 5f, valueY);
        
        float sliderY = y + height - 6f;
        float sliderWidth = width - 10f;
        float sliderHeight = 2f;
        float sliderX = x + 5f;
        
        int trackColor = ColorUtils.rgba(40, 40, 45, (int)(200 * alpha));
        int fillColor = ColorUtils.setAlpha(mainColor, (int)(255 * alpha));
        
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
            context, sliderX, sliderY, sliderWidth, sliderHeight, 1f, trackColor
        );
        
        float progress = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
        float fillWidth = sliderWidth * progress;
        
        if (fillWidth > 0) {
            dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
                context, sliderX, sliderY, fillWidth, sliderHeight, 1f, fillColor
            );
        }
    }
    
    private static void renderModeElement(DrawContext context, ModeElement mode, float x, float y, float width, float height, float alpha) {
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        
        String currentValue = mode.getValue();
        if (currentValue == null) currentValue = "";
        float valueWidth = ClientApi.inter().getWidth(currentValue, 8f);
        float valueHeight = ClientApi.inter().getHeight(currentValue, 8f);
        
        // Центрируем значение вертикально
        float valueY = y + (height - valueHeight) / 2f;
        
        ClientApi.text()
            .font(ClientApi.inter())
            .text(currentValue)
            .size(8f)
            .color(textColor)
            .build()
            .render(context.getMatrices().peek().getPositionMatrix(), x + width - valueWidth - 5f, valueY);
    }
    
    private static void renderColorPicker(DrawContext context, ColorPicker colorPicker, float x, float y, float width, float height, float alpha) {
        float colorSize = 10f;
        float colorX = x + width - colorSize - 5f;
        float colorY = y + (height - colorSize) / 2f;
        
        int color = ColorUtils.setAlpha(colorPicker.getColorRGBA(), (int)(255 * alpha));
        
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
            context, colorX, colorY, colorSize, colorSize, 2f, color
        );
    }

    private static void renderKeyBind(DrawContext context, KeyBind keyBind, float x, float y, float width, float height, float alpha) {
        float boxW = 30f;
        float boxH = 11f;
        float boxX = x + width - boxW - 5f;
        float boxY = y + (height - boxH) / 2f;

        boolean selected = Boolean.TRUE.equals(keyBind.getSelected());
        int bgColor;
        if (selected) {
            bgColor = ColorUtils.rgba(70, 130, 220, (int)(200 * alpha));
        } else {
            bgColor = ColorUtils.rgba(28, 30, 35, (int)(180 * alpha));
        }
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
            context, boxX, boxY, boxW, boxH, 2f, bgColor
        );

        String displayText;
        if (selected) {
            displayText = "...";
        } else {
            int key = keyBind.getKey();
            if (key == 0 || key == -1 || key == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
                displayText = "N/A";
            } else {
                String keyName = dev.client.api.nullcry.helper.client.keyboard.KeyboardStorage.getKey(key);
                displayText = (keyName != null && !keyName.isEmpty() && !"Unknown".equalsIgnoreCase(keyName)) ? keyName : "N/A";
            }
        }

        int textColor = ColorUtils.rgba(255, 255, 255, (int)(220 * alpha));
        ClientApi.text()
                .text(displayText)
                .font(ClientApi.inter())
                .size(6f)
                .color(textColor)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), boxX + 4.5f, boxY + 2f);
    }
    
    private static void renderInput(DrawContext context, Input input, float x, float y, float width, float height, float alpha) {
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int bgColor = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        
        float inputY = y + height - 14f;
        float inputHeight = 12f;
        float inputWidth = width - 10f;
        float inputX = x + 5f;
        
        dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
            context, inputX, inputY, inputWidth, inputHeight, 2f, bgColor
        );
        
        String value = input.getValue();
        if (value == null || value.isEmpty()) {
            value = "...";
            textColor = ColorUtils.rgba(150, 150, 150, (int)(255 * alpha));
        }
        
        float textHeight = ClientApi.inter().getHeight(value, 7f);
        float textY = inputY + (inputHeight - textHeight) / 2f;
        
        ClientApi.text()
            .font(ClientApi.inter())
            .text(value)
            .size(7f)
            .color(textColor)
            .build()
            .render(context.getMatrices().peek().getPositionMatrix(), inputX + 3f, textY);
    }

    
    private static float renderSelectElements(DrawContext context, SelectElements selectElements, float x, float y, float width, float alpha, int nameLinesCount) {
        int mainColor = Interface.INSTANCE.getMainColor();
        int bgColor = ColorUtils.rgba(30, 30, 35, (int)(200 * alpha));
        int selectedColor = ColorUtils.setAlpha(mainColor, (int)(150 * alpha));
        int textColor = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int unselectedTextColor = ColorUtils.rgba(180, 180, 180, (int)(255 * alpha));
        
        float chipHeight = 12f;
        float chipGapX = 2f;
        float chipGapY = 2f;
        float padX = 5f;
        float textSize = 7f;
        
        float nameHeight = nameLinesCount * ClientApi.inter().getHeight("A", 10f) + (nameLinesCount - 1) * 2f;
        float startY = y + nameHeight + 5f;
        float currentX = x + 5f;
        float currentY = startY;
        float maxWidth = x + width - 5f;
        
        for (var checkbox : selectElements.getValues()) {
            String text = checkbox.getName();
            float textWidth = ClientApi.inter().getWidth(text, textSize);
            float chipWidth = textWidth + padX * 2f;
            
            if (currentX + chipWidth > maxWidth && currentX > x + 5f) {
                currentX = x + 5f;
                currentY += chipHeight + chipGapY;
            }
            
            int chipBg = checkbox.getEnabled() ? selectedColor : bgColor;
            dev.client.api.nullcry.uiClient.clickGui.newgui.util.RenderHelper.drawRoundedRect(
                context, currentX, currentY, chipWidth, chipHeight, 2f, chipBg
            );
            
            int chipTextColor = checkbox.getEnabled() ? textColor : unselectedTextColor;
            ClientApi.text()
                .font(ClientApi.inter())
                .text(text)
                .size(textSize)
                .color(chipTextColor)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), 
                       currentX + padX, currentY + (chipHeight - ClientApi.inter().getHeight(text, textSize)) / 2f);
            
            currentX += chipWidth + chipGapX;
        }
        
        float totalHeight = (currentY - y) + chipHeight + 5f;
        return Math.max(20f, totalHeight);
    }
    
    private static float renderCollection(DrawContext context, Collection collection, float x, float y, float width, float alpha, int nameLinesCount) {
        float nameHeight = nameLinesCount * ClientApi.inter().getHeight("A", 10f) + (nameLinesCount - 1) * 2f;
        float currentY = y + nameHeight + 5f;
        
        for (var childSetting : collection.getSettings()) {
            if (childSetting.getShown().get()) {
                float childHeight = renderSetting(context, childSetting, x + 5f, currentY, width - 10f, alpha, 0, 0);
                currentY += childHeight + 2f;
            }
        }
        
        return currentY - y + 5f;
    }

    
    public static boolean handleClick(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float height) {
        if (!setting.getShown().get()) {
            return false;
        }
        
        if (button == 0) {
            if (setting instanceof CheckBox checkBox) {
                // Клик только по области чекбокса справа
                float toggleSize = 10f;
                float toggleX = x + width - toggleSize - 5f;
                float toggleY = y + (height - toggleSize) / 2f;
                if (isHovered(mouseX, mouseY, toggleX - 2f, toggleY - 2f, toggleSize + 4f, toggleSize + 4f)) {
                    checkBox.applyDefault(!checkBox.getEnabled());
                    return true;
                }
            } else if (setting instanceof ModeElement mode) {
                // Клик по всей области настройки
                if (isHovered(mouseX, mouseY, x, y, width, height)) {
                    String currentValue = mode.getValue();
                    int currentIndex = mode.getValues().indexOf(currentValue);
                    int nextIndex = (currentIndex + 1) % mode.getValues().size();
                    mode.set(mode.getValues().get(nextIndex));
                    return true;
                }
            } else if (setting instanceof ColorPicker colorPicker) {
                // Клик только по цветному квадрату справа
                float colorSize = 10f;
                float colorX = x + width - colorSize - 5f;
                float colorY = y + (height - colorSize) / 2f;
                if (isHovered(mouseX, mouseY, colorX - 2f, colorY - 2f, colorSize + 4f, colorSize + 4f)) {
                    // TODO: открыть color picker popup
                    return true;
                }
            } else if (setting instanceof KeyBind keyBind) {
                float boxW = 30f;
                float boxH = 11f;
                float boxX = x + width - boxW - 5f;
                float boxY = y + (height - boxH) / 2f;
                if (isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
                    if (button == 0) {
                        keyBind.setSelected(!Boolean.TRUE.equals(keyBind.getSelected()));
                        return true;
                    }
                }
            } else if (setting instanceof SelectElements selectElements) {
                float textSize = 7f;
                float maxTextWidth = width - 50f;
                java.util.List<String> lines = dev.client.api.nullcry.uiClient.clickGui.newgui.util.TextWrapUtil.wrapText(
                    setting.getName(), maxTextWidth, ClientApi.inter(), textSize
                );
                float nameHeight = lines.size() * ClientApi.inter().getHeight("A", 10f) + (lines.size() - 1) * 2f;
                
                float chipHeight = 12f;
                float chipGapX = 2f;
                float chipGapY = 2f;
                float padX = 5f;
                float chipTextSize = 7f;
                
                float startY = y + nameHeight + 5f;
                float currentX = x + 5f;
                float currentY = startY;
                float maxWidth = x + width - 5f;
                
                for (var checkbox : selectElements.getValues()) {
                    String text = checkbox.getName();
                    float textWidth = ClientApi.inter().getWidth(text, chipTextSize);
                    float chipWidth = textWidth + padX * 2f;
                    
                    if (currentX + chipWidth > maxWidth && currentX > x + 5f) {
                        currentX = x + 5f;
                        currentY += chipHeight + chipGapY;
                    }
                    
                    // Точная проверка попадания в чип
                    if (mouseX >= currentX && mouseX <= currentX + chipWidth && 
                        mouseY >= currentY && mouseY <= currentY + chipHeight) {
                        checkbox.applyDefault(!checkbox.getEnabled());
                        return true;
                    }
                    
                    currentX += chipWidth + chipGapX;
                }
                // Не возвращаем true если клик был мимо всех чипов
            } else if (setting instanceof Collection collection) {
                float textSize = 10f;
                float maxTextWidth = width - 50f;
                java.util.List<String> lines = dev.client.api.nullcry.uiClient.clickGui.newgui.util.TextWrapUtil.wrapText(
                    setting.getName(), maxTextWidth, ClientApi.inter(), textSize
                );
                float nameHeight = lines.size() * ClientApi.inter().getHeight(setting.getName(), textSize) + (lines.size() - 1) * 2f;
                float currentY = y + nameHeight + 5f;
                
                for (var childSetting : collection.getSettings()) {
                    if (childSetting.getShown().get()) {
                        float childHeight = calculateSettingHeight(childSetting, width - 10f);
                        if (handleClick(childSetting, mouseX, mouseY, button, x + 5f, currentY, width - 10f, childHeight)) {
                            return true;
                        }
                        currentY += childHeight + 2f;
                    }
                }
            }
        }
        
        return false;
    }
    
    public static boolean handleDrag(Setting setting, double mouseX, double mouseY, float x, float y, float width, float height) {
        if (setting instanceof Slider slider && slider.getDragging()) {
            float sliderX = x + 5f;
            float sliderWidth = width - 10f;
            
            float relativeX = (float)(mouseX - sliderX);
            float progress = Math.max(0f, Math.min(1f, relativeX / sliderWidth));
            
            float newValue = slider.getMin() + progress * (slider.getMax() - slider.getMin());
            
            if (slider.getIncrements() != null && slider.getIncrements() > 0) {
                newValue = Math.round(newValue / slider.getIncrements()) * slider.getIncrements();
            }
            
            slider.applyDefault(newValue);
            return true;
        }
        
        return false;
    }
    
    public static boolean startDrag(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float height) {
        if (button == 0 && setting instanceof Slider slider) {
            if (isHovered(mouseX, mouseY, x, y, width, height)) {
                slider.setDragging(true);
                handleDrag(setting, mouseX, mouseY, x, y, width, height);
                return true;
            }
        }
        
        return false;
    }
    
    public static void stopDrag(Setting setting) {
        if (setting instanceof Slider slider) {
            slider.setDragging(false);
        }
    }
    
    private static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
