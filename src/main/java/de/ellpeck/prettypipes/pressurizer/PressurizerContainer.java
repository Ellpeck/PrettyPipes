package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.Utility;
import net.minecraft.core.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class PressurizerContainer extends AbstractContainerMenu {
    public final PressurizerBlockEntity tile;

    public PressurizerContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id);
        this.tile = Utility.getBlockEntity(PressurizerBlockEntity.class, player.level, pos);

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.getInventory(), j1 + l * 9 + 9, 8 + j1 * 18, 55 + l * 18));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.getInventory(), i1, 8 + i1 * 18, 113));
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
