package de.ellpeck.prettypipes.pipe;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public interface IPipeConnectable {

    ConnectionType getConnectionType(BlockPos pipePos, Direction direction);

    default ItemStack insertItem(BlockPos pipePos, Direction direction, ItemStack stack, boolean simulate) {
        return stack;
    }

    default boolean allowsModules(BlockPos pipePos, Direction direction) {
        return false;
    }
}
