package dev.client.api.nullcry.uiClient.clickGui.newgui.util;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.core.msdf.core.MsdfFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для работы с текстом
 */
public class TextWrapUtil {
    
    /**
     * Обрезает текст с многоточием если не влезает
     */
    public static String truncateText(String text, float maxWidth, MsdfFont font, float size) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (font.getWidth(text, size) <= maxWidth) {
            return text;
        }
        
        String ellipsis = "...";
        float ellipsisWidth = font.getWidth(ellipsis, size);
        float availableWidth = maxWidth - ellipsisWidth;
        
        if (availableWidth <= 0) {
            return ellipsis;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String testText = result.toString() + text.charAt(i);
            if (font.getWidth(testText, size) > availableWidth) {
                break;
            }
            result.append(text.charAt(i));
        }
        
        return result.toString() + ellipsis;
    }
    
    /**
     * Разбивает текст на строки с учетом максимальной ширины (старый метод, оставлен для совместимости)
     */
    public static List<String> wrapText(String text, float maxWidth, MsdfFont font, float size) {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        // Просто обрезаем текст
        lines.add(truncateText(text, maxWidth, font, size));
        return lines;
    }
    
    /**
     * Вычисляет высоту текста с учетом переноса
     */
    public static float getWrappedHeight(String text, float maxWidth, MsdfFont font, float size, float lineSpacing) {
        List<String> lines = wrapText(text, maxWidth, font, size);
        if (lines.isEmpty()) return 0f;
        
        float lineHeight = font.getHeight(text, size);
        return lines.size() * lineHeight + (lines.size() - 1) * lineSpacing;
    }
}
