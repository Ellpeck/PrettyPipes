package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.blocks.pipe.PipeContainer;
import de.ellpeck.prettypipes.blocks.pipe.PipeGui;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.Range;

import java.util.Collections;
import java.util.List;

public interface IModule {

    void tick(PipeTileEntity tile);

    boolean canAcceptItem(PipeTileEntity tile, ItemStack stack);

    boolean isAvailableDestination(PipeTileEntity tile, ItemStack stack, IItemHandler destination);

    int getPriority(PipeTileEntity tile);

    boolean hasContainerTab(PipeTileEntity tile, PipeContainer container);

    boolean isCompatible(PipeTileEntity tile, IModule other);

    default List<Slot> getContainerSlots(PipeTileEntity tile, PipeContainer container) {
        return Collections.emptyList();
    }

    default Range<Integer> getShiftClickSlots(PipeTileEntity tile, PipeContainer container, ItemStack newStack) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    default void drawContainerGuiBackground(PipeTileEntity tile, PipeContainer container, PipeGui gui, int mouseX, int mouseY) {
    }

    @OnlyIn(Dist.CLIENT)
    default void drawContainerGuiForeground(PipeTileEntity tile, PipeContainer container, PipeGui gui, int mouseX, int mouseY) {
    }
}
