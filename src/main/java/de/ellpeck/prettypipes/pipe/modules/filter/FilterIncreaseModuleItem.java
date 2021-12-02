package de.ellpeck.prettypipes.pipe.modules.filter;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;

public class FilterIncreaseModuleItem extends ModuleItem {

    public FilterIncreaseModuleItem(String name) {
        super(name);
        this.setRegistryName(name);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return true;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new FilterIncreaseModuleContainer(Registry.filterIncreaseModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    @Override
    public ItemFilter getItemFilter(ItemStack module, PipeBlockEntity tile) {
        ItemFilter filter = new ItemFilter(18, module, tile);
        filter.canModifyWhitelist = false;
        return filter;
    }
}
