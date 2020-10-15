package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nullable;

public class CraftingTerminalBlock extends ItemTerminalBlock {

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new CraftingTerminalTileEntity();
    }

    @Override
    public ItemStack insertItem(World world, BlockPos pipePos, Direction direction, ItemStack remain, boolean simulate) {
        BlockPos pos = pipePos.offset(direction);
        CraftingTerminalTileEntity tile = Utility.getTileEntity(CraftingTerminalTileEntity.class, world, pos);
        if (tile != null) {
            remain = remain.copy();
            int lowestSlot = -1;
            do {
                for (int i = 0; i < tile.craftItems.getSlots(); i++) {
                    ItemStack stack = tile.getRequestedCraftItem(i);
                    int count = tile.isGhostItem(i) ? 0 : stack.getCount();
                    if (!ItemHandlerHelper.canItemStacksStackRelaxed(stack, remain))
                        continue;
                    if (lowestSlot < 0 || !tile.isGhostItem(lowestSlot) && count < tile.getRequestedCraftItem(lowestSlot).getCount())
                        lowestSlot = i;
                }
                if (lowestSlot >= 0) {
                    ItemStack copy = remain.copy();
                    copy.setCount(1);
                    remain.shrink(1 - tile.craftItems.insertItem(lowestSlot, copy, simulate).getCount());
                    if (remain.isEmpty())
                        return ItemStack.EMPTY;
                }
            }
            while (lowestSlot >= 0);
            return ItemHandlerHelper.insertItemStacked(tile.items, remain, simulate);
        }
        return remain;
    }

}
