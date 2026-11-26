package de.ellpeck.prettypipes.misc;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public record EquatableItemStack(HolderLookup.Provider provider, ItemStack stack, ItemEquality... equalityTypes) {

    public boolean equals(Object o) {
        if (o instanceof EquatableItemStack other)
            return Arrays.equals(this.equalityTypes, other.equalityTypes) && ItemEquality.compareItems(this.provider, this.stack, other.stack, this.equalityTypes);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.getItem(), this.stack.getComponents());
    }

}
