package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.misc.EquatableItemStack;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkItem {

    private final Set<NetworkLocation> locations = new HashSet<>();
    private final EquatableItemStack item;
    private int amount;

    public NetworkItem(EquatableItemStack item) {
        this.item = item;
    }

    public void add(NetworkLocation location, ItemStack stack) {
        this.locations.add(location);
        this.amount += stack.getCount();
    }

    public ItemStack asStack() {
        ItemStack stack = this.item.stack.copy();
        stack.setCount(this.amount);
        return stack;
    }

    @Override
    public String toString() {
        return "NetworkItem{" + "locations=" + this.locations + ", item=" + this.item + ", amount=" + this.amount + '}';
    }
}
