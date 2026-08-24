package dev.client.api.nullcry.uiClient.clickGui.newgui;

import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.render.core.animations.client.Animation;
import dev.client.api.nullcry.render.core.animations.client.implement.DecelerateAnimation;
import dev.client.api.nullcry.uiClient.clickGui.newgui.util.ScrollUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * Состояние GUI
 */
public class GuiState {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Анимации
    public static Animation mainAnimation = new DecelerateAnimation().setMs(200).setValue(1f);
    
    // Скролл
    public static ScrollUtil scrollUtil = new ScrollUtil();
    
    // Позиция и размеры
    @Getter @Setter public static float x;
    @Getter @Setter public static float y;
    @Getter @Setter public static float width = 516f;
    @Getter @Setter public static float height = 340f;
    
    // Текущие координаты мыши
    @Getter @Setter public static int currentMouseX = 0;
    @Getter @Setter public static int currentMouseY = 0;
    
    // Выбранная категория
    @Getter @Setter public static ModuleCategory selectedCategory = ModuleCategory.Visuals;
    
    // Модули текущей категории
    @Getter @Setter public static List<Module> modules = new ArrayList<>();
    
    // Открытые настройки модулей
    public static Set<Module> openSettingsModules = new HashSet<>();
    
    // Флаг закрытия
    @Getter @Setter public static boolean exit = false;
    
    /**
     * Сбросить состояние
     */
    public static void reset() {
        openSettingsModules.clear();
        scrollUtil.setScroll(0f);
        exit = false;
    }
}
