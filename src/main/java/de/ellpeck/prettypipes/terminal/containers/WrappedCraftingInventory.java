package de.ellpeck.prettypipes.terminal.containers;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class WrappedCraftingInventory extends CraftingContainer {

    private final ItemStackHandler items;
    private final CraftingTerminalContainer eventHandler;

    public WrappedCraftingInventory(ItemStackHandler items, CraftingTerminalContainer eventHandlerIn, int width, int height) {
        super(eventHandlerIn, width, height);
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
            for (var player : this.eventHandler.getTile().getLookingPlayers())
                player.containerMenu.slotsChanged(this);
        }
        return ret;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.items.setStackInSlot(index, stack);
        for (var player : this.eventHandler.getTile().getLookingPlayers())
            player.containerMenu.slotsChanged(this);
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
}
