package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.function.Consumer;

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

    List<ItemStack> getAllCraftables(ItemStack module, PipeTileEntity tile);

    int getCraftableAmount(ItemStack module, PipeTileEntity tile, Consumer<ItemStack> unavailableConsumer, ItemStack stack);

    ItemStack craft(ItemStack module, PipeTileEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack);

    Integer getCustomNextNode(ItemStack module, PipeTileEntity tile, List<BlockPos> nodes, int index);
}
