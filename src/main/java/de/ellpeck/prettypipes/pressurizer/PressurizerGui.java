package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PressurizerGui extends AbstractContainerScreen<PressurizerContainer> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PrettyPipes.ID, "textures/gui/pressurizer.png");

    public PressurizerGui(PressurizerContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn, 176, 137);
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractTooltip(graphics, mouseX, mouseY);
        if (mouseX >= this.leftPos + 26 && mouseY >= this.topPos + 22 && mouseX < this.leftPos + 26 + 124 && mouseY < this.topPos + 22 + 12)
            graphics.setTooltipForNextFrame(this.font, Component.translatable("info." + PrettyPipes.ID + ".energy", this.menu.tile.getEnergy(), this.menu.tile.getMaxEnergy()), mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752, false);
        graphics.text(this.font, this.title.getString(), 8, 6, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitInscribed(PressurizerGui.TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 137);
        var energy = (int) (this.menu.tile.getEnergyPercentage() * 124);
        graphics.blitInscribed(PressurizerGui.TEXTURE, this.leftPos + 26, this.topPos + 22, 0, 137, energy, 12);
    }
}
