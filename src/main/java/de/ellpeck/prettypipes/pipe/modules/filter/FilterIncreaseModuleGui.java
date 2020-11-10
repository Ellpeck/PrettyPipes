package de.ellpeck.prettypipes.pipe.modules.filter;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class FilterIncreaseModuleGui extends AbstractPipeGui<FilterIncreaseModuleContainer> {
    public FilterIncreaseModuleGui(FilterIncreaseModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }
}
