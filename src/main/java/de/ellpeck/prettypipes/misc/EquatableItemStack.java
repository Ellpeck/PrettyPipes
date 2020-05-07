package de.ellpeck.prettypipes.misc;

import net.minecraft.item.ItemStack;

import java.util.Objects;

public class EquatableItemStack {

    public final ItemStack stack;

    public EquatableItemStack(ItemStack stack) {
        this.stack = stack;
    }

    public boolean equals(Object o) {
        if (o instanceof EquatableItemStack) {
            ItemStack other = ((EquatableItemStack) o).stack;
            return ItemStack.areItemsEqual(this.stack, other) && ItemStack.areItemStackTagsEqual(this.stack, other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.getItem(), this.stack.getTag());
    }
}
