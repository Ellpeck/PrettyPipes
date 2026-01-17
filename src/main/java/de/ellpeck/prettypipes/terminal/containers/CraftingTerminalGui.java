package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.platform.InputConstants;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CraftingTerminalGui extends ItemTerminalGui {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "textures/gui/crafting_terminal.png");
    private Button requestButton;
    private Button sendBackButton;

    private ItemTerminalWidget draggedItem;
    private boolean dragging;

    public CraftingTerminalGui(ItemTerminalContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.requestButton = this.addRenderableWidget(Button.builder(Component.translatable("info." + PrettyPipes.ID + ".request"), button -> {
            var amount = ItemTerminalGui.requestModifier();
            // also allow holding backspace instead of alt for people whose alt key is inaccessible (linux?)
            var force = Screen.hasAltDown() || InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), 259) ? 1 : 0;
            PacketDistributor.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_REQUEST, Arrays.asList(amount, force)));
        }).bounds(this.leftPos + 8, this.topPos + 100, 50, 20).build());
        this.sendBackButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            PacketDistributor.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CRAFT_TERMINAL_SEND_BACK, List.of()));
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderLabels(graphics, mouseX, mouseY);
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
                graphics.renderItem(ghost, slot.x, slot.y);
                graphics.renderItemDecorations(this.font, ghost, slot.x, slot.y, "0");
            }
        }

    @Override
    protected ResourceLocation getTexture() {
        return CraftingTerminalGui.TEXTURE;
    }

    @Override
    protected int getXOffset() {
        return 65;
    }

    protected CraftingTerminalContainer getCraftingContainer() {
        return (CraftingTerminalContainer) this.menu;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 0 && this.draggedItem == null) {
            this.getChildAt(mouseX,mouseY).ifPresent(widget -> this.draggedItem = widget instanceof ItemTerminalWidget  terminalWidget && !terminalWidget.stack.isEmpty()? terminalWidget : null);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        Slot curSlot= getSlotUnderMouse();
        if(draggedItem!=null && !draggedItem.stack.isEmpty() && curSlot!=null && curSlot.index>=0 && curSlot.index <9) {
            List<PacketGhostSlot.Entry> stacks = new ArrayList<>();
            if( menu.tile instanceof CraftingTerminalBlockEntity craftingTerminalBlockEntity) {
                for (int i = 0; i < craftingTerminalBlockEntity.ghostItems.getSlots(); i++) {
                    if (i != curSlot.index-1)
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(craftingTerminalBlockEntity.ghostItems.getStackInSlot(i))), Optional.empty()));
                    else {
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(draggedItem.stack)), Optional.empty()));
                    }

                }
                PacketDistributor.sendToServer(new PacketGhostSlot(craftingTerminalBlockEntity.getBlockPos(), stacks));
            }
        }
        dragging=false;
        draggedItem=null;
        return super.mouseReleased(mouseX, mouseY, button);

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int i, double j, double k) {
        if(this.draggedItem != null) {
            double distance = j*j+k*k;
            if(distance>2)
                dragging=true;
        }
        return super.mouseDragged(mouseX, mouseY, i, j, k);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if(draggedItem!=null && dragging) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.renderItem(draggedItem.stack, mouseX-9, mouseY - 9);
            graphics.pose().popPose();
        }
    }

}
