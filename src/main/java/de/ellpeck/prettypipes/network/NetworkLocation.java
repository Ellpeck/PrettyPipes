package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

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

    public List<Integer> getStackSlots(ItemStack stack, ItemEqualityType... equalityTypes) {
        if (this.isEmpty())
            return Collections.emptyList();
        return this.items.entrySet().stream()
                .filter(e -> ItemEqualityType.compareItems(e.getValue(), stack, equalityTypes))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int getItemAmount(ItemStack stack, ItemEqualityType... equalityTypes) {
        return this.items.values().stream()
                .filter(i -> ItemEqualityType.compareItems(stack, i, equalityTypes))
                .mapToInt(ItemStack::getCount).sum();
    }

    public Collection<ItemStack> getItems() {
        return this.items.values();
    }

    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    @Override
    public String toString() {
        return this.items.values().toString();
    }
}
