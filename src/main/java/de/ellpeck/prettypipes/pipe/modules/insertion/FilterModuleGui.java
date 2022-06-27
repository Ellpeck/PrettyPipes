package de.ellpeck.prettypipes.pipe.modules.insertion;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class FilterModuleGui extends AbstractPipeGui<FilterModuleContainer> {

    public FilterModuleGui(FilterModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        for (var widget : this.menu.filter.getButtons(this, this.leftPos + 7, this.topPos + 17 + 32 + 18 * Mth.ceil(this.menu.filter.getSlots() / 9F) + 2))
            this.addRenderableWidget(widget);
    }
}
