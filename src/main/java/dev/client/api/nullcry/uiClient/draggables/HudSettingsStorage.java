package dev.client.api.nullcry.uiClient.draggables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.client.Just;
import dev.client.api.nullcry.uiClient.draggables.core.*;

import java.io.File;
import java.nio.file.Files;

public class HudSettingsStorage {
    private static final File file = new File(Just.getInstance().getFilesDir(), "hud_settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            savePotions(root);
            saveCooldowns(root);
            saveWatermark(root);
            saveTargetHud(root);
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), GSON.toJson(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try {
            if (!file.exists()) return;
            String json = Files.readString(file.toPath());
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;
            loadPotions(root);
            loadCooldowns(root);
            loadWatermark(root);
            loadTargetHud(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePotions(JsonObject root) {
        Potions p = find(Potions.class);
        if (p == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("bgColor", p.bgColor.getColorRGBA());
        o.addProperty("bgAlpha", p.bgAlpha.getValue());
        o.addProperty("borderRadius", p.borderRadius.getValue());
        o.addProperty("glass", p.glass.getEnabled());
        o.addProperty("showAlways", p.showAlways.getEnabled());
        root.add("potions", o);
    }

    private static void loadPotions(JsonObject root) {
        if (!root.has("potions")) return;
        Potions p = find(Potions.class);
        if (p == null) return;
        JsonObject o = root.getAsJsonObject("potions");
        if (o.has("bgColor")) p.bgColor.set(o.get("bgColor").getAsInt());
        if (o.has("bgAlpha")) p.bgAlpha.applyDefault(o.get("bgAlpha").getAsFloat());
        if (o.has("borderRadius")) p.borderRadius.applyDefault(o.get("borderRadius").getAsFloat());
        if (o.has("glass")) p.glass.applyDefault(o.get("glass").getAsBoolean());
        if (o.has("showAlways")) p.showAlways.applyDefault(o.get("showAlways").getAsBoolean());
    }

    private static void saveCooldowns(JsonObject root) {
        Cooldowns c = find(Cooldowns.class);
        if (c == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("bgColor", c.bgColor.getColorRGBA());
        o.addProperty("bgAlpha", c.bgAlpha.getValue());
        o.addProperty("borderRadius", c.borderRadius.getValue());
        o.addProperty("glass", c.glass.getEnabled());
        root.add("cooldowns", o);
    }

    private static void loadCooldowns(JsonObject root) {
        if (!root.has("cooldowns")) return;
        Cooldowns c = find(Cooldowns.class);
        if (c == null) return;
        JsonObject o = root.getAsJsonObject("cooldowns");
        if (o.has("bgColor")) c.bgColor.set(o.get("bgColor").getAsInt());
        if (o.has("bgAlpha")) c.bgAlpha.applyDefault(o.get("bgAlpha").getAsFloat());
        if (o.has("borderRadius")) c.borderRadius.applyDefault(o.get("borderRadius").getAsFloat());
        if (o.has("glass")) c.glass.applyDefault(o.get("glass").getAsBoolean());
    }

    private static void saveWatermark(JsonObject root) {
        Watermark w = find(Watermark.class);
        if (w == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("bgColor", w.bgColor.getColorRGBA());
        o.addProperty("bgAlpha", w.bgAlpha.getValue());
        o.addProperty("borderRadius", w.borderRadius.getValue());
        o.addProperty("glass", w.glass.getEnabled());
        root.add("watermark", o);
    }

    private static void loadWatermark(JsonObject root) {
        if (!root.has("watermark")) return;
        Watermark w = find(Watermark.class);
        if (w == null) return;
        JsonObject o = root.getAsJsonObject("watermark");
        if (o.has("bgColor")) w.bgColor.set(o.get("bgColor").getAsInt());
        if (o.has("bgAlpha")) w.bgAlpha.applyDefault(o.get("bgAlpha").getAsFloat());
        if (o.has("borderRadius")) w.borderRadius.applyDefault(o.get("borderRadius").getAsFloat());
        if (o.has("glass")) w.glass.applyDefault(o.get("glass").getAsBoolean());
    }

    private static void saveTargetHud(JsonObject root) {
        TargetHud t = find(TargetHud.class);
        if (t == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("bgColor", t.bgColor.getColorRGBA());
        o.addProperty("bgAlpha", t.bgAlpha.getValue());
        o.addProperty("borderRadius", t.borderRadius.getValue());
        o.addProperty("glass", t.glass.getEnabled());
        root.add("targethud", o);
    }

    private static void loadTargetHud(JsonObject root) {
        if (!root.has("targethud")) return;
        TargetHud t = find(TargetHud.class);
        if (t == null) return;
        JsonObject o = root.getAsJsonObject("targethud");
        if (o.has("bgColor")) t.bgColor.set(o.get("bgColor").getAsInt());
        if (o.has("bgAlpha")) t.bgAlpha.applyDefault(o.get("bgAlpha").getAsFloat());
        if (o.has("borderRadius")) t.borderRadius.applyDefault(o.get("borderRadius").getAsFloat());
        if (o.has("glass")) t.glass.applyDefault(o.get("glass").getAsBoolean());
    }

    @SuppressWarnings("unchecked")
    private static <T> T find(Class<T> clazz) {
        try {
            var field = clazz.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
