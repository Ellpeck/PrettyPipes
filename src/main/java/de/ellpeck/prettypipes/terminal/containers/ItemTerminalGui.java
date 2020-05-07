package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class ItemTerminalGui extends ContainerScreen<ItemTerminalContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/item_terminal.png");

    public ItemTerminalGui(ItemTerminalContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176 + 15;
        this.ySize = 236;
    }

    public void updateItemList(List<ItemStack> items) {
        this.buttons.removeIf(w -> w instanceof ItemTerminalWidget);
        int x = 0;
        int y = 0;
        for (ItemStack stack : items) {
            this.buttons.add(new ItemTerminalWidget(this.guiLeft + 8 + x * 18, this.guiTop + 18 + y * 18, x, y, stack, this));
            x++;
            if (x > 8) {
                x = 0;
                y++;
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        for (Widget widget : this.buttons) {
            if (widget instanceof ItemTerminalWidget)
                widget.renderToolTip(mouseX, mouseY);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
        this.font.drawString(this.title.getFormattedText(), 8, 6, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }

    // public overload for ItemTerminalWidget
    @Override
    public void renderTooltip(ItemStack stack, int x, int y) {
        super.renderTooltip(stack, x, y);
    }
}
