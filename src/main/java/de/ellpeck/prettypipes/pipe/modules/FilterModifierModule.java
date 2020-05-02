package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class FilterModifierModule extends ModuleItem {

    public final Type type;

    public FilterModifierModule(String name, Type type) {
        super(name);
        this.type = type;
        this.setRegistryName(name);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return other != this;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

    public enum Type {
        DAMAGE(false, (stack, filter) -> stack.getDamage() == filter.getDamage()),
        NBT(false, ItemStack::areItemStackTagsEqual),
        TAG(true, (stack, filter) -> {
            Set<ResourceLocation> stackTags = stack.getItem().getTags();
            Set<ResourceLocation> filterTags = filter.getItem().getTags();
            if (filterTags.isEmpty())
                return false;
            return stackTags.containsAll(filterTags);
        });

        public final boolean ignoreItemEquality;
        public final BiFunction<ItemStack, ItemStack, Boolean> filter;

        Type(boolean ignoreItemEquality, BiFunction<ItemStack, ItemStack, Boolean> filter) {
            this.ignoreItemEquality = ignoreItemEquality;
            this.filter = filter;
        }
    }
}
