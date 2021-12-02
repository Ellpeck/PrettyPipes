package de.ellpeck.prettypipes.pipe.modules;

import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.world.item.ItemStack;

public class SpeedModuleItem extends ModuleItem {
    private final float speedIncrease;

    public SpeedModuleItem(String name, ModuleTier tier) {
        super(name);
        this.speedIncrease = tier.forTier(0.05F, 0.1F, 0.2F);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof SpeedModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return false;
    }

    @Override
    public float getItemSpeedIncrease(ItemStack module, PipeBlockEntity tile) {
        return this.speedIncrease;
    }
}
