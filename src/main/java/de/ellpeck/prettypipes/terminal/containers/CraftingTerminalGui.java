package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.platform.InputConstants;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CraftingTerminalGui extends ItemTerminalGui {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PrettyPipes.ID, "textures/gui/crafting_terminal.png");
    private Button requestButton;
    private Button sendBackButton;
    private ItemTerminalWidget draggedItem;
    private double initialDragX;
    private double initialDragY;
    private boolean dragging;

    public CraftingTerminalGui(ItemTerminalContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn, 256, 236);
    }

    @Override
    protected void init() {
        super.init();
        this.requestButton = this.addRenderableWidget(Button.builder(Component.translatable("info." + PrettyPipes.ID + ".request"), button -> {
            var amount = ItemTerminalGui.requestModifier(this.minecraft);
            // also allow holding backspace instead of alt for people whose alt key is inaccessible (linux?)
            var force = this.minecraft.hasAltDown() || InputConstants.isKeyDown(this.minecraft.getWindow(), 259) ? 1 : 0;
            ClientPacketDistributor.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_REQUEST, Arrays.asList(amount, force)));
        }).bounds(this.leftPos + 8, this.topPos + 100, 50, 20).build());
        this.sendBackButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            ClientPacketDistributor.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_SEND_BACK, List.of()));
        }).bounds(this.leftPos + 47, this.topPos + 72, 12, 12).build());
        this.tick();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        var tile = this.getCraftingContainer().getTile();
        this.requestButton.active = false;
        this.sendBackButton.active = false;
        for (var i = 0; i < tile.craftItems.getSlots(); i++) {
            var requestStack = tile.getRequestedCraftItem(i);
            if (!requestStack.isEmpty() && requestStack.getCount() < requestStack.getMaxStackSize())
                this.requestButton.active = true;
            var realStack = tile.craftItems.getStackInSlot(i);
            if (!realStack.isEmpty())
                this.sendBackButton.active = true;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);

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
            graphics.item(ghost, slot.x, slot.y);
            graphics.itemDecorations(this.font, ghost, slot.x, slot.y, "0");
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Starts drag if clicked widget has item
        if (event.button() == 0 && this.draggedItem == null) {
            this.getChildAt(event.x(), event.y()).ifPresent(child -> {
                this.draggedItem = child instanceof ItemTerminalWidget widget && !widget.stack.isEmpty() ? widget : null;
                this.initialDragX = event.x();
                this.initialDragY = event.y();
            });
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        // Makes sure clicks aren't counted as drags
        if (!this.dragging && this.draggedItem != null && Mth.lengthSquared(event.x() - this.initialDragX, event.y() - this.initialDragY) >= 2 * 2)
            this.dragging = true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        var curSlot = this.getSlotUnderMouse();
        if (this.draggedItem != null && !this.draggedItem.stack.isEmpty() && curSlot != null && curSlot.index >= 0 && curSlot.index <= 9) {
            var ghostStack = this.draggedItem.stack.copyWithCount(1);
            List<PacketGhostSlot.Entry> stacks = new ArrayList<>();
            if (this.menu.tile instanceof CraftingTerminalBlockEntity craftingTerminalBlockEntity) {
                for (var i = 0; i < craftingTerminalBlockEntity.ghostItems.getSlots(); i++) {
                    if (i != curSlot.index - 1) {
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(craftingTerminalBlockEntity.ghostItems.getStackInSlot(i))), Optional.empty()));
                    } else {
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(ghostStack)), Optional.empty()));
                    }
                }
                ClientPacketDistributor.sendToServer(new PacketGhostSlot(craftingTerminalBlockEntity.getBlockPos(), stacks));
            }
        }
        this.draggedItem = null;
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        // Renders dragged item at cursor with z offset
        if (this.dragging && this.draggedItem != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, 0);
            graphics.item(this.draggedItem.stack, mouseX - 9, mouseY - 9);
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected Identifier getTexture() {
        return CraftingTerminalGui.TEXTURE;
    }

    @Override
    protected int getXOffset() {
        return 65;
    }

    protected CraftingTerminalContainer getCraftingContainer() {
        return (CraftingTerminalContainer) this.menu;
    }
}
