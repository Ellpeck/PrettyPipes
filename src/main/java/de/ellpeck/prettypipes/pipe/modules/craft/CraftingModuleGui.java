package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CraftingModuleGui extends AbstractPipeGui<CraftingModuleContainer> {

    public CraftingModuleGui(CraftingModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        graphics.blit(AbstractPipeGui.TEXTURE, this.leftPos + 176 / 2 - 16 / 2, this.topPos + 32 + 18 * 2, 176, 80, 16, 16);
    }
}
