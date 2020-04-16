package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.blocks.pipe.PipeContainer;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public abstract class ModuleItem extends Item implements IModule {
    public ModuleItem() {
        super(new Properties().group(Registry.GROUP).maxStackSize(16));
    }

    @Override
    public void tick(PipeTileEntity tile) {

    }

    @Override
    public boolean canAcceptItem(PipeTileEntity tile, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isAvailableDestination(PipeTileEntity tile, ItemStack stack, IItemHandler destination) {
        return true;
    }

    @Override
    public int getPriority(PipeTileEntity tile) {
        return 0;
    }

    @Override
    public boolean hasContainerTab(PipeTileEntity tile, PipeContainer container) {
        return false;
    }
}
