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

    private boolean intervalMenuOpen = false;
    private String status = "Prêt";

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
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        /*
         * AUTO SAVE ON / OFF
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

        /*
         * CHOISIR UN DOSSIER
         */
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

        /*
         * UTILISER UNE SAUVEGARDE
         */
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

        /*
         * SÉLECTEUR D'INTERVALLE
         */
        intervalButton = Button.builder(
                Component.literal(getCurrentIntervalName()),
                button -> {
                    intervalMenuOpen = !intervalMenuOpen;
                    status = intervalMenuOpen
                            ? "Choisis un intervalle"
                            : "Intervalle : " + getCurrentIntervalName();
                }
        ).bounds(
                left + panelWidth - 190,
                top + 147,
                140,
                24
        ).build();

        addRenderableWidget(intervalButton);

        /*
         * ENREGISTRER
         */
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

        /*
         * FERMER
         */
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

    private void selectInterval(IntervalOption option) {
        AutoSaveConfig.intervalTicks = option.ticks;
        AutoSaveConfig.save();

        intervalMenuOpen = false;

        if (intervalButton != null) {
            intervalButton.setMessage(
                    Component.literal(option.name)
            );
        }

        status = "Intervalle : " + option.name;
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
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (intervalMenuOpen) {
            int panelWidth = getPanelWidth();
            int panelHeight = getPanelHeight();

            int left = (this.width - panelWidth) / 2;
            int top = (this.height - panelHeight) / 2;

            int menuX = left + panelWidth - 310;
            int menuY = top + 177;
            int menuWidth = 260;
            int optionHeight = 26;

            for (int i = 0; i < INTERVALS.length; i++) {
                int optionY = menuY + i * optionHeight;

                if (mouseX >= menuX
                        && mouseX <= menuX + menuWidth
                        && mouseY >= optionY
                        && mouseY <= optionY + optionHeight) {

                    selectInterval(INTERVALS[i]);
                    return true;
                }
            }

            if (mouseX < menuX
                    || mouseX > menuX + menuWidth
                    || mouseY < menuY
                    || mouseY > menuY + INTERVALS.length * optionHeight) {

                intervalMenuOpen = false;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

        /*
         * =========================================================
         * FOND
         * =========================================================
         */

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF000000
        );

        /*
         * Ombre extérieure
         */
        graphics.fill(
                left + 8,
                top + 8,
                left + panelWidth + 8,
                top + panelHeight + 8,
                0x60000000
        );

        /*
         * Panneau noir
         */
        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + panelHeight,
                0xFF050505
        );

        /*
         * Bordure blanche très fine
         */
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

        /*
         * =========================================================
         * HEADER
         * =========================================================
         */

        graphics.fill(
                left + 1,
                top + 1,
                left + panelWidth - 1,
                top + 70,
                0xFF000000
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

        /*
         * Ligne blanche
         */
        graphics.fill(
                left + 35,
                top + 68,
                left + panelWidth - 35,
                top + 69,
                0xFFFFFFFF
        );

        /*
         * =========================================================
         * AUTO SAVE
         * =========================================================
         */

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

        /*
         * =========================================================
         * INTERVALLE
         * =========================================================
         */

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

        /*
         * =========================================================
         * DOSSIER
         * =========================================================
         */

        graphics.text(
                this.font,
                Component.literal("DOSSIER DE SAUVEGARDE"),
                left + 45,
                top + 260,
                0xFFFFFFFF,
                true
        );

        String folderText;

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            folderText = "Aucun dossier sélectionné";
        } else {
            folderText = AutoSaveConfig.folder;
        }

        if (folderText.length() > 88) {
            folderText =
                    "..." + folderText.substring(
                            folderText.length() - 85
                    );
        }

        /*
         * Zone noire du chemin avec contour blanc
         */
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

        graphics.fill(
                folderBoxX,
                folderBoxY,
                folderBoxX + 1,
                folderBoxY + 25,
                0xFF555555
        );

        graphics.fill(
                folderBoxX + folderBoxWidth - 1,
                folderBoxY,
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

        /*
         * =========================================================
         * STATUS
         * =========================================================
         */

        graphics.text(
                this.font,
                Component.literal(status),
                left + 45,
                top + panelHeight - 93,
                0xFFAAAAAA,
                false
        );

        /*
         * =========================================================
         * MENU INTERVALLE
         * =========================================================
         */

        if (intervalMenuOpen) {
            int menuX = left + panelWidth - 310;
            int menuY = top + 177;
            int menuWidth = 260;
            int optionHeight = 26;

            /*
             * Fond du menu
             */
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

            for (int i = 0; i < INTERVALS.length; i++) {
                IntervalOption option = INTERVALS[i];

                int optionY =
                        menuY + i * optionHeight;

                boolean hovered =
                        mouseX >= menuX
                                && mouseX <= menuX + menuWidth
                                && mouseY >= optionY
                                && mouseY <= optionY + optionHeight;

                if (hovered) {
                    graphics.fill(
                            menuX,
                            optionY,
                            menuX + menuWidth,
                            optionY + optionHeight,
                            0xFFFFFFFF
                    );
                }

                graphics.text(
                        this.font,
                        Component.literal(option.name),
                        menuX + 12,
                        optionY + 8,
                        hovered
                                ? 0xFF000000
                                : 0xFFFFFFFF,
                        false
                );
            }
        }

        /*
         * IMPORTANT :
         * Minecraft dessine les widgets après notre fond.
         * Cela évite que notre fond noir recouvre les boutons.
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
