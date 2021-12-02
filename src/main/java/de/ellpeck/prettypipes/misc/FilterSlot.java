package de.ellpeck.prettypipes.misc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class FilterSlot extends SlotItemHandler {

    private final boolean onlyOneItem;

    public FilterSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean onlyOneItem) {
        super(itemHandler, index, xPosition, yPosition);
        this.onlyOneItem = onlyOneItem;
    }

    public static boolean checkFilter(AbstractContainerMenu container, int slotId, Player player) {
        if (slotId >= 0 && slotId < container.slots.size()) {
            var slot = container.getSlot(slotId);
            if (slot instanceof FilterSlot) {
                ((FilterSlot) slot).slotClick(player);
                return true;
            }
        }
        return false;
    }

    private void slotClick(Player player) {
        var heldStack = player.inventoryMenu.getCarried();
        var stackInSlot = this.getItem();

        if (!stackInSlot.isEmpty() && heldStack.isEmpty()) {
            this.safeInsert(ItemStack.EMPTY);
        } else if (!heldStack.isEmpty()) {
            var s = heldStack.copy();
            if (this.onlyOneItem)
                s.setCount(1);
            this.safeInsert(s);
        }
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack) {
        return super.safeInsert(stack.copy());
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return false;
    }

}
