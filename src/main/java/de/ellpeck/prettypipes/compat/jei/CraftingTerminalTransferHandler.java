package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<CraftingTerminalContainer, CraftingRecipe> {

    @Override
    public Class<CraftingTerminalContainer> getContainerClass() {
        return CraftingTerminalContainer.class;
    }

    @Override
    public Class<CraftingRecipe> getRecipeClass() {
        return CraftingRecipe.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(CraftingTerminalContainer container, CraftingRecipe recipe, IRecipeLayout recipeLayout, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        Map<Integer, PacketGhostSlot.Entry> stacks = new HashMap<>();
        var ingredients = recipeLayout.getItemStacks().getGuiIngredients();
        for (var entry : ingredients.entrySet()) {
            if (entry.getValue().isInput())
                stacks.put(entry.getKey() - 1, new PacketGhostSlot.Entry(entry.getValue().getAllIngredients()));
        }
        PacketHandler.sendToServer(new PacketGhostSlot(container.getTile().getBlockPos(), stacks));
        return null;
    }
}
