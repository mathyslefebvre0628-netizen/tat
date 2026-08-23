package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    /*
     * Design de référence pour un écran 1920x1080.
     * Les dimensions sont ensuite limitées à la taille réelle du GUI Minecraft.
     */
    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 430;

    private final Screen parent;

    private Button toggleButton;
    private String status = "Prêt";

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
         * Interrupteur ON/OFF
         */
        toggleButton = Button.builder(
                Component.literal(toggleText()),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();

                    button.setMessage(
                            Component.literal(toggleText())
                    );

                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde automatique activée"
                            : "Sauvegarde automatique désactivée";
                }
        ).bounds(
                left + panelWidth - 190,
                top + 94,
                150,
                30
        ).build();

        addRenderableWidget(toggleButton);

        /*
         * Choisir le dossier
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("Choisir un dossier"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + 45,
                        top + 210,
                        (panelWidth - 125) / 2,
                        34
                ).build()
        );

        /*
         * Restaurer
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("Utiliser une sauvegarde"),
                        button -> openBackupScreen()
                ).bounds(
                        left + 80 + (panelWidth - 125) / 2,
                        top + 210,
                        (panelWidth - 125) / 2,
                        34
                ).build()
        );

        /*
         * Enregistrer
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
                        top + panelHeight - 66,
                        (panelWidth - 125) / 2,
                        32
                ).build()
        );

        /*
         * Fermer
         */
        addRenderableWidget(
                Button.builder(
                        Component.literal("Fermer"),
                        button -> onClose()
                ).bounds(
                        left + 80 + (panelWidth - 125) / 2,
                        top + panelHeight - 66,
                        (panelWidth - 125) / 2,
                        32
                ).build()
        );
    }

    private int getPanelWidth() {
        return Math.min(
                PANEL_WIDTH,
                Math.max(520, this.width - 60)
        );
    }

    private int getPanelHeight() {
        return Math.min(
                PANEL_HEIGHT,
                Math.max(340, this.height - 40)
        );
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

    private String toggleText() {
        return AutoSaveConfig.enabled
                ? "ACTIVÉ"
                : "DÉSACTIVÉ";
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
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        /*
         * Fond général
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF070707
        );

        /*
         * Grande ombre derrière le panneau
         */
        graphics.fill(
                left + 8,
                top + 8,
                left + panelWidth + 8,
                top + panelHeight + 8,
                0x65000000
        );

        /*
         * Panneau principal
         */
        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + panelHeight,
                0xFF111111
        );

        /*
         * En-tête
         */
        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + 72,
                0xFF181818
        );

        /*
         * Ligne de séparation
         */
        graphics.fill(
                left,
                top + 71,
                left + panelWidth,
                top + 72,
                0xFF2B2B2B
        );

        /*
         * Titre
         */
        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 35,
                top + 22,
                0xFFFFFFFF,
                true
        );

        /*
         * Sous-titre
         */
        graphics.text(
                this.font,
                Component.literal(
                        "Sauvegarde automatique de votre monde"
                ),
                left + 35,
                top + 43,
                0xFF8C8C8C,
                false
        );

        /*
         * Section AUTO SAVE
         */
        graphics.text(
                this.font,
                Component.literal("Sauvegarde automatique"),
                left + 45,
                top + 100,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Active ou désactive la création automatique des sauvegardes."
                ),
                left + 45,
                top + 122,
                0xFF858585,
                false
        );

        /*
         * Séparateur de section
         */
        graphics.fill(
                left + 45,
                top + 145,
                left + panelWidth - 45,
                top + 146,
                0xFF252525
        );

        /*
         * Section dossier
         */
        graphics.text(
                this.font,
                Component.literal("Dossier de sauvegarde"),
                left + 45,
                top + 168,
                0xFFFFFFFF,
                true
        );

        String folderText = getFolderText();

        /*
         * Zone du chemin
         */
        graphics.fill(
                left + 45,
                top + 180,
                left + panelWidth - 45,
                top + 201,
                0xFF090909
        );

        graphics.fill(
                left + 45,
                top + 200,
                left + panelWidth - 45,
                top + 201,
                0xFF252525
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                left + 55,
                top + 187,
                0xFFBDBDBD,
                false
        );

        /*
         * Statut
         */
        graphics.text(
                this.font,
                Component.literal(status),
                left + 45,
                top + panelHeight - 106,
                0xFF888888,
                false
        );

        /*
         * Intervalle
         */
        graphics.text(
                this.font,
                Component.literal(
                        "Intervalle automatique : 5 minutes"
                ),
                left + 45,
                top + panelHeight - 88,
                0xFF666666,
                false
        );
    }

    private String getFolderText() {
        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            return "Aucun dossier sélectionné";
        }

        String folder = AutoSaveConfig.folder;

        /*
         * On raccourcit uniquement l'affichage.
         * Le vrai chemin reste intégralement enregistré.
         */
        int maxLength = 86;

        if (folder.length() <= maxLength) {
            return folder;
        }

        return "..." + folder.substring(
                folder.length() - (maxLength - 3)
        );
    }
}
