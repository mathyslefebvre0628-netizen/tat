package fr.monsieur.autosave.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

final class AutoSaveConfig {
    static boolean enabled = true;
    static String folder = "";
    static int intervalTicks = 6000;

    private AutoSaveConfig() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("autosave.properties");
    }

    static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                int sep = line.indexOf('=');
                if (sep <= 0) continue;
                String key = line.substring(0, sep).trim();
                String value = line.substring(sep + 1);
                switch (key) {
                    case "enabled" -> enabled = Boolean.parseBoolean(value);
                    case "folder" -> folder = value;
                    case "intervalTicks" -> intervalTicks = Math.max(20, Integer.parseInt(value));
                }
            }
        } catch (Exception ignored) {
            // Keep defaults if configuration is unreadable.
        }
    }

    static void save() {
        try {
            Files.createDirectories(file().getParent());
            String content = "enabled=" + enabled + "\n"
                    + "folder=" + folder.replace("\n", "").replace("\r", "") + "\n"
                    + "intervalTicks=" + intervalTicks + "\n";
            Files.writeString(file(), content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
