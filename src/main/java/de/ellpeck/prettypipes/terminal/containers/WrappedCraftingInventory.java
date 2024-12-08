package de.ellpeck.prettypipes.terminal.containers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.stream.IntStream;

public class WrappedCraftingInventory implements CraftingContainer {

    private final ItemStackHandler items;
    private final CraftingTerminalContainer eventHandler;

    public WrappedCraftingInventory(ItemStackHandler items, CraftingTerminalContainer eventHandlerIn) {
        this.eventHandler = eventHandlerIn;
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return this.items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (var i = 0; i < this.items.getSlots(); i++) {
            if (!this.items.getStackInSlot(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        var before = this.items.getStackInSlot(index);
        this.items.setStackInSlot(index, ItemStack.EMPTY);
        return before;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        var slotStack = this.items.getStackInSlot(index);
        var ret = !slotStack.isEmpty() && count > 0 ? slotStack.split(count) : ItemStack.EMPTY;
        if (!ret.isEmpty()) {
            this.eventHandler.slotsChanged(this);
        }
        return ret;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.items.setStackInSlot(index, stack);
        this.eventHandler.slotsChanged(this);
    }

    @Override
    public void setChanged() {
        this.eventHandler.slotsChanged(this);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (var i = 0; i < this.items.getSlots(); i++)
            this.items.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public void fillStackedContents(StackedContents helper) {
        for (var i = 0; i < this.items.getSlots(); i++)
            helper.accountStack(this.items.getStackInSlot(i));
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public List<ItemStack> getItems() {
        return IntStream.range(0, this.getContainerSize()).mapToObj(this::getItem).toList();
    }

}
