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
    public final ListMultimap<Direction, Pair<Integer, ItemStack>> items;

    public NetworkLocation(BlockPos pipePos, ListMultimap<Direction, Pair<Integer, ItemStack>> items) {
        this.pipePos = pipePos;
        this.items = items;
    }

    public Pair<Direction, Integer> getStackLocation(ItemStack stack) {
        for (Map.Entry<Direction, Pair<Integer, ItemStack>> entry : this.items.entries()) {
            if (entry.getValue().getRight().isItemEqual(stack))
                return Pair.of(entry.getKey(), entry.getValue().getLeft());
        }
        return null;
    }
}
