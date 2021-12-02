package de.ellpeck.prettypipes.pipe.containers;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MainPipeGui extends AbstractPipeGui<MainPipeContainer> {

    public MainPipeGui(MainPipeContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }
}
