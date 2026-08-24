package dev.client.modules.core.hud;

import dev.client.Just;
import dev.client.api.nullcry.modules.Module;

public class HudModuleHelper {
    
    public static boolean isModuleEnabled(String moduleName) {
        Module module = Just.getInstance().getModuleManager().stream()
            .filter(m -> m.getName().equals(moduleName))
            .findFirst()
            .orElse(null);
        return module != null && module.isEnabled();
    }
    
    public static boolean isWatermarkEnabled() {
        return isModuleEnabled("Watermark");
    }
    
    public static boolean isPlayerInfoEnabled() {
        return isModuleEnabled("PlayerInfo");
    }
    
    public static boolean isKeybindsEnabled() {
        return isModuleEnabled("Keybinds");
    }
    
    public static boolean isPotionsEnabled() {
        return isModuleEnabled("Potions");
    }
    
    public static boolean isCooldownsEnabled() {
        return isModuleEnabled("Cooldowns");
    }
    
    public static boolean isTargetHudEnabled() {
        return isModuleEnabled("TargetHud");
    }
    
    public static boolean isArmorHudEnabled() {
        return isModuleEnabled("ArmorHud");
    }
    
    public static boolean isInventoryHudEnabled() {
        return isModuleEnabled("InventoryHud");
    }
    
    public static boolean isPartyListEnabled() {
        return isModuleEnabled("PartyList");
    }
    
    public static boolean isScoreboardEnabled() {
        return isModuleEnabled("Scoreboard");
    }
    
    public static boolean isNotificationsEnabled() {
        return true; // Notifications всегда включены
    }
    
    public static boolean isHotBarEnabled() {
        return true; // HotBar всегда включен
    }
    
    public static boolean isOverlayEnabled() {
        return true; // Overlay всегда включен
    }
}
