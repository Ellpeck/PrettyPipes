package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.world.item.ItemStack;

public class RedstoneModuleItem extends ModuleItem {

    public RedstoneModuleItem() {
        super("redstone_module");
    }

    @Override
    public boolean canPipeWork(ItemStack module, PipeBlockEntity tile) {
        return !tile.getLevel().hasNeighborSignal(tile.getBlockPos());
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return other != this;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return false;
    }
}
