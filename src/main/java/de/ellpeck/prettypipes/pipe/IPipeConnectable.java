package de.ellpeck.prettypipes.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface IPipeConnectable {

    ConnectionType getConnectionType(BlockPos pipePos, Direction direction);

    default ItemStack insertItem(BlockPos pipePos, Direction direction, ItemStack stack, boolean simulate) {
        return stack;
    }

    default boolean allowsModules(BlockPos pipePos, Direction direction) {
        return false;
    }
}
