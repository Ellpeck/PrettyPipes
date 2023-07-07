package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

public class ItemTerminalContainer extends AbstractContainerMenu {

    public final ItemTerminalBlockEntity tile;

    public ItemTerminalContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id);
        this.tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), pos);

        this.addOwnSlots(player);

        var off = this.getSlotXOffset();
        for (var l = 0; l < 3; ++l)
            for (var j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.getInventory(), j1 + l * 9 + 9, 8 + off + j1 * 18, 154 + l * 18));
        for (var i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.getInventory(), i1, 8 + off + i1 * 18, 212));
    }

    protected void addOwnSlots(Player player) {
        var off = this.getSlotXOffset();
        for (var i = 0; i < 6; i++)
            this.addSlot(new SlotItemHandler(this.tile.items, i, 8 + off + i % 3 * 18, 102 + i / 3 * 18));
        for (var i = 0; i < 6; i++)
            this.addSlot(new SlotItemHandler(this.tile.items, i + 6, 116 + off + i % 3 * 18, 102 + i / 3 * 18));
    }

    protected int getSlotXOffset() {
        return 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::moveItemStackTo, player, slotIndex, stack -> Pair.of(6, 12));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
