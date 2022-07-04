package de.ellpeck.prettypipes.pipe.modules.insertion;

import de.ellpeck.prettypipes.misc.DirectionSelector;
import de.ellpeck.prettypipes.misc.DirectionSelector.IDirectionContainer;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;

public class FilterModuleContainer extends AbstractPipeContainer<FilterModuleItem> implements IFilteredContainer, IDirectionContainer {

    public ItemFilter filter;
    public DirectionSelector directionSelector;

    public FilterModuleContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    public void onFilterPopulated() {
        // reload the filter so that it displays correctly on the client
        this.filter.load();
    }

    @Override
    protected void addSlots() {
        this.filter = this.module.getItemFilter(this.moduleStack, this.tile);
        this.directionSelector = this.module.getDirectionSelector(this.moduleStack, this.tile);

        for (var slot : this.filter.getSlots((176 - Math.min(this.module.filterSlots, 9) * 18) / 2 + 1, 17 + 32))
            this.addSlot(slot);
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        this.filter.save();
        this.directionSelector.save();
    }

    @Override
    public ItemFilter getFilter() {
        return this.filter;
    }

    @Override
    public DirectionSelector getSelector() {
        return this.directionSelector;
    }

}
