package de.ellpeck.prettypipes.pipe.modules.extraction;

import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;

public class ExtractionModuleContainer extends AbstractPipeContainer<ExtractionModuleItem> implements IFilteredContainer {

    public ItemFilter filter;

    public ExtractionModuleContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.filter = this.module.getItemFilter(this.moduleStack, this.tile);
        for (var slot : this.filter.getSlots((176 - this.module.filterSlots * 18) / 2 + 1, 17 + 32))
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
