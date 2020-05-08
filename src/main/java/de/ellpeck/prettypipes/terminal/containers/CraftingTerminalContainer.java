package de.ellpeck.prettypipes.terminal.containers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class CraftingTerminalContainer extends ItemTerminalContainer {
    public CraftingTerminalContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos) {
        super(type, id, player, pos);
    }

    @Override
    protected int getSlotXOffset() {
        return 65;
    }
}
