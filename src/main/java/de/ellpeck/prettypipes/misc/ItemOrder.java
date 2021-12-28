package de.ellpeck.prettypipes.misc;

import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

public enum ItemOrder {
    AMOUNT(Comparator.comparingInt(ItemStack::getCount)),
    NAME(Comparator.comparing(s -> s.getHoverName().getString())),
    MOD(Comparator.comparing(s -> s.getItem().getCreatorModId(s)));

    public final Comparator<ItemStack> comparator;

    ItemOrder(Comparator<ItemStack> comparator) {
        this.comparator = comparator;
    }

    public ItemOrder next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
