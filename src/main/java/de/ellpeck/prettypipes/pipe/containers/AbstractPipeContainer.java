package de.ellpeck.prettypipes.pipe.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

public abstract class AbstractPipeContainer<T extends IModule> extends Container {

    public final PipeTileEntity tile;
    public final T module;
    public final int moduleIndex;
    public final ItemStack moduleStack;

    public AbstractPipeContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id);
        this.tile = Utility.getBlockEntity(PipeTileEntity.class, player.world, pos);
        this.moduleStack = moduleIndex < 0 ? null : this.tile.modules.getStackInSlot(moduleIndex);
        this.module = moduleIndex < 0 ? null : (T) this.moduleStack.getItem();
        this.moduleIndex = moduleIndex;

        // needs to be done here so transferStackInSlot works correctly, bleh
        this.addSlots();

        for (int l = 0; l < 3; ++l)
            for (int j1 = 0; j1 < 9; ++j1)
                this.addSlot(new Slot(player.inventory, j1 + l * 9 + 9, 8 + j1 * 18, 89 + l * 18 + 32));
        for (int i1 = 0; i1 < 9; ++i1)
            this.addSlot(new Slot(player.inventory, i1, 8 + i1 * 18, 147 + 32));
    }

    protected abstract void addSlots();

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::mergeItemStack, player, slotIndex, stack -> {
            if (stack.getItem() instanceof IModule)
                return Pair.of(0, 3);
            return null;
        });
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
        if (FilterSlot.checkFilter(this, slotId, player))
            return ItemStack.EMPTY;
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }
}
