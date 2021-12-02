package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class SortingModuleItem extends ModuleItem {

    private final Type type;

    public SortingModuleItem(String name, Type type) {
        super(name);
        this.type = type;
        this.setRegistryName(name);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return !(other instanceof SortingModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return false;
    }

    @Override
    public Integer getCustomNextNode(ItemStack module, PipeTileEntity tile, List<BlockPos> nodes, int index) {
        switch (this.type) {
            case ROUND_ROBIN:
                // store an ever-increasing index and choose destinations based on that
                int next = module.hasTag() ? module.getTag().getInt("last") + 1 : 0;
                module.getOrCreateTag().putInt("last", next);
                return next % nodes.size();
            case RANDOM:
                return tile.getWorld().rand.nextInt(nodes.size());
        }
        return null;
    }

    public enum Type {
        ROUND_ROBIN,
        RANDOM
    }
}
