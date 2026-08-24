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
import java.nio.file.StandardCopyOption;

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
        loadLocalVersion();
        checkForUpdatesAndAutoUpdate();
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
                        autoDownloadAndRestart();
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
            }
        }, "Updater-Check").start();
    }

    private void autoDownloadAndRestart() {
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
                    Path tempFile = mc.runDirectory.toPath().resolve("config").resolve("just-update-temp.jar");

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

                    Path modsDir = mc.runDirectory.toPath().resolve("mods");
                    Path currentJar = modsDir.resolve(JAR_NAME);
                    Path backupJar = mc.runDirectory.toPath().resolve("config").resolve("just-backup.jar");

                    if (Files.exists(currentJar)) {
                        Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(tempFile, currentJar, StandardCopyOption.REPLACE_EXISTING);

                    downloadProgress = 1.0f;
                    downloading = false;
                    updateAvailable = false;
                    updateReady = true;
                    saveLocalVersion(remoteVersion);

                    restartGame();
                } else {
                    downloading = false;
                }
                conn.disconnect();
            } catch (Exception e) {
                downloading = false;
            }
        }, "Updater-AutoDownload").start();
    }

    private void restartGame() {
        try {
            Path mcDir = mc.runDirectory.toPath();
            Path scriptsDir = mcDir.resolve("config").resolve("just-updater");
            Files.createDirectories(scriptsDir);

            String javaBin = ProcessHandle.current().info().command().orElse("java");

            Path batFile;
            Path psFile;

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                batFile = scriptsDir.resolve("restart.bat");
                String batContent = "@echo off\n"
                        + "timeout /t 3 /nobreak >nul\n"
                        + "del \"" + currentJarPath() + "\" 2>nul\n"
                        + "move \"" + mcDir.resolve("config").resolve("just-backup.jar") + "\" \"" + currentJarPath() + "\" 2>nul\n"
                        + "start \"\" \"%APPDATA%\\.minecraft\\mods\\" + JAR_NAME + "\"\n"
                        + "del \"%~f0\"\n";
                Files.writeString(batFile, batContent);

                new ProcessBuilder("cmd", "/c", "start", "", batFile.toAbsolutePath().toString())
                        .directory(mcDir.toFile())
                        .start();
            } else {
                psFile = scriptsDir.resolve("restart.sh");
                String shContent = "#!/bin/bash\n"
                        + "sleep 3\n"
                        + "rm -f \"" + currentJarPath() + "\"\n"
                        + "mv \"" + mcDir.resolve("config").resolve("just-backup.jar") + "\" \"" + currentJarPath() + "\" 2>/dev/null\n"
                        + "rm -f \"" + psFile.toAbsolutePath() + "\"\n";
                Files.writeString(psFile, shContent);
                Runtime.getRuntime().exec(new String[]{"bash", psFile.toAbsolutePath().toString()});
            }

            Thread.sleep(500);
            MinecraftClient.getInstance().scheduleStop();
        } catch (Exception e) {
            MinecraftClient.getInstance().scheduleStop();
        }
    }

    private String currentJarPath() {
        return mc.runDirectory.toPath().resolve("mods").resolve(JAR_NAME).toAbsolutePath().toString();
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
                    Path tempFile = mc.runDirectory.toPath().resolve("config").resolve("just-update-temp.jar");

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

                    Path currentJar = mc.runDirectory.toPath().resolve("mods").resolve(JAR_NAME);
                    Path backupJar = mc.runDirectory.toPath().resolve("config").resolve("just-backup.jar");

                    if (Files.exists(currentJar)) {
                        Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(tempFile, currentJar, StandardCopyOption.REPLACE_EXISTING);

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
