package de.ellpeck.prettypipes.pipe.modules.extraction;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class ExtractionModuleGui extends AbstractPipeGui<ExtractionModuleContainer> {
    public ExtractionModuleGui(ExtractionModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        for (Widget widget : this.container.filter.getButtons(this, this.guiLeft + 7, this.guiTop + 17 + 32 + 20))
            this.addButton(widget);
    }
}
