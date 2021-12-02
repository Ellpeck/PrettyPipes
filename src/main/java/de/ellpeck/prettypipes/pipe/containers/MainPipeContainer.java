package de.ellpeck.prettypipes.pipe.containers;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class MainPipeContainer extends AbstractPipeContainer<IModule> {

    public MainPipeContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id, player, pos, -1);
    }

    @Override
    protected void addSlots() {
        for (int i = 0; i < 3; i++)
            this.addSlot(new SlotItemHandler(this.tile.modules, i, 62 + i * 18, 17 + 32));
    }
}
