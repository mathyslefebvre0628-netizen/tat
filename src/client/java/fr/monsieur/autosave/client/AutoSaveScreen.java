package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int WIDTH = 620;
    private static final int HEIGHT = 360;

    private final Screen parent;

    private Button toggleButton;
    private String status = "Prêt";

    public AutoSaveScreen(Screen parent) {
        super(Component.literal("Auto Save"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - WIDTH) / 2;
        int top = Math.max(15, (this.height - HEIGHT) / 2);

        // Auto Save
        toggleButton = Button.builder(
                Component.literal(toggleText()),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();
                    button.setMessage(Component.literal(toggleText()));
                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde automatique activée"
                            : "Sauvegarde automatique désactivée";
                }
        ).bounds(
                left + 420,
                top + 78,
                155,
                28
        ).build();

        addRenderableWidget(toggleButton);

        // Choisir un dossier
        addRenderableWidget(
                Button.builder(
                        Component.literal("Choisir un dossier"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + 35,
                        top + 190,
                        265,
                        30
                ).build()
        );

        // Sauvegardes
        addRenderableWidget(
                Button.builder(
                        Component.literal("Utiliser une sauvegarde"),
                        button -> openBackupScreen()
                ).bounds(
                        left + 320,
                        top + 190,
                        265,
                        30
                ).build()
        );

        // Enregistrer
        addRenderableWidget(
                Button.builder(
                        Component.literal("Enregistrer"),
                        button -> {
                            AutoSaveConfig.save();
                            status = "Configuration enregistrée";
                        }
                ).bounds(
                        left + 35,
                        top + 305,
                        265,
                        28
                ).build()
        );

        // Fermer
        addRenderableWidget(
                Button.builder(
                        Component.literal("Fermer"),
                        button -> onClose()
                ).bounds(
                        left + 320,
                        top + 305,
                        265,
                        28
                ).build()
        );
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
    }

    private void openBackupScreen() {
        this.minecraft.setScreen(
                new BackupSelectScreen(this)
        );
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
            this.minecraft.setScreen(parent);
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

        int left = (this.width - WIDTH) / 2;
        int top = Math.max(15, (this.height - HEIGHT) / 2);

        // Fond
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF050505
        );

        // Ombre
        graphics.fill(
                left + 7,
                top + 7,
                left + WIDTH + 7,
                top + HEIGHT + 7,
                0x70000000
        );

        // Cadre
        graphics.fill(
                left,
                top,
                left + WIDTH,
                top + HEIGHT,
                0xFF111111
        );

        // Barre supérieure
        graphics.fill(
                left,
                top,
                left + WIDTH,
                top + 64,
                0xFF181818
        );

        // Ligne de séparation
        graphics.fill(
                left,
                top + 63,
                left + WIDTH,
                top + 64,
                0xFF292929
        );

        // Titre
        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                left + 30,
                top + 20,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Sauvegarde automatique de votre monde"
                ),
                left + 30,
                top + 40,
                0xFF8F8F8F,
                false
        );

        // Section Auto Save
        graphics.text(
                this.font,
                Component.literal("Sauvegarde automatique"),
                left + 35,
                top + 83,
                0xFFFFFFFF,
                false
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Créer automatiquement une copie de votre monde"
                ),
                left + 35,
                top + 101,
                0xFF888888,
                false
        );

        // Section dossier
        graphics.text(
                this.font,
                Component.literal("Dossier de sauvegarde"),
                left + 35,
                top + 150,
                0xFFFFFFFF,
                false
        );

        String folderText;

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {
            folderText = "Aucun dossier sélectionné";
        } else {
            folderText = AutoSaveConfig.folder;
        }

        if (folderText.length() > 75) {
            folderText = folderText.substring(0, 72) + "...";
        }

        // Zone du chemin
        graphics.fill(
                left + 35,
                top + 160,
                left + 585,
                top + 178,
                0xFF0B0B0B
        );

        graphics.text(
                this.font,
                Component.literal(folderText),
                left + 43,
                top + 165,
                0xFFBDBDBD,
                false
        );

        // Statut
        graphics.text(
                this.font,
                Component.literal(status),
                left + 35,
                top + 270,
                0xFF888888,
                false
        );

        // Intervalle
        graphics.text(
                this.font,
                Component.literal("Intervalle automatique : 5 minutes"),
                left + 35,
                top + 285,
                0xFF666666,
                false
        );
    }
}
