package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

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
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();

        Layout layout = calculateLayout();

        toggleButton = Button.builder(
                Component.literal(
                        AutoSaveConfig.enabled ? "ON" : "OFF"
                ),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();

                    button.setMessage(
                            Component.literal(
                                    AutoSaveConfig.enabled ? "ON" : "OFF"
                            )
                    );

                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde automatique activée"
                            : "Sauvegarde automatique désactivée";
                }
        ).bounds(
                layout.controlX,
                layout.toggleY,
                layout.controlWidth,
                layout.buttonHeight
        ).build();

        addRenderableWidget(toggleButton);

        intervalButton = Button.builder(
                Component.literal(getCurrentIntervalName()),
                button -> toggleIntervalMenu()
        ).bounds(
                layout.controlX,
                layout.intervalY,
                layout.controlWidth,
                layout.buttonHeight
        ).build();

        addRenderableWidget(intervalButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("SELECT FOLDER"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        layout.selectFolderX,
                        layout.folderButtonY,
                        layout.halfButtonWidth,
                        layout.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE BACKUP"),
                        button -> openBackupScreen()
                ).bounds(
                        layout.restoreX,
                        layout.folderButtonY,
                        layout.halfButtonWidth,
                        layout.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("SAVE"),
                        button -> {
                            AutoSaveConfig.save();
                            status = "Configuration enregistrée";
                        }
                ).bounds(
                        layout.saveX,
                        layout.footerY,
                        layout.saveWidth,
                        layout.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("CLOSE"),
                        button -> onClose()
                ).bounds(
                        layout.closeX,
                        layout.footerY,
                        layout.closeWidth,
                        layout.buttonHeight
                ).build()
        );

        if (intervalMenuOpen) {
            for (int i = 0; i < INTERVALS.length; i++) {
                final int index = i;

                addRenderableWidget(
                        Button.builder(
                                Component.literal(INTERVALS[i].name),
                                button -> selectInterval(INTERVALS[index])
                        ).bounds(
                                layout.menuX,
                                layout.menuY + i * layout.menuOptionHeight,
                                layout.menuWidth,
                                layout.menuOptionHeight
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

        refreshWidgets();
    }

    private void selectInterval(IntervalOption option) {
        AutoSaveConfig.intervalTicks = option.ticks;
        AutoSaveConfig.save();

        intervalMenuOpen = false;
        status = "Intervalle : " + option.name;

        refreshWidgets();
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

    private void openBackupScreen() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new BackupSelectScreen(this)
            );
        }
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
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
        Layout layout = calculateLayout();

        // Jeu visible derrière.
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x7A000000
        );

        // Ombre.
        graphics.fill(
                layout.left + 7,
                layout.top + 7,
                layout.right + 7,
                layout.bottom + 7,
                0x65000000
        );

        // Fenêtre.
        graphics.fill(
                layout.left,
                layout.top,
                layout.right,
                layout.bottom,
                0xFF090909
        );

        // Bordure.
        graphics.fill(
                layout.left,
                layout.top,
                layout.right,
                layout.top + 1,
                0xFF2C2C2C
        );

        graphics.fill(
                layout.left,
                layout.bottom - 1,
                layout.right,
                layout.bottom,
                0xFF2C2C2C
        );

        graphics.fill(
                layout.left,
                layout.top,
                layout.left + 1,
                layout.bottom,
                0xFF2C2C2C
        );

        graphics.fill(
                layout.right - 1,
                layout.top,
                layout.right,
                layout.bottom,
                0xFF2C2C2C
        );

        // Header.
        graphics.text(
                this.font,
                Component.literal("AS"),
                layout.left + layout.padding,
                layout.top + 24,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                layout.left + layout.padding + 30,
                layout.top + 18,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic World Backup"),
                layout.left + layout.padding + 30,
                layout.top + 37,
                0xFF666666,
                false
        );

        int dotX = layout.right - layout.padding - 68;

        graphics.fill(
                dotX,
                layout.top + 29,
                dotX + 6,
                layout.top + 35,
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
                dotX + 11,
                layout.top + 25,
                0xFF777777,
                false
        );

        graphics.fill(
                layout.left + layout.padding,
                layout.headerBottom,
                layout.right - layout.padding,
                layout.headerBottom + 1,
                0xFF202020
        );

        // Automatic backup.
        graphics.text(
                this.font,
                Component.literal("AUTOMATIC BACKUP"),
                layout.left + layout.padding,
                layout.autoTitleY,
                0xFF666666,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic backup"),
                layout.left + layout.padding,
                layout.autoNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Create a backup automatically while playing."
                ),
                layout.left + layout.padding,
                layout.autoDescriptionY,
                0xFF666666,
                false
        );

        // Intervalle.
        graphics.text(
                this.font,
                Component.literal("Backup interval"),
                layout.left + layout.padding,
                layout.intervalNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Choose how often Auto Save creates a backup."
                ),
                layout.left + layout.padding,
                layout.intervalDescriptionY,
                0xFF666666,
                false
        );

        // Location.
        graphics.text(
                this.font,
                Component.literal("BACKUP LOCATION"),
                layout.left + layout.padding,
                layout.locationTitleY,
                0xFF666666,
                true
        );

        String folderText =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "No folder selected"
                        : AutoSaveConfig.folder;

        if (folderText.length() > layout.maxPathLength) {
            folderText = "..." + folderText.substring(
                    folderText.length() - layout.maxPathLength + 3
            );
        }

        graphics.fill(
                layout.pathX,
                layout.pathY,
                layout.pathRight,
                layout.pathBottom,
                0xFF050505
        );

        graphics.fill(
                layout.pathX,
                layout.pathY,
                layout.pathRight,
                layout.pathY + 1,
                0xFF222222
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                layout.pathTextX,
                layout.pathTextY,
                0xFF888888,
                false
        );

        // Sauvegardes.
        graphics.text(
                this.font,
                Component.literal("SAVED BACKUPS"),
                layout.left + layout.padding,
                layout.backupsTitleY,
                0xFF666666,
                true
        );

        int backupCount = AutoSaveService.listBackups().length;

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
                layout.left + layout.padding,
                layout.backupsCountY,
                0xFF666666,
                false
        );

        // Statut.
        graphics.text(
                this.font,
                Component.literal(status),
                layout.left + layout.padding,
                layout.statusY,
                0xFF555555,
                false
        );

        // Menu intervalle.
        if (intervalMenuOpen) {
            int menuHeight =
                    layout.menuOptionHeight * INTERVALS.length;

            graphics.fill(
                    layout.menuX - 2,
                    layout.menuY - 2,
                    layout.menuX + layout.menuWidth + 2,
                    layout.menuY + menuHeight + 2,
                    0xFF303030
            );

            graphics.fill(
                    layout.menuX,
                    layout.menuY,
                    layout.menuX + layout.menuWidth,
                    layout.menuY + menuHeight,
                    0xFF050505
            );
        }

        // Widgets par-dessus le fond.
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private Layout calculateLayout() {
        int availableWidth = this.width;
        int availableHeight = this.height;

        int windowWidth = clamp(
                (int) (availableWidth * 0.78),
                430,
                620
        );

        int windowHeight = clamp(
                (int) (availableHeight * 0.82),
                330,
                470
        );

        windowWidth = Math.min(
                windowWidth,
                availableWidth - 20
        );

        windowHeight = Math.min(
                windowHeight,
                availableHeight - 20
        );

        int left =
                (availableWidth - windowWidth) / 2;

        int top =
                (availableHeight - windowHeight) / 2;

        int padding = Math.max(18, windowWidth / 20);

        int buttonHeight = clamp(
                windowHeight / 14,
                22,
                28
        );

        int controlWidth = clamp(
                windowWidth / 5,
                90,
                125
        );

        int halfButtonWidth =
                (windowWidth - padding * 2 - 10) / 2;

        int headerBottom =
                top + Math.max(
                        58,
                        windowHeight / 6
                );

        int toggleY =
                top + (int) (windowHeight * 0.20);

        int intervalY =
                toggleY + 56;

        int autoTitleY =
                top + (int) (windowHeight * 0.23);

        int autoNameY =
                autoTitleY + 22;

        int autoDescriptionY =
                autoNameY + 18;

        int intervalNameY =
                autoDescriptionY + 35;

        int intervalDescriptionY =
                intervalNameY + 18;

        int locationTitleY =
                intervalDescriptionY + 39;

        int pathY =
                locationTitleY + 14;

        int pathHeight = 36;

        int folderButtonY =
                pathY + pathHeight + 10;

        int backupsTitleY =
                folderButtonY + buttonHeight + 25;

        int backupsCountY =
                backupsTitleY + 19;

        int footerY =
                top + windowHeight - buttonHeight - 18;

        int statusY =
                footerY - 22;

        int saveWidth =
                clamp(
                        windowWidth / 8,
                        75,
                        90
                );

        int closeWidth = saveWidth;

        int closeX =
                left + windowWidth - padding - closeWidth;

        int saveX =
                closeX - 8 - saveWidth;

        int controlX =
                left + windowWidth
                        - padding
                        - controlWidth;

        int selectFolderX =
                left + padding;

        int restoreX =
                selectFolderX
                        + halfButtonWidth
                        + 10;

        int menuWidth =
                controlWidth + 20;

        int menuX =
                controlX - 20;

        int menuY =
                intervalY + buttonHeight + 8;

        int menuOptionHeight = 25;

        int maxPathLength =
                clamp(
                        windowWidth / 8,
                        35,
                        68
                );

        return new Layout(
                left,
                top,
                left + windowWidth,
                top + windowHeight,
                padding,
                buttonHeight,
                controlX,
                controlWidth,
                toggleY,
                intervalY,
                headerBottom,
                autoTitleY,
                autoNameY,
                autoDescriptionY,
                intervalNameY,
                intervalDescriptionY,
                locationTitleY,
                pathY,
                pathY + pathHeight,
                left + padding,
                pathY + 10,
                left + windowWidth - padding,
                selectFolderX,
                restoreX,
                halfButtonWidth,
                folderButtonY,
                backupsTitleY,
                backupsCountY,
                statusY,
                saveX,
                closeX,
                saveWidth,
                closeWidth,
                footerY,
                menuX,
                menuY,
                menuWidth,
                menuOptionHeight,
                maxPathLength
        );
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {
        return Math.max(
                min,
                Math.min(max, value)
        );
    }

    private record IntervalOption(
            String name,
            int ticks
    ) {
    }

    private record Layout(
            int left,
            int top,
            int right,
            int bottom,
            int padding,
            int buttonHeight,
            int controlX,
            int controlWidth,
            int toggleY,
            int intervalY,
            int headerBottom,
            int autoTitleY,
            int autoNameY,
            int autoDescriptionY,
            int intervalNameY,
            int intervalDescriptionY,
            int locationTitleY,
            int pathY,
            int pathBottom,
            int pathX,
            int pathTextY,
            int pathRight,
            int selectFolderX,
            int restoreX,
            int halfButtonWidth,
            int folderButtonY,
            int backupsTitleY,
            int backupsCountY,
            int statusY,
            int saveX,
            int closeX,
            int saveWidth,
            int closeWidth,
            int footerY,
            int menuX,
            int menuY,
            int menuWidth,
            int menuOptionHeight,
            int maxPathLength
    ) {
        int pathTextX() {
            return pathX + 12;
        }
    }
}
