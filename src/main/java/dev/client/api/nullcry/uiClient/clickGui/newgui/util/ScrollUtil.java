package dev.client.api.nullcry.uiClient.clickGui.newgui.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Утилита для скролла
 */
public class ScrollUtil {
    @Getter @Setter private float scroll = 0f;
    @Getter @Setter private float max = 0f;
    @Getter @Setter private float speed = 10f;
    @Getter @Setter private boolean enabled = true;
    
    private float target = 0f;
    
    public void addScroll(double amount) {
        if (!enabled) return;
        target -= (float) amount * speed; // Инвертировано
        target = MathHelper.clamp(target, 0f, max);
    }
    
    public void update() {
        if (!enabled) return;
        scroll = MathHelper.lerp(0.3f, scroll, target);
    }
    
    public void setMax(float contentHeight, float viewHeight) {
        this.max = Math.max(0f, contentHeight - viewHeight);
        target = MathHelper.clamp(target, 0f, max);
    }
    
    public void render(DrawContext context, float x, float y, float width, float height, float alpha) {
        if (max <= 0f) return;
        
        // Фон скроллбара
        int bgColor = (int)(50 * alpha) << 24 | 0x808080;
        RenderHelper.drawRoundedRect(context, x, y, width, height, 1f, bgColor);
        
        // Ползунок
        float scrollbarHeight = Math.max(20f, height * (height / (max + height)));
        float scrollbarY = (height - scrollbarHeight) * (scroll / max);
        int thumbColor = (int)(150 * alpha) << 24 | 0xFFFFFF;
        RenderHelper.drawRoundedRect(context, x, y + scrollbarY, width, scrollbarHeight, 1f, thumbColor);
    }
}
