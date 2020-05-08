package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketNetworkItems;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ItemTerminalContainer extends Container {

    public final ItemTerminalTileEntity tile;

    public ItemTerminalContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos) {
        super(type, id);
        this.tile = Utility.getTileEntity(ItemTerminalTileEntity.class, player.world, pos);

        int off = this.getSlotXOffset();
        for (int i = 0; i < 6; i++)
            this.addSlot(new SlotItemHandler(this.tile.items, i, 8 + off + i % 3 * 18, 102 + i / 3 * 18));
        for (int i = 0; i < 6; i++)
            this.addSlot(new SlotItemHandler(this.tile.items, i + 6, 116 + off + i % 3 * 18, 102 + i / 3 * 18));

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + off + j1 * 18, 154 + l * 18));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + off + i1 * 18, 212));
    }

    protected int getSlotXOffset() {
        return 0;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::mergeItemStack, player, slotIndex, stack -> {
            return Pair.of(6, 12);
        });
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }
}
