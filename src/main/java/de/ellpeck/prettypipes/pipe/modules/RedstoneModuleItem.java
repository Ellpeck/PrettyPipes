package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.item.ItemStack;

public class RedstoneModuleItem extends ModuleItem {

    public RedstoneModuleItem(String name) {
        super(name);
        this.setRegistryName(name);
    }

    @Override
    public boolean canPipeWork(ItemStack module, PipeTileEntity tile) {
        return !tile.getWorld().isBlockPowered(tile.getPos());
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return other != this;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return false;
    }
}
