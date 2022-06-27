package de.ellpeck.prettypipes.compat.jei;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<CraftingTerminalContainer, CraftingRecipe> {

    @Override
    public Class<CraftingTerminalContainer> getContainerClass() {
        return CraftingTerminalContainer.class;
    }

    @Override
    public Optional<MenuType<CraftingTerminalContainer>> getMenuType() {
        return Optional.of(Registry.craftingTerminalContainer);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CraftingTerminalContainer container, CraftingRecipe recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        List<PacketGhostSlot.Entry> stacks = new ArrayList<>();
        var ingredients = slots.getSlotViews(RecipeIngredientRole.INPUT);
        for (var entry : ingredients)
            stacks.add(new PacketGhostSlot.Entry(entry.getIngredients(VanillaTypes.ITEM_STACK).collect(Collectors.toList())));
        PacketHandler.sendToServer(new PacketGhostSlot(container.getTile().getBlockPos(), stacks));
        return null;
    }
}
