package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.Utility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class PressurizerContainer extends AbstractContainerMenu {

    public final PressurizerBlockEntity tile;

    public PressurizerContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id);
        this.tile = Utility.getBlockEntity(PressurizerBlockEntity.class, player.level, pos);

        for (var l = 0; l < 3; ++l)
            for (var j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.getInventory(), j1 + l * 9 + 9, 8 + j1 * 18, 55 + l * 18));
        for (var i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.getInventory(), i1, 8 + i1 * 18, 113));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::moveItemStackTo, player, slotIndex, stack -> null);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
