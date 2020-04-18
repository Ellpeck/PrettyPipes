package de.ellpeck.prettypipes.pipe.modules.stacksize;

import de.ellpeck.prettypipes.pipe.modules.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class StackSizeModuleContainer extends AbstractPipeContainer<StackSizeModuleItem> {
    public StackSizeModuleContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {

    }
}
