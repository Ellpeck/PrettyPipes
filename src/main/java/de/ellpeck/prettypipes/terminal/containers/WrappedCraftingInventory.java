package de.ellpeck.prettypipes.terminal.containers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraftforge.items.ItemStackHandler;

public class WrappedCraftingInventory extends CraftingInventory {

    private final ItemStackHandler items;
    private final Container eventHandler;

    public WrappedCraftingInventory(ItemStackHandler items, Container eventHandlerIn, int width, int height) {
        super(eventHandlerIn, width, height);
        this.eventHandler = eventHandlerIn;
        this.items = items;
    }

    @Override
    public int getSizeInventory() {
        return this.items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.items.getSlots(); i++) {
            if (!this.items.getStackInSlot(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.items.getStackInSlot(index);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack before = this.items.getStackInSlot(index);
        this.items.setStackInSlot(index, ItemStack.EMPTY);
        return before;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack slotStack = this.items.getStackInSlot(index);
        ItemStack ret = !slotStack.isEmpty() && count > 0 ? slotStack.split(count) : ItemStack.EMPTY;
        if (!ret.isEmpty())
            this.eventHandler.onCraftMatrixChanged(this);
        return ret;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.items.setStackInSlot(index, stack);
        this.eventHandler.onCraftMatrixChanged(this);
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.items.getSlots(); i++)
            this.items.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public void fillStackedContents(RecipeItemHelper helper) {
        for (int i = 0; i < this.items.getSlots(); i++)
            helper.accountPlainStack(this.items.getStackInSlot(i));
    }
}
