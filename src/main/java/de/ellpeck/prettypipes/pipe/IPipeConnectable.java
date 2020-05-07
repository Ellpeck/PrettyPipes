package de.ellpeck.prettypipes.pipe;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IPipeConnectable {

    ConnectionType getConnectionType(World world, BlockPos pos, BlockState state, BlockPos pipePos, Direction direction);

    default boolean provideEnergyStorage(World world, BlockPos pos, BlockPos pipePos, Direction direction) {
        return false;
    }
}
