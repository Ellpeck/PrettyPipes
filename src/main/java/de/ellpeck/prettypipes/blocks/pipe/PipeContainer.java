package de.ellpeck.prettypipes.blocks.pipe;

import de.ellpeck.prettypipes.items.UpgradeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class PipeContainer extends Container {
    public PipeContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, PipeTileEntity tile) {
        super(type, id);

        for (int i = 0; i < 3; i++)
            this.addSlot(new SlotItemHandler(tile.upgrades, i, 62 + i * 18, 17));

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 89 + l * 18));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 147));
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        // change this to the amount of slots that we have
        int inventoryStart = 3;
        int inventoryEnd = inventoryStart + 26;
        int hotbarStart = inventoryEnd + 1;
        int hotbarEnd = hotbarStart + 8;

        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack newStack = slot.getStack();
            ItemStack currentStack = newStack.copy();

            if (slotIndex >= inventoryStart) {
                // shift into this container here
                // mergeItemStack with the slots that newStack should go into
                // return an empty stack if mergeItemStack fails
                if (newStack.getItem() instanceof UpgradeItem) {
                    if (!this.mergeItemStack(newStack, 0, 3, false))
                        return ItemStack.EMPTY;
                }
                // end custom code

                else if (slotIndex >= inventoryStart && slotIndex <= inventoryEnd) {
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
