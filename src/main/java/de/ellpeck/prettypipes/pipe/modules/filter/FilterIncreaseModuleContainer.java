package de.ellpeck.prettypipes.pipe.modules.filter;

import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

public class FilterIncreaseModuleContainer extends AbstractPipeContainer<FilterIncreaseModuleItem> implements ItemFilter.IFilteredContainer {

    public ItemFilter filter;

    public FilterIncreaseModuleContainer(ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.filter = new ItemFilter(18, this.moduleStack, this.tile);
        this.filter.canModifyWhitelist = false;
        for (Slot slot : this.filter.getSlots(8, 49))
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
