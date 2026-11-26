package de.ellpeck.prettypipes.misc;

import com.mojang.serialization.MapCodec;
import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ModuleClearingRecipe extends CustomRecipe {

    public static final ModuleClearingRecipe INSTANCE = new ModuleClearingRecipe();
    public static final MapCodec<ModuleClearingRecipe> MAP_CODEC = MapCodec.unit(ModuleClearingRecipe.INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleClearingRecipe> STREAM_CODEC = StreamCodec.unit(ModuleClearingRecipe.INSTANCE);
    public static final RecipeSerializer<ModuleClearingRecipe> SERIALIZER = new RecipeSerializer<>(ModuleClearingRecipe.MAP_CODEC, ModuleClearingRecipe.STREAM_CODEC);

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
    public ItemStack assemble(CraftingInput container) {
        for (var i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (stack.getItem() instanceof IModule)
                return new ItemStack(stack.getItem());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModuleClearingRecipe.SERIALIZER;
    }

}
