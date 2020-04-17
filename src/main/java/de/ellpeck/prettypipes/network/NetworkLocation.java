package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkLocation {

    public final BlockPos pipePos;
    private ListMultimap<Direction, Pair<Integer, ItemStack>> items;

    public NetworkLocation(BlockPos pipePos) {
        this.pipePos = pipePos;
    }

    public void addItem(Direction direction, int slot, ItemStack stack) {
        if (this.items == null)
            this.items = ArrayListMultimap.create();
        this.items.put(direction, Pair.of(slot, stack));
    }

    public Pair<Direction, Integer> getStackLocation(ItemStack stack) {
        if (this.isEmpty())
            return null;
        for (Map.Entry<Direction, Pair<Integer, ItemStack>> entry : this.items.entries()) {
            if (entry.getValue().getRight().isItemEqual(stack))
                return Pair.of(entry.getKey(), entry.getValue().getLeft());
        }
        return null;
    }

    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }
}
