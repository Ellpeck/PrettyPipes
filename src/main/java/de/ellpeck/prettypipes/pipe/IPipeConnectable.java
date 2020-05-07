package de.ellpeck.prettypipes.pipe;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public interface IPipeConnectable {

    ConnectionType getConnectionType(World world, BlockPos pos, BlockState state, BlockPos pipePos, Direction direction);

    default IItemHandler getItemHandler(World world, BlockPos pos, BlockState state, BlockPos pipePos, Direction direction, boolean force) {
        return null;
    }
}
