package de.ellpeck.prettypipes.pipe.modules.retrieval;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RetrievalModuleGui extends AbstractPipeGui<RetrievalModuleContainer> {

    public RetrievalModuleGui(RetrievalModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        for (var widget : this.menu.filter.getButtons(this, this.leftPos + 7, this.topPos + 17 + 32 + 20, false))
            this.addRenderableWidget(widget);
    }
}
