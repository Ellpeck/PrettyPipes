package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public abstract class ModuleItem extends Item implements IModule {
    public ModuleItem() {
        super(new Properties().group(Registry.GROUP).maxStackSize(16));
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {

    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isAvailableDestination(ItemStack module, PipeTileEntity tile, ItemStack stack, IItemHandler destination) {
        return true;
    }

    @Override
    public int getPriority(ItemStack module, PipeTileEntity tile) {
        return 0;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return null;
    }
}
