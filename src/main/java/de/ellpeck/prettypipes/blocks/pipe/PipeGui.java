package de.ellpeck.prettypipes.blocks.pipe;

import com.mojang.blaze3d.platform.GlStateManager;
import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class PipeGui extends ContainerScreen<PipeContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/pipe.png");

    public PipeGui(PipeContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176;
        this.ySize = 171;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.title.getFormattedText(), 8, 6, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(this.guiLeft, this.guiTop, 0, 0, 176, 171);

        for (int i = 0; i < 3; i++)
            this.blit(this.guiLeft + 61 + i * 18, this.guiTop + 16, 176, 62, 18, 18);
    }
}
