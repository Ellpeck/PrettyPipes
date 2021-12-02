package de.ellpeck.prettypipes.pipe.modules.modifier;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import joptsimple.internal.Strings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class FilterModifierModuleItem extends ModuleItem {

    public final ItemEquality.Type type;

    public FilterModifierModuleItem(String name, ItemEquality.Type type) {
        super(name);
        this.type = type;
        this.setRegistryName(name);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return other != this;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return this.type == ItemEquality.Type.TAG;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new FilterModifierModuleContainer(Registry.filterModifierModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

    public ItemEquality getEqualityType(ItemStack stack) {
        if (this.type == ItemEquality.Type.TAG) {
            return ItemEquality.tag(getFilterTag(stack));
        } else {
            return this.type.getDefaultInstance();
        }
    }

    public static ResourceLocation getFilterTag(ItemStack stack) {
        if (!stack.hasTag())
            return null;
        String tag = stack.getTag().getString("filter_tag");
        if (Strings.isNullOrEmpty(tag))
            return null;
        return new ResourceLocation(tag);
    }

    public static void setFilterTag(ItemStack stack, ResourceLocation tag) {
        stack.getOrCreateTag().putString("filter_tag", tag.toString());
    }
}
