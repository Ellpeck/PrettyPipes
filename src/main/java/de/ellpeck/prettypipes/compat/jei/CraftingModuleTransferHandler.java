package de.ellpeck.prettypipes.compat.jei;

// TODO JEI
/*
public class CraftingModuleTransferHandler implements IRecipeTransferHandler<CraftingModuleContainer, CraftingRecipe> {

    @Override
    public Class<CraftingModuleContainer> getContainerClass() {
        return CraftingModuleContainer.class;
    }

    @Override
    public Class<CraftingRecipe> getRecipeClass() {
        return CraftingRecipe.class;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingModuleContainer container, CraftingRecipe recipe, IRecipeLayout recipeLayout, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer)
            return null;
        var ingredients = recipeLayout.getItemStacks().getGuiIngredients();
        var inputs = new ArrayList<ItemStack>();
        var outputs = new ArrayList<ItemStack>();
        for (var entry : ingredients.entrySet()) {
            var allIngredients = entry.getValue().getAllIngredients();
            if (allIngredients.isEmpty())
                continue;
            var remain = allIngredients.get(0).copy();
            var toAdd = entry.getValue().isInput() ? inputs : outputs;
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
        PacketHandler.sendToServer(new PacketCraftingModuleTransfer(inputs, outputs));
        return null;
    }
}
*/
