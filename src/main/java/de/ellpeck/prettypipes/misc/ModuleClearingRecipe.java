package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ModuleClearingRecipe extends CustomRecipe {

    public static final RecipeSerializer<ModuleClearingRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(ModuleClearingRecipe::new);

    public ModuleClearingRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        var foundModule = false;
        for (var i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (!foundModule && stack.getItem() instanceof IModule) {
                foundModule = true;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return foundModule;
    }

    @Override
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider pRegistries) {
        for (var i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (stack.getItem() instanceof IModule)
                return new ItemStack(stack.getItem());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 1 && y >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModuleClearingRecipe.SERIALIZER;
    }

}
