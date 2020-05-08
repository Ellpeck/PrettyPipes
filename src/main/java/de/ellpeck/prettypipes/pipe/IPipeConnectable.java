package de.ellpeck.prettypipes.pipe;

import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public interface IPipeConnectable {

    ConnectionType getConnectionType(World world, BlockPos pipePos, Direction direction);

    default IItemHandler getItemHandler(World world, BlockPos pipePos, Direction direction, PipeItem item) {
        return null;
    }

    default ItemStack insertItem(World world, BlockPos pipePos, Direction direction, PipeItem item) {
        return item.stack;
    }
}
