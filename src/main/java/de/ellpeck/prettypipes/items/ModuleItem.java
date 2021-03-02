package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;

public abstract class ModuleItem extends Item implements IModule {

    private final String name;

    public ModuleItem(String name) {
        super(new Properties().group(Registry.GROUP).maxStackSize(16));
        this.name = name;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        Utility.addTooltip(this.name, tooltip);
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {

    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxInsertionAmount(ItemStack module, PipeTileEntity tile, ItemStack stack, IItemHandler destination) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPriority(ItemStack module, PipeTileEntity tile) {
        return 0;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return null;
    }

    @Override
    public float getItemSpeedIncrease(ItemStack module, PipeTileEntity tile) {
        return 0;
    }

    @Override
    public boolean canPipeWork(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public List<ItemStack> getAllCraftables(ItemStack module, PipeTileEntity tile) {
        return Collections.emptyList();
    }

    @Override
    public int getCraftableAmount(ItemStack module, PipeTileEntity tile, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        return 0;
    }

    @Override
    public ItemStack craft(ItemStack module, PipeTileEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        return stack;
    }

    @Override
    public Integer getCustomNextNode(ItemStack module, PipeTileEntity tile, List<BlockPos> nodes, int index) {
        return null;
    }

    @Override
    public ItemFilter getItemFilter(ItemStack module, PipeTileEntity tile) {
        return null;
    }
}
