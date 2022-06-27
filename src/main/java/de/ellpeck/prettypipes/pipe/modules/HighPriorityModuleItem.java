package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.world.item.ItemStack;

public class HighPriorityModuleItem extends ModuleItem {

    private final int priority;

    public HighPriorityModuleItem(String name, ModuleTier tier) {
        super(name);
        this.priority = tier.forTier(50, 100, 200);
    }

    @Override
    public int getPriority(ItemStack module, PipeBlockEntity tile) {
        return this.priority;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof HighPriorityModuleItem) && !(other instanceof LowPriorityModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return false;
    }
}
