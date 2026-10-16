package de.ellpeck.prettypipes.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public interface IPipeConnectable {

    ConnectionType getConnectionType(BlockPos pipePos, Direction direction);

    default int insertItem(BlockPos pipePos, Direction direction, ItemResource resource, int amount, boolean simulate) {
        return stack;
    }

    default boolean allowsModules(BlockPos pipePos, Direction direction) {
        return false;
    }
}
