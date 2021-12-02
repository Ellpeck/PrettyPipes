package de.ellpeck.prettypipes.pipe.modules.filter;

import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public class FilterIncreaseModuleContainer extends AbstractPipeContainer<FilterIncreaseModuleItem> implements ItemFilter.IFilteredContainer {

    public ItemFilter filter;

    public FilterIncreaseModuleContainer(MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.filter = this.module.getItemFilter(this.moduleStack, this.tile);
        for (Slot slot : this.filter.getSlots(8, 49))
            this.addSlot(slot);
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        this.filter.save();
    }

    @Override
    public ItemFilter getFilter() {
        return this.filter;
    }
}
