package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.PacketEvent;
import dev.client.api.nullcry.events.core.world.FogEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.SelectElements;
import dev.client.api.nullcry.modules.settings.Slider;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.time.LocalTime;

public class CustomWorld extends Module {
    public static CustomWorld INSTANCE;

    public CustomWorld() {
        super("Ambience", ModuleCategory.Visuals, "Позволяет изменить время суток и настроить туман для создания атмосферы");
    }

    SelectElements mode = new SelectElements("Отображаемые элементы", () -> true)
            .set("Изменение времени", "Изменение тумана")
            .defaultValue("Изменение времени")
            .register(this);
    
    ModeElement time = new ModeElement("Изменение времени в мире", () -> mode.isSelected("Изменение времени"))
            .set("Раннее утро", "Восход", "День", "Полдень", "Закат с красным солнцем", "Ночь", "Реальное время")
            .defaultValue("День")
            .register(this);

    Slider distance = new Slider("Дистанция тумана", () -> mode.isSelected("Изменение тумана"))
            .set(10, 225, 5)
            .defaultValue(80)
            .register(this);

    ModeElement colorMode = new ModeElement("Режим цвета тумана", () -> mode.isSelected("Изменение тумана"))
            .set("Клиентский цвет", "Кастомный")
            .defaultValue("Клиентский цвет")
            .register(this);
    
    ColorPicker customColor = new ColorPicker("Цвет тумана", () -> colorMode.isSelected("Кастомный"))
            .set(-1)
            .defaultValue(-1)
            .register(this);

    long newTime;

    @Subscribe
    public void onFog(FogEvent e) {
        if (mode.isSelected("Изменение тумана")) {
            e.setDistance(255 - distance.getValue());
            e.setColor(colorMode.isSelected("Клиентский цвет") ? Interface.INSTANCE.getMainColor() : customColor.getColorRGBA());
            e.setCancelled(true);
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket packet) {
            if (mc.world == null) return;

            event.setCancelled(true);

            switch (time.getValue()) {
                case "Раннее утро":
                    newTime = 0L;
                    break;
                case "Восход":
                    newTime = 1000L;
                    break;
                case "День":
                    newTime = 6000L;
                    break;
                case "Полдень":
                    newTime = 12000L;
                    break;
                case "Закат с красным солнцем":
                    newTime = 13000L;
                    break;
                case "Ночь":
                    newTime = 18000L;
                    break;
                case "Реальное время":
                    newTime = getRealWorldTime();
                    break;
                default:
                    return;
            }

            mc.world.setTime(newTime, newTime, true);
        }
    }

    private long getRealWorldTime() {
        LocalTime now = LocalTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        int adjustedHours = (hours + 18) % 24;
        long mcTime = (adjustedHours * 1000) + (minutes * (1000 / 60));

        return mcTime % 24000;
    }
}
