package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

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

    public static ItemStack transferStackInSlot(Container container, IMergeItemStack merge, PlayerEntity player, int slotIndex, Function<ItemStack, Pair<Integer, Integer>> predicate) {
        int inventoryStart = (int) container.inventorySlots.stream().filter(slot -> slot.inventory != player.inventory).count();
        int inventoryEnd = inventoryStart + 26;
        int hotbarStart = inventoryEnd + 1;
        int hotbarEnd = hotbarStart + 8;

        Slot slot = container.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack newStack = slot.getStack();
            ItemStack currentStack = newStack.copy();

            if (slotIndex >= inventoryStart) {
                // shift into this container here
                // mergeItemStack with the slots that newStack should go into
                // return an empty stack if mergeItemStack fails
                Pair<Integer, Integer> slots = predicate.apply(newStack);
                if (slots != null) {
                    if (!merge.mergeItemStack(newStack, slots.getLeft(), slots.getRight(), false))
                        return ItemStack.EMPTY;
                }
                // end custom code
                else if (slotIndex >= inventoryStart && slotIndex <= inventoryEnd) {
                    if (!merge.mergeItemStack(newStack, hotbarStart, hotbarEnd + 1, false))
                        return ItemStack.EMPTY;
                } else if (slotIndex >= inventoryEnd + 1 && slotIndex < hotbarEnd + 1 && !merge.mergeItemStack(newStack, inventoryStart, inventoryEnd + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!merge.mergeItemStack(newStack, inventoryStart, hotbarEnd + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (newStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (newStack.getCount() == currentStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, newStack);
            return currentStack;
        }
        return ItemStack.EMPTY;
    }

    public interface IMergeItemStack {
        boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
    }
}
