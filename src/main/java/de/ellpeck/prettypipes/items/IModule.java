package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;

public interface IModule {

    void tick(PipeTileEntity tile);

    boolean canAcceptItem(PipeTileEntity tile, ItemStack stack);

    boolean isAvailableDestination(PipeTileEntity tile, ItemStack stack, IItemHandler destination);

    int getPriority(PipeTileEntity tile);
}
