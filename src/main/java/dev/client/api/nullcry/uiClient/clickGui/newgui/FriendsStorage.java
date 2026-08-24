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

public class FriendsStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("just-friends.json");

    private static List<String> friends = new ArrayList<>();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                Type type = new TypeToken<List<String>>() {}.getType();
                friends = GSON.fromJson(json, type);
                if (friends == null) friends = new ArrayList<>();
            }
        } catch (Exception e) {
            friends = new ArrayList<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(friends));
        } catch (Exception e) {
            // ignore
        }
    }

    public static boolean addFriend(String name) {
        if (name == null || name.isBlank()) return false;
        String trimmed = name.trim();
        if (friends.contains(trimmed)) return false;
        friends.add(trimmed);
        save();
        return true;
    }

    public static void removeFriend(String name) {
        friends.remove(name);
        save();
    }

    public static List<String> getFriends() {
        return friends;
    }
}
