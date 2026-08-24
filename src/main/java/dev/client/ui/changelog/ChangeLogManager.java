package dev.client.ui.changelog;

import java.util.ArrayList;
import java.util.List;

public class ChangeLogManager {
    private final List<ChangeLog> changeLogs = new ArrayList<>();
    
    public ChangeLogManager() {
        addLog("=== Just Visuals — Changelog ===");
        addLog("");
        addLog("▸ 7 анимаций меча: Swipe, Spin, Slash, Snap, Wave, Flick");
        addLog("▸ Discord: Playing with Just Visuals");
        addLog("▸ MotionBlur — максимальная плавность");
        addLog("▸ Стекло: мягкая тень вокруг HUD");
        addLog("▸ Стекло: размытие 50 + обводка");
        addLog("▸ Стекло: прозрачность, скругление");
        addLog("▸ RGB пикер цвета фона");
        addLog("▸ Configs раздел в клик гуи");
        addLog("▸ Potions/Cooldowns/Watermark настройки");
        addLog("▸ Fullbright фильтр Night Vision");
        addLog("▸ Оптимизация частиц и свечения");
        addLog("");
        addLog("▸ Пиксельный логотип JUST");
        addLog("▸ Текстура фона главного экрана");
        addLog("▸ Watermark: FPS, Ping, ник");
        addLog("▸ Клик ГУИ увеличен");
        addLog("▸ Убраны зазоры пикселей");
    }
    
    public void addLog(String text) {
        changeLogs.add(new ChangeLog(text));
    }
    
    public List<ChangeLog> getChangeLogs() {
        return changeLogs;
    }
}
