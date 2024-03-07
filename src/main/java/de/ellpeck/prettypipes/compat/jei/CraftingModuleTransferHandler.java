package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.packets.PacketCraftingModuleTransfer;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class CraftingModuleTransferHandler implements IRecipeTransferHandler<CraftingModuleContainer, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<CraftingModuleContainer> getContainerClass() {
        return CraftingModuleContainer.class;
    }

    @Override
    public Optional<MenuType<CraftingModuleContainer>> getMenuType() {
        return Optional.of(Registry.craftingModuleContainer);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CraftingModuleContainer container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        var inputs = new ArrayList<ItemStack>();
        var outputs = new ArrayList<ItemStack>();
        for (var entry : slots.getSlotViews()) {
            var allIngredients = entry.getIngredients(VanillaTypes.ITEM_STACK).toList();
            if (allIngredients.isEmpty())
                continue;
            var remain = allIngredients.get(0).copy();
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
        PacketDistributor.SERVER.noArg().send(new PacketCraftingModuleTransfer(inputs, outputs));
        return null;
    }

}
