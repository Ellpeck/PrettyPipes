package de.ellpeck.prettypipes.pipe.modules.insertion;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FilterModuleItem extends ModuleItem {

    public final int filterSlots;
    private final boolean canPopulateFromInventories;

    public FilterModuleItem(String name, ModuleTier tier) {
        super(name);
        this.filterSlots = tier.forTier(5, 9, 18);
        this.canPopulateFromInventories = tier.forTier(false, false, true);
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        ItemFilter filter = this.getItemFilter(module, tile);
        return filter.isAllowed(stack);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return !(other instanceof FilterModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new FilterModuleContainer(Registry.filterModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    @Override
    public ItemFilter getItemFilter(ItemStack module, PipeTileEntity tile) {
        ItemFilter filter = new ItemFilter(this.filterSlots, module, tile);
        filter.canPopulateFromInventories = this.canPopulateFromInventories;
        return filter;
    }
}
