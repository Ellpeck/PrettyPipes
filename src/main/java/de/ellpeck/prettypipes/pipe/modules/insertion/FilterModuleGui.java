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
        var buttonsY = this.topPos + 17 + 32 + 18 * Mth.ceil(this.menu.filter.content.getSlots() / 9F) + 2;
        for (var widget : this.menu.filter.getButtons(this, this.leftPos + this.imageWidth - 7, buttonsY, true))
            this.addRenderableWidget(widget);
        this.addRenderableWidget(this.menu.directionSelector.getButton(this.leftPos + 7, buttonsY));
    }
}
