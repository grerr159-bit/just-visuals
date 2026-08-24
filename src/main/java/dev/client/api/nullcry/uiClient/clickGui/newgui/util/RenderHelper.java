package dev.client.api.nullcry.uiClient.clickGui.newgui.util;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * Wrapper для рендеринга GUI элементов
 */
public class RenderHelper implements ClientApi {
    
    /**
     * Нарисовать прямоугольник
     */
    public static void drawRect(DrawContext context, float x, float y, float width, float height, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        
        ClientApi.rectangle()
            .size(new SizeState(width, height))
            .color(new QuadColorState(new Color(color, true)))
            .build()
            .render(matrix, x, y);
    }
    
    /**
     * Нарисовать скругленный прямоугольник
     */
    public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        
        ClientApi.rectangle()
            .size(new SizeState(width, height))
            .radius(new QuadRadiusState(radius))
            .color(new QuadColorState(new Color(color, true)))
            .build()
            .render(matrix, x, y);
    }
    
    /**
     * Нарисовать обводку скругленного прямоугольника
     */
    public static void drawRoundedRectOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        
        ClientApi.outline()
            .size(new SizeState(width, height))
            .radius(new QuadRadiusState(radius))
            .thickness(thickness)
            .color(new QuadColorState(new Color(color, true)))
            .build()
            .render(matrix, x, y);
    }
}
