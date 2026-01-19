package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.packets.PacketFilterSlot;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class JEIFilterGhostIngredients implements IGhostIngredientHandler<AbstractPipeGui> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(AbstractPipeGui gui, ITypedIngredient<I> ingredient, boolean doStart) {
        var targetList = new ArrayList<Target<I>>();
        if(gui.getMenu() instanceof ItemFilter.IFilteredContainer container) {

            for (Slot slot : gui.getMenu().slots) {
                if (slot instanceof FilterSlot) {
                    targetList.add((Target<I>) new GhostTarget(gui, slot));
                }
            }

        }
        return targetList;
    }

    @Override
    public void onComplete() {

    }

    public static class GhostTarget implements IGhostIngredientHandler.Target<ItemStack> {
        private final Slot slot;
        private final AbstractPipeGui gui;
        public GhostTarget(AbstractPipeGui gui,Slot slot) {
            this.slot = slot;
            this.gui = gui;
        }
        @Override
        public Rect2i getArea() {
            return new Rect2i(gui.getGuiLeft()+slot.x, gui.getGuiTop()+slot.y, 16, 16);
        }

        @Override
        public void accept(ItemStack ingredient) {
            if (ingredient.isEmpty() || !(slot instanceof FilterSlot)) return;
            PacketDistributor.sendToServer(new PacketFilterSlot(slot.index, ingredient));
        }
    }
}