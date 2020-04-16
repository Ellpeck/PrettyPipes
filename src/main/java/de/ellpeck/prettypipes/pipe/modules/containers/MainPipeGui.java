package de.ellpeck.prettypipes.pipe.modules.containers;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class MainPipeGui extends AbstractPipeGui<MainPipeContainer> {
    public MainPipeGui(MainPipeContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }
}
