package de.ellpeck.prettypipes;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

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

    public static Direction getDirectionFromOffset(BlockPos pos, BlockPos other) {
        BlockPos diff = pos.subtract(other);
        return Direction.getFacingFromVector(diff.getX(), diff.getY(), diff.getZ());
    }

    public static void addTooltip(String name, List<ITextComponent> tooltip) {
        if (Screen.hasShiftDown()) {
            String[] content = I18n.format("info." + PrettyPipes.ID + "." + name).split("\n");
            for (String s : content)
                tooltip.add(new StringTextComponent(s).setStyle(new Style().setColor(TextFormatting.GRAY)));
        } else {
            tooltip.add(new TranslationTextComponent("info." + PrettyPipes.ID + ".shift").setStyle(new Style().setColor(TextFormatting.DARK_GRAY)));
        }
    }
}
