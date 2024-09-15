package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PressurizerGui extends AbstractContainerScreen<PressurizerContainer> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "textures/gui/pressurizer.png");

    public PressurizerGui(PressurizerContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 176;
        this.imageHeight = 137;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= this.leftPos + 26 && mouseY >= this.topPos + 22 && mouseX < this.leftPos + 26 + 124 && mouseY < this.topPos + 22 + 12)
            graphics.renderTooltip(this.font, Component.translatable("info." + PrettyPipes.ID + ".energy", this.menu.tile.getEnergy(), this.menu.tile.getMaxEnergy()), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752, false);
        graphics.drawString(this.font, this.title.getString(), 8, 6, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(PressurizerGui.TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 137);
        var energy = (int) (this.menu.tile.getEnergyPercentage() * 124);
        graphics.blit(PressurizerGui.TEXTURE, this.leftPos + 26, this.topPos + 22, 0, 137, energy, 12);
    }

}
