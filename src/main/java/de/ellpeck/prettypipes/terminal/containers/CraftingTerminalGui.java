package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

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
        this.requestButton = this.addButton(new Button(this.guiLeft + 8, this.guiTop + 100, 50, 20, new TranslationTextComponent("info." + PrettyPipes.ID + ".request"), button -> {
            int amount = requestModifier();
            int force = hasAltDown() ? 1 : 0;
            PacketHandler.sendToServer(new PacketButton(this.container.tile.getPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_REQUEST, amount, force));
        }));
        this.tick();
    }

    @Override
    public void tick() {
        super.tick();
        CraftingTerminalTileEntity tile = this.getCraftingContainer().getTile();
        this.requestButton.active = false;
        for (int i = 0; i < tile.craftItems.getSlots(); i++) {
            ItemStack stack = tile.getRequestedCraftItem(i);
            if (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize()) {
                this.requestButton.active = true;
                break;
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(matrix, mouseX, mouseY);

        CraftingTerminalContainer container = this.getCraftingContainer();
        CraftingTerminalTileEntity tile = container.getTile();
        for (int i = 0; i < tile.ghostItems.getSlots(); i++) {
            if (!tile.craftItems.getStackInSlot(i).isEmpty())
                continue;
            ItemStack ghost = tile.ghostItems.getStackInSlot(i);
            if (ghost.isEmpty())
                continue;
            int finalI = i;
            Slot slot = container.inventorySlots.stream().filter(s -> s.inventory == container.craftInventory && s.getSlotIndex() == finalI).findFirst().orElse(null);
            if (slot == null)
                continue;
            this.minecraft.getItemRenderer().renderItemIntoGUI(ghost, slot.xPos, slot.yPos);
            this.minecraft.getItemRenderer().renderItemOverlayIntoGUI(this.font, ghost, slot.xPos, slot.yPos, "0");
        }
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
