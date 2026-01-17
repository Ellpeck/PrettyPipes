package de.ellpeck.prettypipes.misc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class FilterSlot extends SlotItemHandler {

    private final boolean onlyOneItem;

    public FilterSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean onlyOneItem) {
        super(itemHandler, index, xPosition, yPosition);
        this.onlyOneItem = onlyOneItem;
    }

    public static boolean checkFilter(AbstractContainerMenu menu, int slotId) {
        if (slotId >= 0 && slotId < menu.slots.size()) {
            var slot = menu.getSlot(slotId);
            if (slot instanceof FilterSlot) {
                ((FilterSlot) slot).slotClick(menu);
                return true;
            }
        }
        return false;
    }

    public void slotClick(AbstractContainerMenu menu, ItemStack heldStack) {
        var stackInSlot = this.getItem();

        if (!stackInSlot.isEmpty() && heldStack.isEmpty()) {
            this.set(ItemStack.EMPTY);
        } else if (!heldStack.isEmpty()) {
            var s = heldStack.copy();
            if (this.onlyOneItem)
                s.setCount(1);
            this.set(s);
        }
    }
    private void slotClick(AbstractContainerMenu menu) {
        var heldStack = menu.getCarried();
        slotClick(menu, heldStack);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return false;
    }

}
