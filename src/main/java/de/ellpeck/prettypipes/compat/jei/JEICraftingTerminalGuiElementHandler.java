package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JEICraftingTerminalGuiElementHandler implements IGuiContainerHandler<ItemTerminalGui> {
    @Override
    public List<Rect2i> getGuiExtraAreas(ItemTerminalGui containerScreen) {
        List<Rect2i> ret = new ArrayList<>();
        // sorting buttons
        ret.add(new Rect2i(containerScreen.getGuiLeft() - 22, containerScreen.getGuiTop(), 22, 64));
        // crafting hud
        if (containerScreen.currentlyCrafting != null && !containerScreen.currentlyCrafting.isEmpty())
            ret.add(new Rect2i(containerScreen.getGuiLeft() + containerScreen.getXSize(), containerScreen.getGuiTop() + 4, 65, 89));
        return ret;
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, ItemTerminalGui containerScreen, double mouseX, double mouseY) {
        var child = containerScreen.getChildAt(mouseX, mouseY);
        var curSlot = containerScreen.getSlotUnderMouse();
        // Exposes terminal widget or ghost crafting ingredient
        if (child.isPresent()) {
            var childSlot = child.get();
            if (childSlot instanceof ItemTerminalWidget widget) {
                return builder.createBuilder(widget.stack).buildWithArea(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
            }
        } else if (curSlot != null && curSlot.index > 0 && curSlot.index <= 9) {
            // Exposes ghost crafting ingredient when present
            if (containerScreen.getMenu().tile instanceof CraftingTerminalBlockEntity craftingTerminal) {
                if (craftingTerminal.isGhostItem(curSlot.index - 1)) {
                    var stack = craftingTerminal.ghostItems.getStackInSlot(curSlot.index - 1);
                    return builder.createBuilder(stack).buildWithArea(curSlot.x, curSlot.y, 18, 18);
                }
            }
        }
        return Optional.empty();
    }
}
