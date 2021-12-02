package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public interface IModule {

    void tick(ItemStack module, PipeBlockEntity tile);

    boolean canNetworkSee(ItemStack module, PipeBlockEntity tile);

    boolean canAcceptItem(ItemStack module, PipeBlockEntity tile, ItemStack stack);

    int getMaxInsertionAmount(ItemStack module, PipeBlockEntity tile, ItemStack stack, IItemHandler destination);

    int getPriority(ItemStack module, PipeBlockEntity tile);

    boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other);

    boolean hasContainer(ItemStack module, PipeBlockEntity tile);

    AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex);

    float getItemSpeedIncrease(ItemStack module, PipeBlockEntity tile);

    boolean canPipeWork(ItemStack module, PipeBlockEntity tile);

    List<ItemStack> getAllCraftables(ItemStack module, PipeBlockEntity tile);

    int getCraftableAmount(ItemStack module, PipeBlockEntity tile, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain);

    ItemStack craft(ItemStack module, PipeBlockEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain);

    Integer getCustomNextNode(ItemStack module, PipeBlockEntity tile, List<BlockPos> nodes, int index);

    ItemFilter getItemFilter(ItemStack module, PipeBlockEntity tile);
}
