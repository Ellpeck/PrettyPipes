package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

public class PressurizerContainer extends Container {
    public final PressurizerTileEntity tile;

    public PressurizerContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos) {
        super(type, id);
        this.tile = Utility.getTileEntity(PressurizerTileEntity.class, player.world, pos);

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 55 + l * 18));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 113));
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::mergeItemStack, player, slotIndex, stack -> null);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }
}
