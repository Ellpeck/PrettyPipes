package de.ellpeck.prettypipes.pipe.insertion;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

public class FilterModuleGui extends AbstractPipeGui<FilterModuleContainer> {
    public FilterModuleGui(FilterModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        for (Widget widget : this.container.filter.getButtons(this.guiLeft + 7, this.guiTop + 17 + 32 + 18 * MathHelper.ceil(this.container.filter.getSlots() / 9F) + 2))
            this.addButton(widget);
    }
}
