package de.ellpeck.prettypipes.pipe.containers;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class MainPipeContainer extends AbstractPipeContainer<IModule> {
    public MainPipeContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos) {
        super(type, id, player, pos, -1);
    }

    @Override
    protected void addSlots() {
        for (int i = 0; i < 3; i++)
            this.addSlot(new SlotItemHandler(this.tile.modules, i, 62 + i * 18, 17 + 32));
    }
}
