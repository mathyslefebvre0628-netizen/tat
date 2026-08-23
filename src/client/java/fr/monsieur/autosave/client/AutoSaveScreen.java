package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 340;

    private final Screen parent;

    private Button toggleButton;
    private String status = "Prêt";

    public AutoSaveScreen(Screen parent) {
        super(Component.literal("Auto Save"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        if (top < 20) {
            top = 20;
        }

        toggleButton = Button.builder(
                Component.literal(toggleText()),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();
                    button.setMessage(Component.literal(toggleText()));
                }
        ).bounds(
                left + 40,
                top + 90,
                480,
                24
        ).build();

        addRenderableWidget(toggleButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Choisir un dossier"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        left + 40,
                        top + 140,
                        230,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Utiliser une sauvegarde"),
                        button -> openBackupScreen()
                ).bounds(
                        left + 290,
                        top + 140,
                        230,
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
                        left + 40,
                        top + 275,
                        230,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Fermer"),
                        button -> onClose()
                ).bounds(
                        left + 290,
                        top + 275,
                        230,
                        24
                ).build()
        );
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
    }

    private void openBackupScreen() {
        this.minecraft.gui.setScreen(
                new BackupSelectScreen(this)
        );
    }

    private String toggleText() {
        return "Auto Save : "
                + (AutoSaveConfig.enabled ? "ON" : "OFF");
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

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        if (top < 20) {
            top = 20;
        }

        // Fond général noir
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF050505
        );

        // Ombre
        graphics.fill(
                left + 6,
                top + 6,
                left + PANEL_WIDTH + 6,
                top + PANEL_HEIGHT + 6,
                0x70000000
        );

        // Panneau principal
        graphics.fill(
                left,
                top,
                left + PANEL_WIDTH,
                top + PANEL_HEIGHT,
                0xFF111111
        );

        // Barre supérieure
        graphics.fill(
                left,
                top,
                left + PANEL_WIDTH,
                top + 58,
                0xFF171717
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

        // Sous-titre
        graphics.text(
                this.font,
                Component.literal(
                        "Sauvegarde automatique de votre monde"
                ),
                left + 30,
                top + 39,
                0xFFAAAAAA,
                false
        );

        // Dossier
        String folderText;

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {

            folderText = "Dossier : non configuré";

        } else {

            folderText =
                    "Dossier : " + AutoSaveConfig.folder;
        }

        if (folderText.length() > 75) {
            folderText =
                    folderText.substring(0, 72) + "...";
        }

        graphics.text(
                this.font,
                Component.literal(folderText),
                left + 40,
                top + 205,
                0xFFCCCCCC,
                false
        );

        // Intervalle
        graphics.text(
                this.font,
                Component.literal(
                        "Intervalle : 5 minutes"
                ),
                left + 40,
                top + 225,
                0xFF888888,
                false
        );

        // Statut
        graphics.text(
                this.font,
                Component.literal(status),
                left + 40,
                top + 250,
                0xFFAAAAAA,
                false
        );
    }
}
