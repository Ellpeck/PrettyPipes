package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public interface IModule {

    void tick(ItemStack module, PipeTileEntity tile);

    boolean canNetworkSee(ItemStack module, PipeTileEntity tile);

    boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack);

    int getMaxInsertionAmount(ItemStack module, PipeTileEntity tile, ItemStack stack, IItemHandler destination);

    int getPriority(ItemStack module, PipeTileEntity tile);

    boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other);

    boolean hasContainer(ItemStack module, PipeTileEntity tile);

    AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex);

    float getItemSpeedIncrease(ItemStack module, PipeTileEntity tile);

    boolean canPipeWork(ItemStack module, PipeTileEntity tile);

    List<ItemStack> getCraftables(ItemStack module, PipeTileEntity tile);
}
