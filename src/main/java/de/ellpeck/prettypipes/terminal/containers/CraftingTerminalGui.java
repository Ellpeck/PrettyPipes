package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CraftingTerminalGui extends ItemTerminalGui {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/crafting_terminal.png");
    private Button requestButton;

    public CraftingTerminalGui(ItemTerminalContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.requestButton = this.addRenderableWidget(new Button(this.leftPos + 8, this.topPos + 100, 50, 20, new TranslatableComponent("info." + PrettyPipes.ID + ".request"), button -> {
            var amount = requestModifier();
            PacketHandler.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_REQUEST, amount));
        }));
        this.tick();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        var tile = this.getCraftingContainer().getTile();
        this.requestButton.active = false;
        for (var i = 0; i < tile.craftItems.getSlots(); i++) {
            var stack = tile.getRequestedCraftItem(i);
            if (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize()) {
                this.requestButton.active = true;
                break;
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrix, int mouseX, int mouseY) {
        super.renderLabels(matrix, mouseX, mouseY);

        var container = this.getCraftingContainer();
        var tile = container.getTile();
        for (var i = 0; i < tile.ghostItems.getSlots(); i++) {
            if (!tile.craftItems.getStackInSlot(i).isEmpty())
                continue;
            var ghost = tile.ghostItems.getStackInSlot(i);
            if (ghost.isEmpty())
                continue;
            var finalI = i;
            var slot = container.slots.stream().filter(s -> s.container == container.craftInventory && s.getSlotIndex() == finalI).findFirst().orElse(null);
            if (slot == null)
                continue;
            this.minecraft.getItemRenderer().renderGuiItem(ghost, slot.x, slot.y);
            this.minecraft.getItemRenderer().renderGuiItemDecorations(this.font, ghost, slot.x, slot.y, "0");
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
        return (CraftingTerminalContainer) this.menu;
    }
}
