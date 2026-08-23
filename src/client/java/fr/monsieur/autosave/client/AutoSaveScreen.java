package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 500;

    private final Screen parent;

    private Button toggleButton;
    private Button intervalButton;

    private String status = "Ready";

    private boolean intervalMenuOpen = false;

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

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        /*
         * AUTOMATIC BACKUP - ON/OFF
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
                            ? "Automatic backup enabled"
                            : "Automatic backup disabled";
                }
        ).bounds(
                left + PANEL_WIDTH - 125,
                top + 106,
                90,
                30
        ).build();

        addRenderableWidget(toggleButton);

        /*
         * INTERVAL
         */
        intervalButton = Button.builder(
                Component.literal(
                        getCurrentIntervalName()
                ),
                button -> toggleIntervalMenu()
        ).bounds(
                left + PANEL_WIDTH - 155,
                top + 177,
                120,
                30
        ).build();

        addRenderableWidget(intervalButton);

        /*
         * SELECT FOLDER
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("SELECT FOLDER"),
                        button ->
                                AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + PANEL_WIDTH - 185,
                        top + 300,
                        150,
                        30
                ).build()
        );

        /*
         * RESTORE BACKUP
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE BACKUP"),
                        button -> openBackupScreen()
                ).bounds(
                        left + PANEL_WIDTH - 185,
                        top + 382,
                        150,
                        30
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
                            status = "Configuration saved";
                        }
                ).bounds(
                        left + PANEL_WIDTH - 215,
                        top + PANEL_HEIGHT - 48,
                        90,
                        28
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
                        left + PANEL_WIDTH - 115,
                        top + PANEL_HEIGHT - 48,
                        80,
                        28
                ).build()
        );

        /*
         * INTERVAL MENU
         */
        if (intervalMenuOpen) {

            int menuX = left + PANEL_WIDTH - 275;
            int menuY = top + 215;
            int menuWidth = 140;
            int optionHeight = 30;

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
                                menuY + i * optionHeight,
                                menuWidth,
                                optionHeight
                        ).build()
                );
            }
        }
    }

    private void toggleIntervalMenu() {
        intervalMenuOpen = !intervalMenuOpen;

        status = intervalMenuOpen
                ? "Choose an interval"
                : "Interval: " + getCurrentIntervalName();

        refreshWidgets();
    }

    private void selectInterval(
            IntervalOption option
    ) {
        AutoSaveConfig.intervalTicks = option.ticks;
        AutoSaveConfig.save();

        intervalMenuOpen = false;

        status = "Interval: " + option.name;

        refreshWidgets();
    }

    private String getCurrentIntervalName() {

        int ticks =
                AutoSaveConfig.intervalTicks;

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

        status = "Folder selected";
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

        int left =
                (this.width - PANEL_WIDTH) / 2;

        int top =
                (this.height - PANEL_HEIGHT) / 2;

        /*
         * BACKGROUND
         */

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF000000
        );

        /*
         * SOFT PANEL SHADOW
         */

        graphics.fill(
                left + 8,
                top + 8,
                left + PANEL_WIDTH + 8,
                top + PANEL_HEIGHT + 8,
                0x65000000
        );

        /*
         * MAIN PANEL
         */

        graphics.fill(
                left,
                top,
                left + PANEL_WIDTH,
                top + PANEL_HEIGHT,
                0xFF090909
        );

        /*
         * HEADER LINE
         */

        graphics.fill(
                left,
                top + 81,
                left + PANEL_WIDTH,
                top + 82,
                0xFF202020
        );

        /*
         * BRAND
         */

        graphics.fill(
                left + 28,
                top + 25,
                left + 62,
                top + 59,
                0xFFFFFFFF
        );

        graphics.text(
                this.font,
                Component.literal("AS"),
                left + 36,
                top + 39,
                0xFF000000,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 76,
                top + 29,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Automatic World Backup"
                ),
                left + 76,
                top + 48,
                0xFF666666,
                false
        );

        /*
         * GLOBAL STATUS
         */

        graphics.fill(
                left + PANEL_WIDTH - 86,
                top + 39,
                left + PANEL_WIDTH - 79,
                top + 46,
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
                left + PANEL_WIDTH - 72,
                top + 37,
                0xFFAAAAAA,
                false
        );

        /*
         * SECTION TITLE
         */

        graphics.text(
                this.font,
                Component.literal(
                        "AUTOMATIC BACKUP"
                ),
                left + 32,
                top + 105,
                0xFF666666,
                true
        );

        /*
         * AUTO BACKUP
         */

        graphics.text(
                this.font,
                Component.literal(
                        "Automatic backup"
                ),
                left + 32,
                top + 128,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Create a backup automatically while playing."
                ),
                left + 32,
                top + 147,
                0xFF666666,
                false
        );

        /*
         * INTERVAL
         */

        graphics.text(
                this.font,
                Component.literal(
                        "Backup interval"
                ),
                left + 32,
                top + 199,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Choose how often Auto Save creates a backup."
                ),
                left + 32,
                top + 218,
                0xFF666666,
                false
        );

        /*
         * LOCATION
         */

        graphics.text(
                this.font,
                Component.literal(
                        "BACKUP LOCATION"
                ),
                left + 32,
                top + 270,
                0xFF666666,
                true
        );

        String folderText =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "No folder selected"
                        : AutoSaveConfig.folder;

        if (folderText.length() > 82) {

            folderText =
                    "..." + folderText.substring(
                            folderText.length() - 79
                    );
        }

        /*
         * PATH BOX
         */

        graphics.fill(
                left + 32,
                top + 282,
                left + PANEL_WIDTH - 32,
                top + 328,
                0xFF050505
        );

        graphics.fill(
                left + 32,
                top + 282,
                left + PANEL_WIDTH - 32,
                top + 283,
                0xFF222222
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                left + 46,
                top + 302,
                0xFF888888,
                false
        );

        /*
         * SAVED BACKUPS
         */

        graphics.text(
                this.font,
                Component.literal(
                        "SAVED BACKUPS"
                ),
                left + 32,
                top + 358,
                0xFF666666,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        getBackupCountText()
                ),
                left + 32,
                top + 378,
                0xFF666666,
                false
        );

        /*
         * STATUS
         */

        graphics.text(
                this.font,
                Component.literal(status),
                left + 32,
                top + PANEL_HEIGHT - 38,
                0xFF555555,
                false
        );

        /*
         * INTERVAL MENU BACKGROUND
         */

        if (intervalMenuOpen) {

            int menuX =
                    left + PANEL_WIDTH - 305;

            int menuY =
                    top + 215;

            int menuWidth =
                    140;

            int menuHeight =
                    INTERVALS.length * 30;

            graphics.fill(
                    menuX - 1,
                    menuY - 1,
                    menuX + menuWidth + 1,
                    menuY + menuHeight + 1,
                    0xFF333333
            );

            graphics.fill(
                    menuX,
                    menuY,
                    menuX + menuWidth,
                    menuY + menuHeight,
                    0xFF080808
            );
        }

        /*
         * WIDGETS LAST
         */

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private String getBackupCountText() {

        int count =
                AutoSaveService.listBackups().length;

        return count + (
                count == 1
                        ? " backup available"
                        : " backups available"
        );
    }

    private record IntervalOption(
            String name,
            int ticks
    ) {
    }
}
