package de.ellpeck.prettypipes.compat.jei;

// TODO JEI
/*
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
*/
