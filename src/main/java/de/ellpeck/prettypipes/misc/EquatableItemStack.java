package de.ellpeck.prettypipes.misc;

import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class EquatableItemStack {

    public final ItemStack stack;
    public final ItemEqualityType[] equalityTypes;

    public EquatableItemStack(ItemStack stack, ItemEqualityType... equalityTypes) {
        this.stack = stack;
        this.equalityTypes = equalityTypes;
    }

    public boolean equals(Object o) {
        if (o instanceof EquatableItemStack) {
            EquatableItemStack other = (EquatableItemStack) o;
            return Arrays.equals(this.equalityTypes, other.equalityTypes) && ItemEqualityType.compareItems(this.stack, other.stack, this.equalityTypes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.getItem(), this.stack.getTag());
    }
}
