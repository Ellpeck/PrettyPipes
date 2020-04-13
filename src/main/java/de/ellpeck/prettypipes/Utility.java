package de.ellpeck.prettypipes;

import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public final class Utility {

    public static <T extends TileEntity> T getTileEntity(Class<T> type, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return type.isInstance(tile) ? (T) tile : null;
    }

    public static void dropInventory(TileEntity tile, IItemHandler inventory) {
        BlockPos pos = tile.getPos();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty())
                InventoryHelper.spawnItemStack(tile.getWorld(), pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }
}
