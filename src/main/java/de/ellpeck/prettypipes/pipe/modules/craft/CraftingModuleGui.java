package de.ellpeck.prettypipes.pipe.modules.craft;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class CraftingModuleGui extends AbstractPipeGui<CraftingModuleContainer> {
    public CraftingModuleGui(CraftingModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(matrix, partialTicks, mouseX, mouseY);
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(matrix, this.guiLeft + 176 / 2 - 16 / 2, this.guiTop + 32 + 18 * 2, 176, 80, 16, 16);
    }
}
