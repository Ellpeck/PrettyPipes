package de.ellpeck.prettypipes.misc;

import net.minecraft.item.ItemStack;

import java.util.Comparator;

public enum ItemOrder {
    AMOUNT(Comparator.comparingInt(ItemStack::getCount)),
    NAME(Comparator.comparing(s -> s.getDisplayName().getFormattedText())),
    MOD(Comparator.comparing(s -> s.getItem().getRegistryName().getNamespace()));

    public final Comparator<ItemStack> comparator;

    ItemOrder(Comparator<ItemStack> comparator) {
        this.comparator = comparator;
    }
}
