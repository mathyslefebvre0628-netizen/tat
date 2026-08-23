package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

final class AutoSaveService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private AutoSaveService() {
    }

    /**
     * Ouvre le sélecteur de dossier Windows natif.
     *
     * Le dialogue est lancé dans un thread séparé pour ne pas bloquer
     * le thread principal de Minecraft.
     */
    static void chooseFolder(AutoSaveScreen screen) {
        CompletableFuture
                .supplyAsync(AutoSaveService::openWindowsFolderPicker)
                .thenAccept(path -> {
                    if (path == null || path.isBlank()) {
                        return;
                    }

                    Minecraft minecraft = Minecraft.getInstance();

                    minecraft.execute(() ->
                            screen.updateFolder(
                                    Path.of(path)
                                            .toAbsolutePath()
                                            .normalize()
                                            .toString()
                            )
                    );
                });
    }

    /**
     * Lance le sélecteur de dossiers Windows avec PowerShell.
     */
    private static String openWindowsFolderPicker() {
        String script = """
                Add-Type -AssemblyName System.Windows.Forms
                Add-Type -AssemblyName System.Drawing

                $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
                $dialog.Description = 'Choisir le dossier des sauvegardes'
                $dialog.UseDescriptionForTitle = $true
                $dialog.ShowNewFolderButton = $true

                $result = $dialog.ShowDialog()

                if ($result -eq [System.Windows.Forms.DialogResult]::OK) {
                    [Console]::WriteLine($dialog.SelectedPath)
                }
                """;

        Process process = null;

        try {
            process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-STA",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    script
            )
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        output.append(line.trim());
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return null;
            }

            String result = output.toString().trim();

            return result.isBlank() ? null : result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;

        } catch (Exception e) {
            return null;

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Crée une sauvegarde automatique.
     */
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

                Path world = server
                        .getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath()
                        .normalize();

                Path root = Path
                        .of(AutoSaveConfig.folder)
                        .toAbsolutePath()
                        .normalize();

                Files.createDirectories(root);

                Path target = root.resolve(
                        "autosave_" + STAMP.format(LocalDateTime.now())
                );

                copyDirectory(world, target);

            } catch (Exception ignored) {
                // Une erreur de sauvegarde ne doit pas faire planter Minecraft.
            }
        });
    }

    /**
     * Retourne toutes les sauvegardes disponibles.
     */
    static String[] listBackups() {
        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            return new String[0];
        }

        Path root;

        try {
            root = Path
                    .of(AutoSaveConfig.folder)
                    .toAbsolutePath()
                    .normalize();
        } catch (Exception e) {
            return new String[0];
        }

        if (!Files.isDirectory(root)) {
            return new String[0];
        }

        try (var stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name ->
                            name.startsWith("autosave_")
                                    || name.startsWith(
                                    "backup_before_restore_"
                            )
                    )
                    .sorted(Comparator.reverseOrder())
                    .toArray(String[]::new);

        } catch (IOException e) {
            return new String[0];
        }
    }

    /**
     * Restaure une sauvegarde.
     *
     * Une copie de sécurité du monde actuel est créée avant
     * de remplacer son contenu.
     */
    static void restore(
            Minecraft client,
            String backupName
    ) throws IOException {

        MinecraftServer server =
                client.getSingleplayerServer();

        if (server == null) {
            throw new IOException(
                    "Une partie solo doit être ouverte."
            );
        }

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            throw new IOException(
                    "Aucun dossier de sauvegarde n'est configuré."
            );
        }

        Path configuredRoot = Path
                .of(AutoSaveConfig.folder)
                .toAbsolutePath()
                .normalize();

        Path backup = configuredRoot
                .resolve(backupName)
                .toAbsolutePath()
                .normalize();

        if (!backup.startsWith(configuredRoot)
                || !Files.isDirectory(backup)) {
            throw new IOException(
                    "Sauvegarde invalide."
            );
        }

        /*
         * Sauvegarde du monde avant restauration.
         */
        if (!server.saveEverything(false, true, true)) {
            throw new IOException(
                    "Impossible de sauvegarder le monde actuel."
            );
        }

        Path world = server
                .getWorldPath(LevelResource.ROOT)
                .toAbsolutePath()
                .normalize();

        /*
         * Sauvegarde de sécurité.
         */
        Path safety = configuredRoot.resolve(
                "backup_before_restore_"
                        + STAMP.format(LocalDateTime.now())
        );

        copyDirectory(world, safety);

        /*
         * Remplacement du monde.
         */
        deleteDirectory(world);

        copyDirectory(backup, world);
    }

    /**
     * Copie récursive d'un dossier.
     */
    private static void copyDirectory(
            Path source,
            Path target
    ) throws IOException {

        if (!Files.exists(source)) {
            throw new IOException(
                    "Source inexistante : " + source
            );
        }

        Files.createDirectories(target);

        try (var stream = Files.walk(source)) {

            stream.forEach(path -> {
                try {
                    Path relative =
                            source.relativize(path);

                    Path destination =
                            target.resolve(relative);

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

    /**
     * Supprime récursivement un dossier.
     */
    private static void deleteDirectory(
            Path root
    ) throws IOException {

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

    private static final class StringBuilderHolder {
        private StringBuilderHolder() {
        }
    }
}
