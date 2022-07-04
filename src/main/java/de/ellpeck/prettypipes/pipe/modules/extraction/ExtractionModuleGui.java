package de.ellpeck.prettypipes.pipe.modules.extraction;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtractionModuleGui extends AbstractPipeGui<ExtractionModuleContainer> {

    public ExtractionModuleGui(ExtractionModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        for (var widget : this.menu.filter.getButtons(this, this.leftPos + this.imageWidth - 7, this.topPos + 17 + 32 + 20, true))
            this.addRenderableWidget(widget);
        this.addRenderableWidget(this.menu.directionSelector.getButton(this.leftPos + 7, this.topPos + 17 + 32 + 20));
    }
}
