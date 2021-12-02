/*
package de.ellpeck.prettypipes.compat.jei;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<CraftingTerminalContainer> {
    @Override
    public Class<CraftingTerminalContainer> getContainerClass() {
        return CraftingTerminalContainer.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(CraftingTerminalContainer container, IRecipeLayout recipeLayout, PlayerEntity player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        ListMultimap<Integer, ItemStack> stacks = ArrayListMultimap.create();
        Map<Integer, ? extends IGuiIngredient<ItemStack>> ings = recipeLayout.getItemStacks().getGuiIngredients();
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : ings.entrySet()) {
            if (entry.getValue().isInput())
                stacks.putAll(entry.getKey() - 1, entry.getValue().getAllIngredients());
        }
        PacketHandler.sendToServer(new PacketGhostSlot(container.getTile().getPos(), stacks));
        return null;
    }
}
*/
