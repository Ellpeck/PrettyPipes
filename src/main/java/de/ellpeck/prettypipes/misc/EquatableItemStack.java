package de.ellpeck.prettypipes.misc;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class EquatableItemStack {

    public final ItemStack stack;
    public final ItemEquality[] equalityTypes;

    public EquatableItemStack(ItemStack stack, ItemEquality... equalityTypes) {
        this.stack = stack;
        this.equalityTypes = equalityTypes;
    }

    public boolean equals(Object o) {
        if (o instanceof EquatableItemStack) {
            EquatableItemStack other = (EquatableItemStack) o;
            return Arrays.equals(this.equalityTypes, other.equalityTypes) && ItemEquality.compareItems(this.stack, other.stack, this.equalityTypes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.getItem(), this.stack.getTag());
    }
}
