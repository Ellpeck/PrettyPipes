package de.ellpeck.prettypipes.misc;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Objects;
import java.util.function.Supplier;

public class ItemEquality {

    public static final ItemEquality DAMAGE = new ItemEquality((_, stack, filter) -> stack.getDamageValue() == filter.getDamageValue(), false, Type.DAMAGE);
    public static final ItemEquality NBT = new ItemEquality((_, stack, filter) -> Objects.equals(stack.getComponents(), filter.getComponents()), false, Type.NBT);
    public static final ItemEquality MOD = new ItemEquality((provider, stack, filter) -> stack.getItem().getCreatorModId(provider, stack).equals(filter.getItem().getCreatorModId(provider, filter)), true, Type.MOD);

    public final Type type;
    private final TriFunction<HolderLookup.Provider, ItemStack, ItemStack, Boolean> filter;
    private final boolean ignoreItemEquality;

    ItemEquality(TriFunction<HolderLookup.Provider, ItemStack, ItemStack, Boolean> filter, boolean ignoreItemEquality, Type type) {
        this.filter = filter;
        this.ignoreItemEquality = ignoreItemEquality;
        this.type = type;
    }

    public static ItemEquality tag(Identifier tag) {
        return new ItemEquality((_, stack, _) -> stack.tags().anyMatch(t -> Objects.equals(tag, t.location())), true, Type.TAG);
    }

    public static boolean compareItems(HolderLookup.Provider provider, ItemStack stack, ItemStack filter, ItemEquality... types) {
        var equal = ItemStack.isSameItem(stack, filter);
        if (types.length <= 0)
            return equal;
        for (var type : types) {
            if (!type.ignoreItemEquality && !equal)
                return false;
            if (!type.filter.apply(provider, stack, filter))
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
