package dev.client.api.nullcry.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.client.api.nullcry.ClientApi;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Updater implements ClientApi {
    public static final Updater INSTANCE = new Updater();

    private static final String GITHUB_OWNER = "grerr159-bit";
    private static final String GITHUB_REPO = "just-visuals";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/main/version.json";
    private static final String JAR_NAME = "Just-Visuals-1.21.4.jar";

    private String localVersion = "1.0.0";
    private String remoteVersion = null;
    private String downloadUrl = null;
    private String changelog = "";
    private boolean updateAvailable = false;
    private boolean downloading = false;
    private float downloadProgress = 0f;
    private boolean updateReady = false;

    public void init() {
        applyPendingUpdate();
        loadLocalVersion();
        checkForUpdatesAndAutoUpdate();
    }

    private Path modsDir() {
        return mc.runDirectory.toPath().resolve("mods");
    }

    private Path currentJar() {
        return modsDir().resolve(JAR_NAME);
    }

    private Path newJar() {
        return modsDir().resolve(JAR_NAME + ".new");
    }

    private void applyPendingUpdate() {
        try {
            Path pending = newJar();
            if (Files.exists(pending)) {
                Path current = currentJar();
                if (Files.exists(current)) {
                    Files.delete(current);
                }
                Files.move(pending, current);
            }
        } catch (Exception e) {
        }
    }

    private void loadLocalVersion() {
        try {
            Path versionFile = mc.runDirectory.toPath().resolve("config").resolve("just-version.txt");
            if (Files.exists(versionFile)) {
                localVersion = Files.readString(versionFile).trim();
            }
        } catch (Exception e) {
        }
    }

    public void saveLocalVersion(String version) {
        try {
            Path configDir = mc.runDirectory.toPath().resolve("config");
            Files.createDirectories(configDir);
            Files.writeString(configDir.resolve("just-version.txt"), version);
            localVersion = version;
        } catch (Exception e) {
        }
    }

    public void checkForUpdatesAndAutoUpdate() {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    String json = new String(conn.getInputStream().readAllBytes());
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                    remoteVersion = obj.get("version").getAsString();
                    downloadUrl = obj.has("download_url") ? obj.get("download_url").getAsString() : null;
                    changelog = obj.has("changelog") ? obj.get("changelog").getAsString() : "";

                    updateAvailable = !localVersion.equals(remoteVersion);

                    if (updateAvailable && downloadUrl != null) {
                        autoDownload();
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
            }
        }, "Updater-Check").start();
    }

    private void autoDownload() {
        downloading = true;
        downloadProgress = 0f;

        new Thread(() -> {
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    int fileSize = conn.getContentLength();
                    Path tempFile = newJar();

                    Files.createDirectories(modsDir());

                    try (InputStream in = conn.getInputStream();
                         OutputStream out = Files.newOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        long totalRead = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            if (fileSize > 0) {
                                downloadProgress = (float) totalRead / fileSize;
                            }
                        }
                    }

                    downloadProgress = 1.0f;
                    downloading = false;
                    updateAvailable = false;
                    updateReady = true;
                    saveLocalVersion(remoteVersion);
                } else {
                    downloading = false;
                }
                conn.disconnect();
            } catch (Exception e) {
                downloading = false;
            }
        }, "Updater-AutoDownload").start();
    }

    public void checkForUpdates() {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    String json = new String(conn.getInputStream().readAllBytes());
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                    remoteVersion = obj.get("version").getAsString();
                    downloadUrl = obj.has("download_url") ? obj.get("download_url").getAsString() : null;
                    changelog = obj.has("changelog") ? obj.get("changelog").getAsString() : "";

                    updateAvailable = !localVersion.equals(remoteVersion);
                }
                conn.disconnect();
            } catch (Exception e) {
            }
        }, "Updater-Check").start();
    }

    public void downloadUpdate() {
        if (downloadUrl == null || downloading) return;
        downloading = true;
        downloadProgress = 0f;

        new Thread(() -> {
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    int fileSize = conn.getContentLength();
                    Path tempFile = newJar();

                    Files.createDirectories(modsDir());

                    try (InputStream in = conn.getInputStream();
                         OutputStream out = Files.newOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        long totalRead = 0;
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            if (fileSize > 0) {
                                downloadProgress = (float) totalRead / fileSize;
                            }
                        }
                    }

                    downloadProgress = 1.0f;
                    downloading = false;
                    updateAvailable = false;
                    updateReady = true;
                    saveLocalVersion(remoteVersion);
                }
                conn.disconnect();
            } catch (Exception e) {
                downloading = false;
            }
        }, "Updater-Download").start();
    }

    public String getLocalVersion() { return localVersion; }
    public String getRemoteVersion() { return remoteVersion; }
    public String getChangelog() { return changelog; }
    public boolean isUpdateAvailable() { return updateAvailable; }
    public boolean isDownloading() { return downloading; }
    public float getDownloadProgress() { return downloadProgress; }
    public boolean isUpdateReady() { return updateReady; }
}
