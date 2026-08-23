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

        Layout l = calculateLayout();

        toggleButton = Button.builder(
                Component.literal(AutoSaveConfig.enabled ? "ON" : "OFF"),
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
                l.controlX,
                l.toggleY,
                l.controlWidth,
                l.buttonHeight
        ).build();

        addRenderableWidget(toggleButton);

        intervalButton = Button.builder(
                Component.literal(getCurrentIntervalName()),
                button -> toggleIntervalMenu()
        ).bounds(
                l.controlX,
                l.intervalY,
                l.controlWidth,
                l.buttonHeight
        ).build();

        addRenderableWidget(intervalButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("SELECT FOLDER"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        l.selectFolderX,
                        l.folderButtonY,
                        l.halfButtonWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE BACKUP"),
                        button -> openBackupScreen()
                ).bounds(
                        l.restoreX,
                        l.folderButtonY,
                        l.halfButtonWidth,
                        l.buttonHeight
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
                        l.saveX,
                        l.footerY,
                        l.saveWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("CLOSE"),
                        button -> onClose()
                ).bounds(
                        l.closeX,
                        l.footerY,
                        l.closeWidth,
                        l.buttonHeight
                ).build()
        );

        if (intervalMenuOpen) {
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
                                l.menuX,
                                l.menuY + i * l.menuOptionHeight,
                                l.menuWidth,
                                l.menuOptionHeight
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
        Layout l = calculateLayout();

        // Minecraft reste visible derrière
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x7A000000
        );

        // Ombre
        graphics.fill(
                l.left + 7,
                l.top + 7,
                l.right + 7,
                l.bottom + 7,
                0x65000000
        );

        // Fenêtre
        graphics.fill(
                l.left,
                l.top,
                l.right,
                l.bottom,
                0xFF090909
        );

        // Bordure
        graphics.fill(
                l.left,
                l.top,
                l.right,
                l.top + 1,
                0xFF2C2C2C
        );

        graphics.fill(
                l.left,
                l.bottom - 1,
                l.right,
                l.bottom,
                0xFF2C2C2C
        );

        graphics.fill(
                l.left,
                l.top,
                l.left + 1,
                l.bottom,
                0xFF2C2C2C
        );

        graphics.fill(
                l.right - 1,
                l.top,
                l.right,
                l.bottom,
                0xFF2C2C2C
        );

        // Header
        graphics.text(
                this.font,
                Component.literal("AS"),
                l.left + l.padding,
                l.top + 24,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                l.left + l.padding + 30,
                l.top + 18,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic World Backup"),
                l.left + l.padding + 30,
                l.top + 37,
                0xFF666666,
                false
        );

        int dotX = l.right - l.padding - 68;

        graphics.fill(
                dotX,
                l.top + 29,
                dotX + 6,
                l.top + 35,
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
                l.top + 25,
                0xFF777777,
                false
        );

        graphics.fill(
                l.left + l.padding,
                l.headerBottom,
                l.right - l.padding,
                l.headerBottom + 1,
                0xFF202020
        );

        // Automatic Backup
        graphics.text(
                this.font,
                Component.literal("AUTOMATIC BACKUP"),
                l.left + l.padding,
                l.autoTitleY,
                0xFF666666,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic backup"),
                l.left + l.padding,
                l.autoNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Create a backup automatically while playing."
                ),
                l.left + l.padding,
                l.autoDescriptionY,
                0xFF666666,
                false
        );

        // Interval
        graphics.text(
                this.font,
                Component.literal("Backup interval"),
                l.left + l.padding,
                l.intervalNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Choose how often Auto Save creates a backup."
                ),
                l.left + l.padding,
                l.intervalDescriptionY,
                0xFF666666,
                false
        );

        // Backup Location
        graphics.text(
                this.font,
                Component.literal("BACKUP LOCATION"),
                l.left + l.padding,
                l.locationTitleY,
                0xFF666666,
                true
        );

        String folderText =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "No folder selected"
                        : AutoSaveConfig.folder;

        if (folderText.length() > l.maxPathLength) {
            folderText = "..." + folderText.substring(
                    folderText.length() - l.maxPathLength + 3
            );
        }

        graphics.fill(
                l.pathX,
                l.pathY,
                l.pathRight,
                l.pathBottom,
                0xFF050505
        );

        graphics.fill(
                l.pathX,
                l.pathY,
                l.pathRight,
                l.pathY + 1,
                0xFF222222
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                l.pathTextX,
                l.pathTextY,
                0xFF888888,
                false
        );

        // Backups
        graphics.text(
                this.font,
                Component.literal("SAVED BACKUPS"),
                l.left + l.padding,
                l.backupsTitleY,
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
                l.left + l.padding,
                l.backupsCountY,
                0xFF666666,
                false
        );

        // Status
        graphics.text(
                this.font,
                Component.literal(status),
                l.left + l.padding,
                l.statusY,
                0xFF555555,
                false
        );

        // Menu intervalle
        if (intervalMenuOpen) {
            int menuHeight =
                    l.menuOptionHeight * INTERVALS.length;

            graphics.fill(
                    l.menuX - 2,
                    l.menuY - 2,
                    l.menuX + l.menuWidth + 2,
                    l.menuY + menuHeight + 2,
                    0xFF303030
            );

            graphics.fill(
                    l.menuX,
                    l.menuY,
                    l.menuX + l.menuWidth,
                    l.menuY + menuHeight,
                    0xFF050505
            );
        }

        // Widgets au-dessus du rendu personnalisé
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

        int padding =
                Math.max(18, windowWidth / 20);

        int buttonHeight =
                clamp(
                        windowHeight / 14,
                        22,
                        28
                );

        int controlWidth =
                clamp(
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
                top + windowHeight
                        - buttonHeight
                        - 18;

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
                left + windowWidth
                        - padding
                        - closeWidth;

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

        int pathX = left + padding;
        int pathRight = left + windowWidth - padding;
        int pathTextX = pathX + 12;
        int pathTextY = pathY + 10;

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
                pathX,
                pathRight,
                pathTextX,
                pathTextY,
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
            int pathRight,
            int pathTextX,
            int pathTextY,
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
    }
}
