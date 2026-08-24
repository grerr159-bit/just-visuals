package dev.client.modules.core.render;

import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;

import java.awt.*;

public class HitColor extends Module {
    public static HitColor INSTANCE;

    public HitColor() {
        super("HitColor", ModuleCategory.Visuals, "Изменяет цвет подсветки при ударе");
        INSTANCE = this;
    }

    ModeElement mode = new ModeElement("Режим цвета", () -> true)
            .set("Кастомный", "Радужный", "Клиентский")
            .defaultValue("Кастомный")
            .register(this);

    ColorPicker customColor = new ColorPicker("Цвет удара", () -> mode.isSelected("Кастомный"))
            .set(new Color(255, 0, 0, 100).getRGB())
            .defaultValue(new Color(255, 0, 0, 100).getRGB())
            .register(this);

    Slider alpha = new Slider("Прозрачность", () -> true)
            .set(0, 255, 1)
            .defaultValue(100)
            .register(this);

    Slider duration = new Slider("Длительность (тики)", () -> true)
            .set(1, 20, 1)
            .defaultValue(10)
            .register(this);

    CheckBox fadeOut = new CheckBox("Плавное затухание", () -> true)
            .defaultValue(true)
            .register(this);

    public int getHitColor(float progress) {
        if (!isEnabled()) {
            return new Color(255, 0, 0, 100).getRGB();
        }

        int baseAlpha = alpha.getValue().intValue();
        int finalAlpha = fadeOut.getEnabled() 
            ? (int) (baseAlpha * (1.0f - progress))
            : baseAlpha;

        if (mode.isSelected("Радужный")) {
            float hue = (System.currentTimeMillis() % 3000) / 3000.0f;
            Color rainbow = Color.getHSBColor(hue, 1.0f, 1.0f);
            return new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), finalAlpha).getRGB();
        } else if (mode.isSelected("Клиентский")) {
            Color clientColor = Interface.INSTANCE != null 
                ? new Color(Interface.INSTANCE.getMainColor(), true)
                : new Color(255, 255, 255, 255);
            return new Color(clientColor.getRed(), clientColor.getGreen(), clientColor.getBlue(), finalAlpha).getRGB();
        } else {
            Color custom = new Color(customColor.getColorRGBA(), true);
            return new Color(custom.getRed(), custom.getGreen(), custom.getBlue(), finalAlpha).getRGB();
        }
    }

    public int getDuration() {
        return duration.getValue().intValue();
    }
}
