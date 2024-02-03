package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ModuleClearingRecipe extends CustomRecipe {

    public static final RecipeSerializer<ModuleClearingRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(ModuleClearingRecipe::new);

    public ModuleClearingRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        var foundModule = false;
        for (var stack : container.getItems()) {
            if (!foundModule && stack.getItem() instanceof IModule) {
                foundModule = true;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return foundModule;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        var module = container.getItems().stream().filter(i -> i.getItem() instanceof IModule).findFirst().orElse(ItemStack.EMPTY);
        if (!module.isEmpty())
            module = new ItemStack(module.getItem());
        return module;
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
