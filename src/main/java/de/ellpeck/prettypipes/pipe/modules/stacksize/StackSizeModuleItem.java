package de.ellpeck.prettypipes.pipe.modules.stacksize;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class StackSizeModuleItem extends ModuleItem {

    public StackSizeModuleItem(String name) {
        super(name);
        this.setRegistryName(name);
    }

    public static int getMaxStackSize(ItemStack module) {
        if (module.hasTag()) {
            int amount = module.getTag().getInt("max_stack_size");
            if (amount > 0)
                return amount;
        }
        return 64;
    }

    public static void setMaxStackSize(ItemStack module, int amount) {
        module.getOrCreateTag().putInt("max_stack_size", amount);
    }

    public static boolean getLimitToMaxStackSize(ItemStack module) {
        if (module.hasTag())
            return module.getTag().getBoolean("limit_to_max_stack_size");
        return false;
    }

    public static void setLimitToMaxStackSize(ItemStack module, boolean yes) {
        module.getOrCreateTag().putBoolean("limit_to_max_stack_size", yes);
    }

    @Override
    public int getMaxInsertionAmount(ItemStack module, PipeTileEntity tile, ItemStack stack, IItemHandler destination) {
        int max = getMaxStackSize(module);
        if (getLimitToMaxStackSize(module))
            max = Math.min(max, stack.getMaxStackSize());
        int amount = 0;
        for (int i = 0; i < destination.getSlots(); i++) {
            ItemStack stored = destination.getStackInSlot(i);
            if (stored.isEmpty())
                continue;
            if (!ItemEquality.compareItems(stored, stack))
                continue;
            amount += stored.getCount();
            if (amount >= max)
                return 0;
        }
        return max - amount;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return !(other instanceof StackSizeModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new StackSizeModuleContainer(Registry.stackSizeModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }
}
