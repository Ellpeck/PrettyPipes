package de.ellpeck.prettypipes.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FilterSlot extends SlotItemHandler {

    private final boolean onlyOneItem;

    public FilterSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean onlyOneItem) {
        super(itemHandler, index, xPosition, yPosition);
        this.onlyOneItem = onlyOneItem;
    }

    public static boolean checkFilter(Container container, int slotId, PlayerEntity player) {
        if (slotId >= 0 && slotId < container.inventorySlots.size()) {
            Slot slot = container.getSlot(slotId);
            if (slot instanceof FilterSlot) {
                ((FilterSlot) slot).slotClick(player);
                return true;
            }
        }
        return false;
    }

    private void slotClick(PlayerEntity player) {
        ItemStack heldStack = player.inventory.getItemStack();
        ItemStack stackInSlot = this.getStack();

        if (!stackInSlot.isEmpty() && heldStack.isEmpty()) {
            this.putStack(ItemStack.EMPTY);
        } else if (!heldStack.isEmpty()) {
            ItemStack s = heldStack.copy();
            if (this.onlyOneItem)
                s.setCount(1);
            this.putStack(s);
        }
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public void putStack(ItemStack stack) {
        super.putStack(stack.copy());
    }

    @Override
    public boolean canTakeStack(PlayerEntity playerIn) {
        return false;
    }
}
