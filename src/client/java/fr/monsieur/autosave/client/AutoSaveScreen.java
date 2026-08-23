package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 440;

    private final Screen parent;

    private Button toggleButton;
    private Button intervalButton;

    private String status = "Prêt";
    private boolean intervalMenuOpen = false;

    private static final IntervalOption[] INTERVALS = {
            new IntervalOption("1 min", 20 * 60),
            new IntervalOption("5 min", 20 * 60 * 5),
            new IntervalOption("10 min", 20 * 60 * 10),
            new IntervalOption("15 min", 20 * 60 * 15),
            new IntervalOption("30 min", 20 * 60 * 30),
            new IntervalOption("1 h", 20 * 60 * 60)
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

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

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
                left + panelWidth - 190,
                top + 91,
                140,
                24
        ).build();

        addRenderableWidget(toggleButton);

        intervalButton = Button.builder(
                Component.literal(getCurrentIntervalName()),
                button -> toggleIntervalMenu()
        ).bounds(
                left + panelWidth - 190,
                top + 147,
                140,
                24
        ).build();

        addRenderableWidget(intervalButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Choisir un dossier"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + 45,
                        top + 205,
                        320,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Utiliser une sauvegarde"),
                        button -> openBackupScreen()
                ).bounds(
                        left + 395,
                        top + 205,
                        320,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Enregistrer"),
                        button -> {
                            AutoSaveConfig.save();
                            status = "Configuration enregistrée";
                        }
                ).bounds(
                        left + 45,
                        top + panelHeight - 55,
                        320,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Fermer"),
                        button -> onClose()
                ).bounds(
                        left + 395,
                        top + panelHeight - 55,
                        320,
                        24
                ).build()
        );

        if (intervalMenuOpen) {
            int menuX = left + panelWidth - 310;
            int menuY = top + 177;
            int menuWidth = 260;
            int optionHeight = 26;

            for (int i = 0; i < INTERVALS.length; i++) {
                IntervalOption option = INTERVALS[i];
                final int index = i;

                addRenderableWidget(
                        Button.builder(
                                Component.literal(option.name),
                                button -> selectInterval(INTERVALS[index])
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

    private int getPanelWidth() {
        return Math.min(
                PANEL_WIDTH,
                Math.max(560, this.width - 50)
        );
    }

    private int getPanelHeight() {
        return Math.min(
                PANEL_HEIGHT,
                Math.max(350, this.height - 30)
        );
    }

    private String getCurrentIntervalName() {
        int ticks = AutoSaveConfig.intervalTicks;

        for (IntervalOption option : INTERVALS) {
            if (option.ticks == ticks) {
                return option.name;
            }
        }

        return "5 min";
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
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF000000
        );

        graphics.fill(
                left + 8,
                top + 8,
                left + panelWidth + 8,
                top + panelHeight + 8,
                0x60000000
        );

        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + panelHeight,
                0xFF050505
        );

        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + 1,
                0xFFFFFFFF
        );

        graphics.fill(
                left,
                top + panelHeight - 1,
                left + panelWidth,
                top + panelHeight,
                0xFFFFFFFF
        );

        graphics.fill(
                left,
                top,
                left + 1,
                top + panelHeight,
                0xFFFFFFFF
        );

        graphics.fill(
                left + panelWidth - 1,
                top,
                left + panelWidth,
                top + panelHeight,
                0xFFFFFFFF
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 35,
                top + 23,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Sauvegarde automatique de votre monde"
                ),
                left + 35,
                top + 44,
                0xFFAAAAAA,
                false
        );

        graphics.fill(
                left + 35,
                top + 68,
                left + panelWidth - 35,
                top + 69,
                0xFFFFFFFF
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 45,
                top + 94,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Activer ou désactiver les sauvegardes automatiques"
                ),
                left + 45,
                top + 114,
                0xFF999999,
                false
        );

        graphics.text(
                this.font,
                Component.literal("INTERVALLE"),
                left + 45,
                top + 150,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Fréquence des sauvegardes automatiques"
                ),
                left + 45,
                top + 170,
                0xFF999999,
                false
        );

        graphics.text(
                this.font,
                Component.literal("DOSSIER DE SAUVEGARDE"),
                left + 45,
                top + 260,
                0xFFFFFFFF,
                true
        );

        String folderText =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "Aucun dossier sélectionné"
                        : AutoSaveConfig.folder;

        if (folderText.length() > 88) {
            folderText = "..." + folderText.substring(
                    folderText.length() - 85
            );
        }

        int folderBoxX = left + 45;
        int folderBoxY = top + 275;
        int folderBoxWidth = panelWidth - 90;

        graphics.fill(
                folderBoxX,
                folderBoxY,
                folderBoxX + folderBoxWidth,
                folderBoxY + 25,
                0xFF000000
        );

        graphics.fill(
                folderBoxX,
                folderBoxY,
                folderBoxX + folderBoxWidth,
                folderBoxY + 1,
                0xFF555555
        );

        graphics.fill(
                folderBoxX,
                folderBoxY + 24,
                folderBoxX + folderBoxWidth,
                folderBoxY + 25,
                0xFF555555
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                folderBoxX + 10,
                folderBoxY + 8,
                0xFFFFFFFF,
                false
        );

        graphics.text(
                this.font,
                Component.literal(status),
                left + 45,
                top + panelHeight - 93,
                0xFFAAAAAA,
                false
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Intervalle actuel : "
                                + getCurrentIntervalName()
                ),
                left + 45,
                top + panelHeight - 75,
                0xFF666666,
                false
        );

        if (intervalMenuOpen) {
            int menuX = left + panelWidth - 310;
            int menuY = top + 177;
            int menuWidth = 260;
            int optionHeight = 26;

            graphics.fill(
                    menuX - 2,
                    menuY - 2,
                    menuX + menuWidth + 2,
                    menuY + INTERVALS.length * optionHeight + 2,
                    0xFFFFFFFF
            );

            graphics.fill(
                    menuX,
                    menuY,
                    menuX + menuWidth,
                    menuY + INTERVALS.length * optionHeight,
                    0xFF000000
            );
        }

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
