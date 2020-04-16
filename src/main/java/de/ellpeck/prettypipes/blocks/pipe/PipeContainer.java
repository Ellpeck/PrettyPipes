package de.ellpeck.prettypipes.blocks.pipe;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.commons.lang3.Range;

import javax.annotation.Nullable;

public class PipeContainer extends Container {

    public final PipeTileEntity tile;
    public final IModule openModule;

    public PipeContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, PipeTileEntity tile, IModule openModule) {
        super(type, id);
        this.tile = tile;
        this.openModule = openModule;

        if (openModule == null) {
            for (int i = 0; i < 3; i++)
                this.addSlot(new SlotItemHandler(tile.modules, i, 62 + i * 18, 17 + 32));
        } else {
            for (Slot slot : openModule.getContainerSlots(tile, this))
                this.addSlot(slot);
        }

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 89 + l * 18 + 32));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 147 + 32));
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        int inventoryStart = (int) this.inventorySlots.stream().filter(slot -> slot.inventory != player.inventory).count();
        int inventoryEnd = inventoryStart + 26;
        int hotbarStart = inventoryEnd + 1;
        int hotbarEnd = hotbarStart + 8;

        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack newStack = slot.getStack();
            ItemStack currentStack = newStack.copy();

            inv:
            if (slotIndex >= inventoryStart) {
                // shift into this container here
                // mergeItemStack with the slots that newStack should go into
                // return an empty stack if mergeItemStack fails
                if (this.openModule == null) {
                    if (newStack.getItem() instanceof IModule) {
                        if (!this.mergeItemStack(newStack, 0, 3, false))
                            return ItemStack.EMPTY;
                        break inv;
                    }
                } else {
                    // bleh
                    Range<Integer> range = this.openModule.getShiftClickSlots(this.tile, this, newStack);
                    if (range != null) {
                        if (!this.mergeItemStack(newStack, range.getMinimum(), range.getMaximum(), false))
                            return ItemStack.EMPTY;
                        break inv;
                    }
                }
                // end custom code

                if (slotIndex >= inventoryStart && slotIndex <= inventoryEnd) {
                    if (!this.mergeItemStack(newStack, hotbarStart, hotbarEnd + 1, false))
                        return ItemStack.EMPTY;
                } else if (slotIndex >= inventoryEnd + 1 && slotIndex < hotbarEnd + 1 && !this.mergeItemStack(newStack, inventoryStart, inventoryEnd + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(newStack, inventoryStart, hotbarEnd + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (newStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (newStack.getCount() == currentStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, newStack);
            return currentStack;
        }
        return ItemStack.EMPTY;
    }
}
