package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.packets.PacketCraftingModuleTransfer;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CraftingModuleTransferHandler implements IRecipeTransferHandler<CraftingModuleContainer> {
    @Override
    public Class<CraftingModuleContainer> getContainerClass() {
        return CraftingModuleContainer.class;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingModuleContainer container, IRecipeLayout recipeLayout, PlayerEntity player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        Map<Integer, ? extends IGuiIngredient<ItemStack>> ings = recipeLayout.getItemStacks().getGuiIngredients();
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : ings.entrySet()) {
            List<ItemStack> allIngredients = entry.getValue().getAllIngredients();
            if (allIngredients.isEmpty())
                continue;
            ItemStack remain = allIngredients.get(0).copy();
            List<ItemStack> toAdd = entry.getValue().isInput() ? inputs : outputs;
            for (ItemStack stack : toAdd) {
                if (ItemEqualityType.compareItems(stack, remain)) {
                    int fits = Math.min(stack.getMaxStackSize() - stack.getCount(), remain.getCount());
                    stack.grow(fits);
                    remain.shrink(fits);
                    if (remain.isEmpty())
                        break;
                }
            }
            if (!remain.isEmpty())
                toAdd.add(remain);
        }
        PacketHandler.sendToServer(new PacketCraftingModuleTransfer(inputs, outputs));
        return null;
    }
}
