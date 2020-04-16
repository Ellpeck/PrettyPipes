package de.ellpeck.prettypipes.pipe.modules.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.SlotFilter;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public abstract class AbstractPipeContainer<T extends IModule> extends Container {

    public final PipeTileEntity tile;
    public final T module;
    public final ItemStack moduleStack;

    public AbstractPipeContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id);
        this.tile = Utility.getTileEntity(PipeTileEntity.class, player.world, pos);
        this.moduleStack = moduleIndex < 0 ? null : this.tile.modules.getStackInSlot(moduleIndex);
        this.module = moduleIndex < 0 ? null : (T) this.moduleStack.getItem();

        // needs to be done here so transferStackInSlot works correctly, bleh
        this.addSlots();

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 89 + l * 18 + 32));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 147 + 32));
    }

    protected abstract void addSlots();

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

            if (slotIndex >= inventoryStart) {
                // shift into this container here
                // mergeItemStack with the slots that newStack should go into
                // return an empty stack if mergeItemStack fails
                if (newStack.getItem() instanceof IModule) {
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

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
        if (SlotFilter.checkFilter(this, slotId, player))
            return ItemStack.EMPTY;
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }
}
