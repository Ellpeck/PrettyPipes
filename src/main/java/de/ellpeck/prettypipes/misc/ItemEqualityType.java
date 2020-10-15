package de.ellpeck.prettypipes.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Set;
import java.util.function.BiFunction;

public enum ItemEqualityType {

    DAMAGE((stack, filter) -> stack.getDamage() == filter.getDamage(), false),
    NBT(ItemStack::areItemStackTagsEqual, false),
    TAG((stack, filter) -> {
        Set<ResourceLocation> stackTags = stack.getItem().getTags();
        Set<ResourceLocation> filterTags = filter.getItem().getTags();
        if (filterTags.isEmpty())
            return false;
        return stackTags.containsAll(filterTags);
    }, true),
    MOD((stack, filter) -> stack.getItem().getCreatorModId(stack).equals(filter.getItem().getCreatorModId(filter)), true);

    public final BiFunction<ItemStack, ItemStack, Boolean> filter;
    public final boolean ignoreItemEquality;

    ItemEqualityType(BiFunction<ItemStack, ItemStack, Boolean> filter, boolean ignoreItemEquality) {
        this.filter = filter;
        this.ignoreItemEquality = ignoreItemEquality;
    }

    public static boolean compareItems(ItemStack stack, ItemStack filter, ItemEqualityType... types) {
        boolean equal = ItemStack.areItemsEqual(stack, filter);
        if (types.length <= 0)
            return equal;
        for (ItemEqualityType type : types) {
            if (!type.ignoreItemEquality && !equal)
                return false;
            if (!type.filter.apply(stack, filter))
                return false;
        }
        return true;
    }
}
