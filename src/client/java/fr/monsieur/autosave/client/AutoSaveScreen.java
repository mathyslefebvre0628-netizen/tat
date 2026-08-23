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
    private int intervalScroll = 0;

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
                                    AutoSaveConfig.enabled
                                            ? "ON"
                                            : "OFF"
                            )
                    );

                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde activée"
                            : "Sauvegarde désactivée";
                }
        ).bounds(
                layout.controlX,
                layout.toggleY,
                layout.controlWidth,
                layout.buttonHeight
        ).build();

        addRenderableWidget(toggleButton);

        intervalButton = Button.builder(
                Component.literal(
                        getCurrentIntervalName()
                ),
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
                        Component.literal("SELECT"),
                        button ->
                                AutoSaveService.chooseFolder(this)
                ).bounds(
                        layout.selectFolderX,
                        layout.folderButtonY,
                        layout.smallButtonWidth,
                        layout.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE"),
                        button -> openBackupScreen()
                ).bounds(
                        layout.restoreX,
                        layout.folderButtonY,
                        layout.smallButtonWidth,
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
                        layout.footerButtonWidth,
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
                        layout.footerButtonWidth,
                        layout.buttonHeight
                ).build()
        );

        if (intervalMenuOpen) {
            int visible = layout.visibleIntervalCount;

            for (int i = 0; i < visible; i++) {
                int actualIndex = intervalScroll + i;

                if (actualIndex >= INTERVALS.length) {
                    break;
                }

                final int index = actualIndex;

                addRenderableWidget(
                        Button.builder(
                                Component.literal(
                                        INTERVALS[index].name
                                ),
                                button ->
                                        selectInterval(
                                                INTERVALS[index]
                                        )
                        ).bounds(
                                layout.menuX,
                                layout.menuY
                                        + i * layout.menuOptionHeight,
                                layout.menuWidth,
                                layout.menuOptionHeight
                        ).build()
                );
            }
        }
    }

    private void toggleIntervalMenu() {
        intervalMenuOpen = !intervalMenuOpen;

        if (!intervalMenuOpen) {
            intervalScroll = 0;
        }

        status = intervalMenuOpen
                ? "Choisis un intervalle"
                : "Intervalle : "
                + getCurrentIntervalName();

        refreshWidgets();
    }

    private void selectInterval(
            IntervalOption option
    ) {
        AutoSaveConfig.intervalTicks =
                option.ticks;

        AutoSaveConfig.save();

        intervalMenuOpen = false;
        intervalScroll = 0;

        status =
                "Intervalle : "
                        + option.name;

        refreshWidgets();
    }

    /*
     * Molette pour faire défiler le menu.
     *
     * Minecraft 26.2 utilise les quatre paramètres
     * de mouseScrolled.
     */
    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (!intervalMenuOpen) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount
            );
        }

        Layout layout = calculateLayout();

        boolean insideMenu =
                mouseX >= layout.menuX
                        && mouseX <= layout.menuX
                        + layout.menuWidth
                        && mouseY >= layout.menuY
                        && mouseY <= layout.menuY
                        + layout.menuHeight;

        if (!insideMenu) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount
            );
        }

        int maxScroll =
                Math.max(
                        0,
                        INTERVALS.length
                                - layout.visibleIntervalCount
                );

        if (verticalAmount < 0) {
            intervalScroll =
                    Math.min(
                            maxScroll,
                            intervalScroll + 1
                    );
        } else if (verticalAmount > 0) {
            intervalScroll =
                    Math.max(
                            0,
                            intervalScroll - 1
                    );
        }

        refreshWidgets();
        return true;
    }

    private String getCurrentIntervalName() {
        int ticks =
                AutoSaveConfig.intervalTicks;

        for (IntervalOption option :
                INTERVALS) {

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
        AutoSaveConfig.folder =
                path;

        AutoSaveConfig.save();

        status =
                "Dossier sélectionné";
    }

    @Override
    public void onClose() {
        AutoSaveConfig.save();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    parent
            );
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        Layout layout =
                calculateLayout();

        /*
         * Minecraft visible derrière.
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x76000000
        );

        /*
         * Ombre.
         */
        graphics.fill(
                layout.left + 5,
                layout.top + 5,
                layout.right + 5,
                layout.bottom + 5,
                0x55000000
        );

        /*
         * Fenêtre compacte.
         */
        graphics.fill(
                layout.left,
                layout.top,
                layout.right,
                layout.bottom,
                0xFF090909
        );

        /*
         * Bordure.
         */
        graphics.fill(
                layout.left,
                layout.top,
                layout.right,
                layout.top + 1,
                0xFF242424
        );

        graphics.fill(
                layout.left,
                layout.bottom - 1,
                layout.right,
                layout.bottom,
                0xFF242424
        );

        graphics.fill(
                layout.left,
                layout.top,
                layout.left + 1,
                layout.bottom,
                0xFF242424
        );

        graphics.fill(
                layout.right - 1,
                layout.top,
                layout.right,
                layout.bottom,
                0xFF242424
        );

        /*
         * Header.
         */
        graphics.text(
                this.font,
                Component.literal("AS"),
                layout.left + layout.padding,
                layout.top + 16,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                layout.left + layout.padding + 24,
                layout.top + 11,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Automatic World Backup"
                ),
                layout.left + layout.padding + 24,
                layout.top + 27,
                0xFF666666,
                false
        );

        /*
         * Status.
         */
        int dotX =
                layout.right
                        - layout.padding
                        - 55;

        graphics.fill(
                dotX,
                layout.top + 19,
                dotX + 5,
                layout.top + 24,
                AutoSaveConfig.enabled
                        ? 0xFFFFFFFF
                        : 0xFF444444
        );

        graphics.text(
                this.font,
                Component.literal(
                        AutoSaveConfig.enabled
                                ? "ON"
                                : "OFF"
                ),
                dotX + 10,
                layout.top + 15,
                0xFF777777,
                false
        );

        /*
         * Séparateur.
         */
        graphics.fill(
                layout.left
                        + layout.padding,
                layout.headerBottom,
                layout.right
                        - layout.padding,
                layout.headerBottom + 1,
                0xFF202020
        );

        /*
         * AUTOMATIC BACKUP.
         */
        graphics.text(
                this.font,
                Component.literal(
                        "AUTOMATIC BACKUP"
                ),
                layout.left
                        + layout.padding,
                layout.autoTitleY,
                0xFF666666,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Automatic backup"
                ),
                layout.left
                        + layout.padding,
                layout.autoNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Backup while playing."
                ),
                layout.left
                        + layout.padding,
                layout.autoDescriptionY,
                0xFF666666,
                false
        );

        /*
         * INTERVALLE.
         */
        graphics.text(
                this.font,
                Component.literal(
                        "Backup interval"
                ),
                layout.left
                        + layout.padding,
                layout.intervalNameY,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Backup frequency"
                ),
                layout.left
                        + layout.padding,
                layout.intervalDescriptionY,
                0xFF666666,
                false
        );

        /*
         * LOCATION.
         */
        graphics.text(
                this.font,
                Component.literal(
                        "BACKUP LOCATION"
                ),
                layout.left
                        + layout.padding,
                layout.locationTitleY,
                0xFF666666,
                true
        );

        String folder =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "No folder selected"
                        : AutoSaveConfig.folder;

        if (folder.length()
                > layout.maxPathLength) {

            folder =
                    "..."
                            + folder.substring(
                            folder.length()
                                    - layout.maxPathLength
                                    + 3
                    );
        }

        graphics.fill(
                layout.pathX,
                layout.pathY,
                layout.pathRight,
                layout.pathBottom,
                0xFF050505
        );

        graphics.text(
                this.font,
                Component.literal(folder),
                layout.pathTextX,
                layout.pathTextY,
                0xFF888888,
                false
        );

        /*
         * BACKUPS.
         */
        graphics.text(
                this.font,
                Component.literal(
                        "SAVED BACKUPS"
                ),
                layout.left
                        + layout.padding,
                layout.backupsTitleY,
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
                                        ? " backup"
                                        : " backups"
                        )
                ),
                layout.left
                        + layout.padding,
                layout.backupsCountY,
                0xFF666666,
                false
        );

        graphics.text(
                this.font,
                Component.literal(status),
                layout.left
                        + layout.padding,
                layout.statusY,
                0xFF555555,
                false
        );

        /*
         * Fond du menu.
         */
        if (intervalMenuOpen) {
            graphics.fill(
                    layout.menuX - 1,
                    layout.menuY - 1,
                    layout.menuX
                            + layout.menuWidth + 1,
                    layout.menuY
                            + layout.menuHeight + 1,
                    0xFF333333
            );

            graphics.fill(
                    layout.menuX,
                    layout.menuY,
                    layout.menuX
                            + layout.menuWidth,
                    layout.menuY
                            + layout.menuHeight,
                    0xFF050505
            );

            /*
             * Indication visuelle du scroll.
             */
            if (INTERVALS.length
                    > layout.visibleIntervalCount) {

                int trackX =
                        layout.menuX
                                + layout.menuWidth
                                - 5;

                graphics.fill(
                        trackX,
                        layout.menuY,
                        trackX + 2,
                        layout.menuY
                                + layout.menuHeight,
                        0xFF222222
                );

                int thumbHeight =
                        Math.max(
                                10,
                                layout.menuHeight
                                        * layout.visibleIntervalCount
                                        / INTERVALS.length
                        );

                int maxScroll =
                        INTERVALS.length
                                - layout.visibleIntervalCount;

                int thumbOffset =
                        maxScroll == 0
                                ? 0
                                : (layout.menuHeight
                                - thumbHeight)
                                * intervalScroll
                                / maxScroll;

                graphics.fill(
                        trackX,
                        layout.menuY
                                + thumbOffset,
                        trackX + 2,
                        layout.menuY
                                + thumbOffset
                                + thumbHeight,
                        0xFFFFFFFF
                );
            }
        }

        /*
         * Widgets au-dessus du rendu.
         */
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private Layout calculateLayout() {

        /*
         * GUI SCALE 4 :
         * on travaille uniquement avec l'espace
         * logique réel de Minecraft.
         */
        int w = this.width;
        int h = this.height;

        int windowWidth =
                clamp(
                        (int) (w * 0.84),
                        300,
                        520
                );

        int windowHeight =
                clamp(
                        (int) (h * 0.86),
                        260,
                        390
                );

        windowWidth =
                Math.min(
                        windowWidth,
                        w - 12
                );

        windowHeight =
                Math.min(
                        windowHeight,
                        h - 12
                );

        int left =
                (w - windowWidth) / 2;

        int top =
                (h - windowHeight) / 2;

        int padding =
                clamp(
                        windowWidth / 14,
                        12,
                        24
                );

        int buttonHeight =
                clamp(
                        windowHeight / 12,
                        20,
                        26
                );

        int controlWidth =
                clamp(
                        windowWidth / 5,
                        70,
                        105
                );

        int smallButtonWidth =
                (windowWidth
                        - padding * 2
                        - 8) / 2;

        int headerBottom =
                top
                        + clamp(
                        windowHeight / 5,
                        45,
                        65
                );

        int toggleY =
                top + 62;

        int intervalY =
                top + 111;

        int autoTitleY =
                top + 72;

        int autoNameY =
                autoTitleY + 16;

        int autoDescriptionY =
                autoNameY + 14;

        int intervalNameY =
                autoDescriptionY + 28;

        int intervalDescriptionY =
                intervalNameY + 15;

        int locationTitleY =
                intervalDescriptionY + 31;

        int pathY =
                locationTitleY + 10;

        int pathHeight =
                27;

        int folderButtonY =
                pathY + pathHeight + 8;

        int backupsTitleY =
                folderButtonY
                        + buttonHeight
                        + 17;

        int backupsCountY =
                backupsTitleY + 15;

        int footerY =
                top
                        + windowHeight
                        - buttonHeight
                        - 11;

        int statusY =
                footerY - 18;

        int footerButtonWidth =
                clamp(
                        windowWidth / 6,
                        62,
                        82
                );

        int closeX =
                left
                        + windowWidth
                        - padding
                        - footerButtonWidth;

        int saveX =
                closeX
                        - 7
                        - footerButtonWidth;

        int controlX =
                left
                        + windowWidth
                        - padding
                        - controlWidth;

        int selectFolderX =
                left + padding;

        int restoreX =
                selectFolderX
                        + smallButtonWidth
                        + 8;

        int menuWidth =
                controlWidth + 12;

        int visibleIntervalCount =
                h < 190
                        ? 3
                        : 4;

        int menuOptionHeight = 22;

        int menuX =
                controlX
                        - 12;

        int menuY =
                intervalY
                        + buttonHeight
                        + 5;

        int menuHeight =
                visibleIntervalCount
                        * menuOptionHeight;

        int pathX =
                left + padding;

        int pathRight =
                left
                        + windowWidth
                        - padding;

        int pathTextX =
                pathX + 8;

        int pathTextY =
                pathY + 7;

        int maxPathLength =
                clamp(
                        windowWidth / 8,
                        28,
                        55
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
                pathX,
                pathRight,
                pathTextX,
                pathTextY,
                selectFolderX,
                restoreX,
                smallButtonWidth,
                folderButtonY,
                backupsTitleY,
                backupsCountY,
                statusY,
                saveX,
                closeX,
                footerButtonWidth,
                footerY,
                menuX,
                menuY,
                menuWidth,
                menuOptionHeight,
                menuHeight,
                visibleIntervalCount,
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
                Math.min(
                        max,
                        value
                )
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
            int smallButtonWidth,
            int folderButtonY,
            int backupsTitleY,
            int backupsCountY,
            int statusY,
            int saveX,
            int closeX,
            int footerButtonWidth,
            int footerY,
            int menuX,
            int menuY,
            int menuWidth,
            int menuOptionHeight,
            int menuHeight,
            int visibleIntervalCount,
            int maxPathLength
    ) {
    }
}
