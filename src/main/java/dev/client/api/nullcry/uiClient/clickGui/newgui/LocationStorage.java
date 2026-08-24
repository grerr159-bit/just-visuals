package dev.client.api.nullcry.uiClient.clickGui.newgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocationStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("just-locations.json");

    private static List<Waypoint> waypoints = new ArrayList<>();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                Type type = new TypeToken<List<Waypoint>>() {}.getType();
                waypoints = GSON.fromJson(json, type);
                if (waypoints == null) waypoints = new ArrayList<>();
            }
        } catch (Exception e) {
            waypoints = new ArrayList<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(waypoints));
        } catch (Exception e) {
            // ignore
        }
    }

    public static boolean addWaypoint(String name, int x, int y, int z, int color, int keyBind) {
        return addWaypoint(name, x, y, z, color, keyBind, true);
    }

    public static boolean addWaypoint(String name, int x, int y, int z, int color, int keyBind, boolean enabled) {
        if (name == null || name.isBlank()) return false;
        String trimmed = name.trim();
        for (Waypoint w : waypoints) {
            if (w.name.equalsIgnoreCase(trimmed)) return false;
        }
        waypoints.add(new Waypoint(trimmed, x, y, z, color, keyBind, enabled));
        save();
        return true;
    }

    public static void removeWaypoint(String name) {
        waypoints.removeIf(w -> w.name.equalsIgnoreCase(name));
        save();
    }

    public static List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public static void toggleWaypoint(String name) {
        for (Waypoint w : waypoints) {
            if (w.name.equalsIgnoreCase(name)) {
                w.enabled = !w.enabled;
                save();
                return;
            }
        }
    }

    public static Waypoint findByKey(int keyCode) {
        for (Waypoint w : waypoints) {
            if (w.keyBind == keyCode) return w;
        }
        return null;
    }

    public static class Waypoint {
        public String name;
        public int x, y, z;
        public int color;
        public int keyBind;
        public boolean enabled;

        public Waypoint(String name, int x, int y, int z, int color, int keyBind, boolean enabled) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.keyBind = keyBind;
            this.enabled = enabled;
        }
    }
}
