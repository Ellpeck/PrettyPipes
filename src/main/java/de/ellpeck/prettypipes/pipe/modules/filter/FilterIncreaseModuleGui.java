package de.ellpeck.prettypipes.pipe.modules.filter;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FilterIncreaseModuleGui extends AbstractPipeGui<FilterIncreaseModuleContainer> {

    public FilterIncreaseModuleGui(FilterIncreaseModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }
}
