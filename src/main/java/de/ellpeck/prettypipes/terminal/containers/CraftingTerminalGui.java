package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketRequest;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class CraftingTerminalGui extends ItemTerminalGui {
    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/crafting_terminal.png");
    private Button requestButton;

    public CraftingTerminalGui(ItemTerminalContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.requestButton = this.addButton(new Button(this.guiLeft + 8, this.guiTop + 100, 50, 20, I18n.format("info." + PrettyPipes.ID + ".request"), button -> {
            CraftingTerminalTileEntity tile = this.getCraftingContainer().getTile();
            for (int i = 0; i < tile.craftItems.getSlots(); i++) {
                ItemStack stack = tile.getRequestedCraftItem(i);
                if (stack.isEmpty())
                    continue;
                stack = stack.copy();
                stack.setCount(1);
                PacketHandler.sendToServer(new PacketRequest(this.container.tile.getPos(), stack, 1));
            }
        }));
        this.requestButton.active = !this.getCraftingContainer().craftInventory.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        this.requestButton.active = !this.getCraftingContainer().craftInventory.isEmpty();
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    protected int getXOffset() {
        return 65;
    }

    protected CraftingTerminalContainer getCraftingContainer() {
        return (CraftingTerminalContainer) this.container;
    }
}
