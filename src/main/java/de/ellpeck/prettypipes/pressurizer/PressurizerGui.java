package de.ellpeck.prettypipes.pressurizer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PressurizerGui extends AbstractContainerScreen<PressurizerContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/pressurizer.png");

    public PressurizerGui(PressurizerContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 176;
        this.imageHeight = 137;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrix, mouseX, mouseY);
        if (mouseX >= this.leftPos + 26 && mouseY >= this.topPos + 22 && mouseX < this.leftPos + 26 + 124 && mouseY < this.topPos + 22 + 12)
            this.renderTooltip(matrix, Component.translatable("info." + PrettyPipes.ID + ".energy", this.menu.tile.getEnergy(), this.menu.tile.getMaxEnergy()), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack matrix, int mouseX, int mouseY) {
        this.font.draw(matrix, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752);
        this.font.draw(matrix, this.title.getString(), 8, 6, 4210752);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PressurizerGui.TEXTURE);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, 176, 137);
        var energy = (int) (this.menu.tile.getEnergyPercentage() * 124);
        this.blit(matrixStack, this.leftPos + 26, this.topPos + 22, 0, 137, energy, 12);
    }
}
