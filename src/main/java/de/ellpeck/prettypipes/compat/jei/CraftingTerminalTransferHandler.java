package de.ellpeck.prettypipes.compat.jei;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;

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
        ListMultimap<Integer, ItemStack> stacks = ArrayListMultimap.create();
        var ingredients = recipeLayout.getItemStacks().getGuiIngredients();
        for (var entry : ingredients.entrySet()) {
            if (entry.getValue().isInput())
                stacks.putAll(entry.getKey() - 1, entry.getValue().getAllIngredients());
        }
        PacketHandler.sendToServer(new PacketGhostSlot(container.getTile().getBlockPos(), stacks));
        return null;
    }
}
