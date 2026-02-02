package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalGui;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JEICraftingTerminalGhostIngredients implements IGhostIngredientHandler<CraftingTerminalGui> {


    @Override
    public <I> List<Target<I>> getTargetsTyped(CraftingTerminalGui gui, ITypedIngredient<I> ingredient, boolean doStart) {
        var targetList = new ArrayList<IGhostIngredientHandler.Target<I>>();
        for (var i = 0; i <= 9; i++) {
            var currentTarget = new GhostSlotTarget(i, gui);
            targetList.add((Target<I>) currentTarget);
        }
        return targetList;
    }

    @Override
    public void onComplete() {

    }

    public static class GhostSlotTarget implements IGhostIngredientHandler.Target<ItemStack> {
        private final int curSlotIndex;
        private final CraftingTerminalGui gui;
        private final Slot curSlot;

        public GhostSlotTarget(int curSlotIndex, CraftingTerminalGui gui) {
            this.curSlotIndex = curSlotIndex;
            this.gui = gui;
            this.curSlot = gui.getMenu().getSlot(curSlotIndex + 1);
        }

        @Override
        public Rect2i getArea() {
            return new Rect2i(this.gui.getGuiLeft() + this.curSlot.x, this.gui.getGuiTop() + this.curSlot.y, 16, 16);
        }

        @Override
        public void accept(ItemStack ingredient) {
            if (this.gui.getMenu().tile instanceof CraftingTerminalBlockEntity craftingTerminalBlockEntity) {
                var ghostStack = ingredient.copyWithCount(1);
                List<PacketGhostSlot.Entry> stacks = new ArrayList<>();
                for (var i = 0; i < craftingTerminalBlockEntity.craftItems.getSlots(); i++) {
                    if (i != this.curSlotIndex) {
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(craftingTerminalBlockEntity.ghostItems.getStackInSlot(i))), Optional.empty()));
                    } else {
                        stacks.add(i, new PacketGhostSlot.Entry(Optional.of(List.of(ghostStack)), Optional.empty()));
                    }
                }
                PacketDistributor.sendToServer(new PacketGhostSlot(craftingTerminalBlockEntity.getBlockPos(), stacks));
            }
        }
    }
}
