package de.ellpeck.prettypipes.pipe.modules.insertion;

import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class FilterModuleContainer extends AbstractPipeContainer<FilterModuleItem> implements IFilteredContainer {

    public ItemFilter filter;

    public FilterModuleContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.filter = new ItemFilter(this.module.filterSlots, this.moduleStack, this.tile);
        this.filter.canPopulateFromInventories = this.module.canPopulateFromInventories;
        for (Slot slot : this.filter.getSlots((176 - Math.min(this.module.filterSlots, 9) * 18) / 2 + 1, 17 + 32))
            this.addSlot(slot);
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        this.filter.save();
    }

    @Override
    public ItemFilter getFilter() {
        return this.filter;
    }
}
