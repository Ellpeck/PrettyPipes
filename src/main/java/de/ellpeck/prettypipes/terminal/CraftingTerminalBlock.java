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
    public ItemStack insertItem(World world, BlockPos pipePos, Direction direction, PipeItem item) {
        BlockPos pos = pipePos.offset(direction);
        CraftingTerminalTileEntity tile = Utility.getTileEntity(CraftingTerminalTileEntity.class, world, pos);
        if (tile != null) {
            ItemStack remain = item.stack;
            int lowestFitting = -1;
            do {
                for (int i = 0; i < tile.craftItems.getSlots(); i++) {
                    ItemStack stack = tile.getRequestedCraftItem(i);
                    if (!ItemHandlerHelper.canItemStacksStackRelaxed(stack, remain))
                        continue;
                    if (lowestFitting < 0 || stack.getCount() < tile.getRequestedCraftItem(lowestFitting).getCount())
                        lowestFitting = i;
                }
                if (lowestFitting >= 0) {
                    remain = tile.craftItems.insertItem(lowestFitting, remain, false);
                    if (remain.isEmpty())
                        return ItemStack.EMPTY;
                }
            }
            while (lowestFitting >= 0);
            return ItemHandlerHelper.insertItemStacked(tile.items, remain, false);
        }
        return item.stack;
    }

}
