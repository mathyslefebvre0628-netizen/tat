package fr.monsieur.autosave.client;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import javax.swing.JFileChooser;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class AutoSaveService {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private AutoSaveService() {}

    static void chooseFolder(AutoSaveScreen screen) {
        EventQueue.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choisir le dossier des sauvegardes");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString();
                Minecraft.getInstance().execute(() -> screen.updateFolder(path));
            }
        });
    }

    static void createBackup(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null || AutoSaveConfig.folder.isBlank()) return;

        server.execute(() -> {
            try {
                server.saveEverything(false, true, true);
                Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                Path root = Path.of(AutoSaveConfig.folder).toAbsolutePath().normalize();
                Files.createDirectories(root);
                Path target = uniqueTarget(root, "autosave_" + STAMP.format(LocalDateTime.now()));
                copyDirectory(world, target);
            } catch (Exception ignored) {
            }
        });
    }

    static String[] listBackups() {
        if (AutoSaveConfig.folder.isBlank()) return new String[0];
        try (var stream = Files.list(Path.of(AutoSaveConfig.folder))) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("autosave_") || name.startsWith("backup_before_restore_"))
                    .sorted(Comparator.reverseOrder())
                    .toArray(String[]::new);
        } catch (IOException e) {
            return new String[0];
        }
    }

    static String restore(Minecraft client, String backupName) throws IOException {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) throw new IOException("Une partie solo doit être ouverte.");
        if (AutoSaveConfig.folder.isBlank()) throw new IOException("Aucun dossier de sauvegarde configuré.");

        Path root = Path.of(AutoSaveConfig.folder).toAbsolutePath().normalize();
        Path backup = root.resolve(backupName).normalize();
        if (!backup.startsWith(root) || !Files.isDirectory(backup)) {
            throw new IOException("Sauvegarde invalide.");
        }

        server.saveEverything(false, true, true);
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path safety = uniqueTarget(root, "backup_before_restore_" + STAMP.format(LocalDateTime.now()));
        copyDirectory(world, safety);

        deleteDirectory(world);
        copyDirectory(backup, world);
        return backupName;
    }

    private static Path uniqueTarget(Path root, String baseName) throws IOException {
        Path target = root.resolve(baseName);
        int i = 1;
        while (Files.exists(target)) {
            target = root.resolve(baseName + "_" + i++);
        }
        Files.createDirectories(target);
        return target;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path destination = target.resolve(source.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new BackupCopyException(e);
                }
            });
        } catch (BackupCopyException e) {
            throw e.io;
        }
    }

    private static final class BackupCopyException extends RuntimeException {
        private final IOException io;

        private BackupCopyException(IOException io) {
            this.io = io;
        }
    }
}
