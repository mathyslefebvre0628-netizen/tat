package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.swing.JFileChooser;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

final class AutoSaveService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private AutoSaveService() {
    }

    static void chooseFolder(AutoSaveScreen screen) {
        EventQueue.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choisir le dossier des sauvegardes");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                Path path = chooser.getSelectedFile()
                        .toPath()
                        .toAbsolutePath()
                        .normalize();

                screen.updateFolder(path.toString());
            }
        });
    }

    static void createBackup(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();

        if (server == null
                || AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            return;
        }

        server.execute(() -> {
            try {
                if (!server.saveEverything(false, true, true)) {
                    return;
                }

                Path world = server.getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath()
                        .normalize();

                Path root = Path.of(AutoSaveConfig.folder)
                        .toAbsolutePath()
                        .normalize();

                Files.createDirectories(root);

                Path target = root.resolve(
                        "autosave_" + STAMP.format(LocalDateTime.now())
                );

                copyDirectory(world, target);

            } catch (Exception ignored) {
                // Une erreur de sauvegarde automatique ne doit pas faire planter le jeu.
            }
        });
    }

    static String[] listBackups() {
        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            return new String[0];
        }

        try (var stream = Files.list(Path.of(AutoSaveConfig.folder))) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name ->
                            name.startsWith("autosave_")
                                    || name.startsWith("backup_before_restore_"))
                    .sorted(Comparator.reverseOrder())
                    .toArray(String[]::new);

        } catch (IOException e) {
            return new String[0];
        }
    }

    static void restore(Minecraft client, String backupName) throws IOException {
        MinecraftServer server = client.getSingleplayerServer();

        if (server == null) {
            throw new IOException("Une partie solo doit être ouverte.");
        }

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            throw new IOException(
                    "Aucun dossier de sauvegarde n'est configuré."
            );
        }

        Path configuredRoot = Path.of(AutoSaveConfig.folder)
                .toAbsolutePath()
                .normalize();

        Path backup = configuredRoot
                .resolve(backupName)
                .toAbsolutePath()
                .normalize();

        if (!backup.startsWith(configuredRoot)
                || !Files.isDirectory(backup)) {
            throw new IOException("Sauvegarde invalide.");
        }

        server.saveEverything(false, true, true);

        Path world = server.getWorldPath(LevelResource.ROOT)
                .toAbsolutePath()
                .normalize();

        Path safety = configuredRoot.resolve(
                "backup_before_restore_"
                        + STAMP.format(LocalDateTime.now())
        );

        copyDirectory(world, safety);

        deleteDirectory(world);

        copyDirectory(backup, world);
    }

    private static void copyDirectory(Path source, Path target)
            throws IOException {

        Files.createDirectories(target);

        try (var stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destination = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(
                                path,
                                destination,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES
                        );
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void deleteDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
