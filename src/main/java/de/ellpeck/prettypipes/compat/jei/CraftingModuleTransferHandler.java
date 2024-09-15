package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.packets.PacketCraftingModuleTransfer;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class CraftingModuleTransferHandler implements IUniversalRecipeTransferHandler<CraftingModuleContainer> {

    @Override
    public Class<CraftingModuleContainer> getContainerClass() {
        return CraftingModuleContainer.class;
    }

    @Override
    public Optional<MenuType<CraftingModuleContainer>> getMenuType() {
        return Optional.of(Registry.craftingModuleContainer);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CraftingModuleContainer container, Object recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        var inputs = new ArrayList<ItemStack>();
        var outputs = new ArrayList<ItemStack>();
        for (var entry : recipeSlots.getSlotViews()) {
            var allIngredients = entry.getIngredients(VanillaTypes.ITEM_STACK).toList();
            if (allIngredients.isEmpty())
                continue;
            var remain = allIngredients.getFirst().copy();
            var toAdd = entry.getRole() == RecipeIngredientRole.INPUT ? inputs : outputs;
            for (var stack : toAdd) {
                if (ItemEquality.compareItems(stack, remain)) {
                    var fits = Math.min(stack.getMaxStackSize() - stack.getCount(), remain.getCount());
                    stack.grow(fits);
                    remain.shrink(fits);
                    if (remain.isEmpty())
                        break;
                }
            }
            if (!remain.isEmpty())
                toAdd.add(remain);
        }
        PacketDistributor.sendToServer(new PacketCraftingModuleTransfer(inputs, outputs));
        return null;
    }

}
