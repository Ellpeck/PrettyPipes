package de.ellpeck.prettypipes.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ItemEquality {

    public static final ItemEquality DAMAGE = new ItemEquality((stack, filter) -> stack.getDamage() == filter.getDamage(), false, Type.DAMAGE);
    public static final ItemEquality NBT = new ItemEquality(ItemStack::areItemStackTagsEqual, false, Type.NBT);
    public static final ItemEquality MOD = new ItemEquality((stack, filter) -> stack.getItem().getCreatorModId(stack).equals(filter.getItem().getCreatorModId(filter)), true, Type.MOD);

    public final Type type;
    private final BiFunction<ItemStack, ItemStack, Boolean> filter;
    private final boolean ignoreItemEquality;

    ItemEquality(BiFunction<ItemStack, ItemStack, Boolean> filter, boolean ignoreItemEquality, Type type) {
        this.filter = filter;
        this.ignoreItemEquality = ignoreItemEquality;
        this.type = type;
    }

    public static ItemEquality tag(ResourceLocation tag) {
        return new ItemEquality((stack, filter) -> stack.getItem().getTags().contains(tag), true, Type.TAG);
    }

    public static boolean compareItems(ItemStack stack, ItemStack filter, ItemEquality... types) {
        boolean equal = ItemStack.areItemsEqual(stack, filter);
        if (types.length <= 0)
            return equal;
        for (ItemEquality type : types) {
            if (!type.ignoreItemEquality && !equal)
                return false;
            if (!type.filter.apply(stack, filter))
                return false;
        }
        return true;
    }

    public enum Type {
        DAMAGE(() -> ItemEquality.DAMAGE),
        NBT(() -> ItemEquality.NBT),
        MOD(() -> ItemEquality.MOD),
        TAG(null);

        private final Supplier<ItemEquality> defaultInstance;

        Type(Supplier<ItemEquality> defaultInstance) {
            this.defaultInstance = defaultInstance;
        }

        public ItemEquality getDefaultInstance() {
            return this.defaultInstance.get();
        }
    }
}
