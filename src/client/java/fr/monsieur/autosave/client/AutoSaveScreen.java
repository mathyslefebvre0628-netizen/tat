package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 440;

    private final Screen parent;

    private Button toggleButton;
    private Button intervalButton;

    private boolean intervalMenuOpen = false;
    private String status = "Prêt";

    private static final IntervalOption[] INTERVALS = {
            new IntervalOption("1 MIN", 20 * 60),
            new IntervalOption("5 MIN", 20 * 60 * 5),
            new IntervalOption("10 MIN", 20 * 60 * 10),
            new IntervalOption("15 MIN", 20 * 60 * 15),
            new IntervalOption("30 MIN", 20 * 60 * 30),
            new IntervalOption("1 H", 20 * 60 * 60)
    };

    public AutoSaveScreen(Screen parent) {
        super(Component.literal("Auto Save"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        createWidgets();
    }

    private void createWidgets() {
        clearWidgets();

        int width = getWindowWidth();
        int height = getWindowHeight();

        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;

        /*
         * AUTO SAVE
         */
        toggleButton = Button.builder(
                Component.literal(
                        AutoSaveConfig.enabled ? "ON" : "OFF"
                ),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();

                    button.setMessage(
                            Component.literal(
                                    AutoSaveConfig.enabled
                                            ? "ON"
                                            : "OFF"
                            )
                    );

                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde automatique activée"
                            : "Sauvegarde automatique désactivée";
                }
        ).bounds(
                left + width - 135,
                top + 86,
                95,
                28
        ).build();

        addRenderableWidget(toggleButton);

        /*
         * INTERVALLE
         */
        intervalButton = Button.builder(
                Component.literal(getCurrentIntervalName()),
                button -> toggleIntervalMenu()
        ).bounds(
                left + width - 155,
                top + 151,
                115,
                28
        ).build();

        addRenderableWidget(intervalButton);

        /*
         * CHOISIR DOSSIER
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("SELECT FOLDER"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + width - 180,
                        top + 245,
                        145,
                        28
                ).build()
        );

        /*
         * RESTAURER
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE BACKUP"),
                        button -> openBackupScreen()
                ).bounds(
                        left + width - 180,
                        top + 330,
                        145,
                        28
                ).build()
        );

        /*
         * SAVE
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("SAVE"),
                        button -> {
                            AutoSaveConfig.save();
                            status = "Configuration enregistrée";
                        }
                ).bounds(
                        left + width - 215,
                        top + height - 43,
                        90,
                        26
                ).build()
        );

        /*
         * CLOSE
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("CLOSE"),
                        button -> onClose()
                ).bounds(
                        left + width - 115,
                        top + height - 43,
                        80,
                        26
                ).build()
        );

        /*
         * MENU INTERVALLE
         */
        if (intervalMenuOpen) {
            int menuX = left + width - 280;
            int menuY = top + 184;

            for (int i = 0; i < INTERVALS.length; i++) {
                final int index = i;

                addRenderableWidget(
                        Button.builder(
                                Component.literal(
                                        INTERVALS[i].name
                                ),
                                button ->
                                        selectInterval(
                                                INTERVALS[index]
                                        )
                        ).bounds(
                                menuX,
                                menuY + i * 27,
                                120,
                                27
                        ).build()
                );
            }
        }
    }

    private void toggleIntervalMenu() {
        intervalMenuOpen = !intervalMenuOpen;

        status = intervalMenuOpen
                ? "Choisis un intervalle"
                : "Intervalle : " + getCurrentIntervalName();

        createWidgets();
    }

    private void selectInterval(IntervalOption option) {
        AutoSaveConfig.intervalTicks = option.ticks;
        AutoSaveConfig.save();

        intervalMenuOpen = false;
        status = "Intervalle : " + option.name;

        createWidgets();
    }

    private int getWindowWidth() {
        return Math.min(WINDOW_WIDTH, this.width - 40);
    }

    private int getWindowHeight() {
        return Math.min(WINDOW_HEIGHT, this.height - 30);
    }

    private String getCurrentIntervalName() {
        int ticks = AutoSaveConfig.intervalTicks;

        for (IntervalOption option : INTERVALS) {
            if (option.ticks == ticks) {
                return option.name;
            }
        }

        return "5 MIN";
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
    }

    private void openBackupScreen() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new BackupSelectScreen(this)
            );
        }
    }

    @Override
    public void onClose() {
        AutoSaveConfig.save();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        /*
         * IMPORTANT :
         * On ne dessine PAS de fond opaque.
         * Minecraft reste visible derrière.
         *
         * Voile noir semi-transparent.
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x88000000
        );

        int width = getWindowWidth();
        int height = getWindowHeight();

        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;

        /*
         * Ombre de la fenêtre
         */
        graphics.fill(
                left + 8,
                top + 8,
                left + width + 8,
                top + height + 8,
                0x70000000
        );

        /*
         * Fenêtre principale
         */
        graphics.fill(
                left,
                top,
                left + width,
                top + height,
                0xFF090909
        );

        /*
         * Bordure
         */
        graphics.fill(
                left,
                top,
                left + width,
                top + 1,
                0xFF292929
        );

        graphics.fill(
                left,
                top + height - 1,
                left + width,
                top + height,
                0xFF292929
        );

        graphics.fill(
                left,
                top,
                left + 1,
                top + height,
                0xFF292929
        );

        graphics.fill(
                left + width - 1,
                top,
                left + width,
                top + height,
                0xFF292929
        );

        /*
         * HEADER
         */
        graphics.fill(
                left,
                top,
                left + width,
                top + 70,
                0xFF0C0C0C
        );

        graphics.text(
                this.font,
                Component.literal("AS"),
                left + 28,
                top + 23,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 55,
                top + 20,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Automatic World Backup"
                ),
                left + 55,
                top + 39,
                0xFF666666,
                false
        );

        /*
         * STATUS GLOBAL
         */
        graphics.fill(
                left + width - 72,
                top + 31,
                left + width - 66,
                top + 37,
                AutoSaveConfig.enabled
                        ? 0xFFFFFFFF
                        : 0xFF444444
        );

        graphics.text(
                this.font,
                Component.literal(
                        AutoSaveConfig.enabled
                                ? "ENABLED"
                                : "DISABLED"
                ),
                left + width - 60,
                top + 27,
                0xFF777777,
                false
        );

        /*
         * SEPARATEUR
         */
        graphics.fill(
                left + 25,
                top + 70,
                left + width - 25,
                top + 71,
                0xFF202020
        );

        /*
         * AUTOMATIC BACKUP
         */
        graphics.text(
                this.font,
                Component.literal("AUTOMATIC BACKUP"),
                left + 30,
                top + 92,
                0xFF666666,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic backup"),
                left + 30,
                top + 117,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Create a backup automatically while playing."
                ),
                left + 30,
                top + 136,
                0xFF666666,
                false
        );

        /*
         * INTERVALLE
         */
        graphics.text(
                this.font,
                Component.literal("Backup interval"),
                left + 30,
                top + 170,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Choose how often Auto Save creates a backup."
                ),
                left + 30,
                top + 188,
                0xFF666666,
                false
        );

        /*
         * LOCATION
         */
        graphics.text(
                this.font,
                Component.literal("BACKUP LOCATION"),
                left + 30,
                top + 225,
                0xFF666666,
                true
        );

        String folder = (
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
        )
                ? "No folder selected"
                : AutoSaveConfig.folder;

        if (folder.length() > 75) {
            folder =
                    "..." +
                    folder.substring(folder.length() - 72);
        }

        /*
         * CHEMIN
         */
        graphics.fill(
                left + 30,
                top + 235,
                left + width - 30,
                top + 276,
                0xFF050505
        );

        graphics.fill(
                left + 30,
                top + 235,
                left + width - 30,
                top + 236,
                0xFF222222
        );

        graphics.text(
                this.font,
                Component.literal(folder),
                left + 42,
                top + 251,
                0xFF888888,
                false
        );

        /*
         * BACKUPS
         */
        graphics.text(
                this.font,
                Component.literal("SAVED BACKUPS"),
                left + 30,
                top + 305,
                0xFF666666,
                true
        );

        int backupCount =
                AutoSaveService.listBackups().length;

        graphics.text(
                this.font,
                Component.literal(
                        backupCount
                                + (
                                backupCount == 1
                                        ? " backup available"
                                        : " backups available"
                        )
                ),
                left + 30,
                top + 324,
                0xFF666666,
                false
        );

        /*
         * STATUS
         */
        graphics.text(
                this.font,
                Component.literal(status),
                left + 30,
                top + height - 38,
                0xFF555555,
                false
        );

        /*
         * MENU INTERVALLE
         */
        if (intervalMenuOpen) {
            int menuX = left + width - 280;
            int menuY = top + 184;

            graphics.fill(
                    menuX - 2,
                    menuY - 2,
                    menuX + 122,
                    menuY + 6 * 27 + 2,
                    0xFF333333
            );

            graphics.fill(
                    menuX,
                    menuY,
                    menuX + 120,
                    menuY + 6 * 27,
                    0xFF050505
            );
        }

        /*
         * Widgets au-dessus du fond.
         */
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private record IntervalOption(
            String name,
            int ticks
    ) {
    }
}
