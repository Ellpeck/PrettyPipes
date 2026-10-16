package de.ellpeck.prettypipes.misc;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.function.Function;

public enum ItemOrder {
    AMOUNT(_ -> Comparator.comparingInt(ItemStack::getCount)),
    NAME(_ -> Comparator.comparing((ItemStack s) -> s.getHoverName().getString())),
    MOD(provider -> Comparator.comparing((ItemStack s) -> s.getItem().getCreatorModId(provider, s)));

    public final Function<HolderLookup.Provider, Comparator<ItemStack>> makeComparator;

    ItemOrder(Function<HolderLookup.Provider, Comparator<ItemStack>> makeComparator) {
        this.makeComparator = makeComparator;
    }

    public ItemOrder next() {
        return ItemOrder.values()[(this.ordinal() + 1) % ItemOrder.values().length];
    }
}
