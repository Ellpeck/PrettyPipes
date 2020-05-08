package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class CraftingTerminalGui extends ItemTerminalGui {
    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/crafting_terminal.png");

    public CraftingTerminalGui(ItemTerminalContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 256;
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    protected int getXOffset() {
        return 65;
    }
}
