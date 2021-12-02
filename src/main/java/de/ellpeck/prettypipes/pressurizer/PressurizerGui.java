package de.ellpeck.prettypipes.pressurizer;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PressurizerGui extends ContainerScreen<PressurizerContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/pressurizer.png");

    public PressurizerGui(PressurizerContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176;
        this.ySize = 137;
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        this.func_230459_a_(matrix, mouseX, mouseY);
        if (mouseX >= this.guiLeft + 26 && mouseY >= this.guiTop + 22 && mouseX < this.guiLeft + 26 + 124 && mouseY < this.guiTop + 22 + 12)
            this.renderTooltip(matrix, new TranslationTextComponent("info." + PrettyPipes.ID + ".energy", this.container.tile.getEnergy(), this.container.tile.getMaxEnergy()), mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
        this.font.drawString(matrix, this.playerInventory.getDisplayName().getString(), 8, this.ySize - 96 + 2, 4210752);
        this.font.drawString(matrix, this.title.getString(), 8, 6, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, 176, 137);
        int energy = (int) (this.container.tile.getEnergyPercentage() * 124);
        this.blit(matrixStack, this.guiLeft + 26, this.guiTop + 22, 0, 137, energy, 12);
    }
}
