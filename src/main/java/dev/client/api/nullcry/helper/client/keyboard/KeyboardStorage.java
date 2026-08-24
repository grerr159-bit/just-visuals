package dev.client.api.nullcry.helper.client.keyboard;

import dev.client.api.nullcry.ClientApi;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class KeyboardStorage implements ClientApi {
    private static final Map<String, Integer> nameToKeyCode = new LinkedHashMap<>();

    static {
        registerAlphabet();
        registerDigits();
    }

       public static String getKey(int integer) {
        if (integer < 0) {
            return switch (integer) {
                case -100 -> I18n.translate("key.mouse.left");
                case -99 -> I18n.translate("key.mouse.right");
                case -98 -> "Middle";
                case -97 -> "M4";
                case -96 -> "M5";
                default -> "Mouse" + (integer + 101);
            };
        } else {
            String key = InputUtil.fromKeyCode(integer, -1).getTranslationKey();
            int keyboardIndex = key.indexOf("keyboard.");
            if (keyboardIndex >= 0) {
                String result = key.substring(keyboardIndex + "keyboard.".length());
                result = result.replace(".", "_").replace("grave_accent", "`");
                return capitalize(result);
            }
            return "Unknown";
        }
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public static boolean isKeyPressed(KeyBinding key) {
        if (key.getDefaultKey().getCode() == GLFW.GLFW_KEY_UNKNOWN) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getDefaultKey().getCode());
    }

    public static boolean hasKey(String keyName) {
        if (keyName == null) {
            return false;
        }
        return nameToKeyCode.containsKey(keyName.toUpperCase(Locale.ROOT));
    }

    public static int getKey(String keyName) {
        if (keyName == null) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        return nameToKeyCode.getOrDefault(keyName.toUpperCase(Locale.ROOT), GLFW.GLFW_KEY_UNKNOWN);
    }

    public static List<String> getAllKeys() {
        return new ArrayList<>(nameToKeyCode.keySet());
    }

    private static void registerAlphabet() {
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            registerKey(String.valueOf(letter), GLFW.GLFW_KEY_A + i);
        }
    }

    private static void registerDigits() {
        for (int i = 0; i <= 9; i++) {
            registerKey(String.valueOf(i), GLFW.GLFW_KEY_0 + i);
        }
    }

    private static void registerKey(String name, int code) {
        nameToKeyCode.putIfAbsent(name.toUpperCase(Locale.ROOT), code);
    }
}
