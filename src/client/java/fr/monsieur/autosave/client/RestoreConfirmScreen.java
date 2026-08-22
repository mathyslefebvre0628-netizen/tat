package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RestoreConfirmScreen extends Screen {
    private final Screen backupList;
    private final Screen parent;
    private final String backupName;
    private String status = "";

    public RestoreConfirmScreen(Screen backupList, Screen parent, String backupName) {
        super(Component.literal("Confirmer la restauration"));
        this.backupList = backupList;
        this.parent = parent;
        this.backupName = backupName;
    }

    @Override
    protected void init() {
        int width = 420;
        int left = (this.width - width) / 2;
        int top = Math.max(30, (this.height - 230) / 2);
        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> this.minecraft.gui.setScreen(backupList))
                .bounds(left, top + 155, 200, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Restaurer"), button -> restore())
                .bounds(left + 220, top + 155, 200, 24).build());
    }

    private void restore() {
        try {
            AutoSaveService.restore(Minecraft.getInstance(), backupName);
            status = "Restauration terminée : " + prepared;
            this.minecraft.gui.setScreen(parent);
        } catch (Exception e) {
            status = "Échec : " + e.getMessage();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, 0xFF050505);
        int width = 420;
        int height = 230;
        int left = (this.width - width) / 2;
        int top = Math.max(40, (this.height - height) / 2);
        graphics.fill(left, top, left + width, top + height, 0xFF101010);
        graphics.text(this.font, Component.literal("RESTAURER CETTE SAUVEGARDE ?"), left + 20, top + 25, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal(backupName), left + 20, top + 60, 0xFFCCCCCC, false);
        graphics.text(this.font, Component.literal("Une sauvegarde de sécurité sera créée."), left + 20, top + 90, 0xFFAAAAAA, false);
        graphics.text(this.font, Component.literal("Le monde actuel sera remplacé après création d'une sauvegarde de sécurité."), left + 20, top + 112, 0xFFAAAAAA, false);
        graphics.text(this.font, status, left + 20, top + 205, 0xFFAAAAAA, false);
    }
}
