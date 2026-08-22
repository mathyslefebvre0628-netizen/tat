package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {
    private final Screen parent;
    private String status = "Prêt";

    public AutoSaveScreen(Screen parent) {
        super(Component.literal("Auto Save"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = 420;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(25, (this.height - 300) / 2);

        addRenderableWidget(Button.builder(Component.literal(toggleText()), button -> {
            AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
            AutoSaveConfig.save();
            button.setMessage(Component.literal(toggleText()));
        }).bounds(left + 40, top + 70, 340, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Choisir un dossier"), button -> AutoSaveService.chooseFolder(this))
                .bounds(left + 40, top + 110, 165, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Utiliser une sauvegarde"), button ->
                this.minecraft.gui.setScreen(new BackupSelectScreen(this)))
                .bounds(left + 215, top + 110, 165, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Enregistrer"), button -> {
            AutoSaveConfig.save();
            status = "Configuration enregistrée";
        }).bounds(left + 40, top + 230, 165, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Fermer"), button -> onClose())
                .bounds(left + 215, top + 230, 165, 24).build());
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
    }

    private String toggleText() {
        return "Auto Save : " + (AutoSaveConfig.enabled ? "ON" : "OFF");
    }

    @Override
    public void onClose() {
        AutoSaveConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, this.width, this.height, 0xFF050505);
        int panelWidth = 420;
        int panelHeight = 300;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(25, (this.height - panelHeight) / 2);

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF101010);
        graphics.text(this.font, Component.literal("AUTO SAVE"), left + 40, top + 24, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Sauvegardes automatiques du monde"), left + 40, top + 45, 0xFFAAAAAA, false);

        String folderText = AutoSaveConfig.folder.isBlank() ? "Dossier : non configuré" : "Dossier : " + AutoSaveConfig.folder;
        if (folderText.length() > 52) folderText = folderText.substring(0, 49) + "...";
        graphics.text(this.font, folderText, left + 40, top + 155, 0xFFCCCCCC, false);
        graphics.text(this.font, "Intervalle : 5 minutes", left + 40, top + 175, 0xFF888888, false);
        graphics.text(this.font, status, left + 40, top + 205, 0xFFAAAAAA, false);
    }
}
