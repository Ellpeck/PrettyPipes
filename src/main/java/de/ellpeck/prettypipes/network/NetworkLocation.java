package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkLocation {

    public final BlockPos pipePos;
    public final Direction direction;
    public final BlockPos pos;
    public final IItemHandler handler;
    private Map<Integer, ItemStack> items;

    public NetworkLocation(BlockPos pipePos, Direction direction, IItemHandler handler) {
        this.pipePos = pipePos;
        this.direction = direction;
        this.pos = pipePos.offset(direction);
        this.handler = handler;
    }

    public void addItem(int slot, ItemStack stack) {
        if (this.items == null)
            this.items = new HashMap<>();
        this.items.put(slot, stack);
    }

    public int getStackSlot(ItemStack stack) {
        if (this.isEmpty())
            return -1;
        for (Map.Entry<Integer, ItemStack> entry : this.items.entrySet()) {
            if (entry.getValue().isItemEqual(stack))
                return entry.getKey();
        }
        return -1;
    }

    public int getItemAmount(ItemStack stack) {
        return this.items.values().stream()
                .filter(i -> i.isItemEqual(stack))
                .mapToInt(ItemStack::getCount).sum();
    }

    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    @Override
    public String toString() {
        return this.items.values().toString();
    }
}
