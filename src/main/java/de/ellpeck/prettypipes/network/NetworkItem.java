package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.misc.EquatableItemStack;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class NetworkItem {

    private final List<NetworkLocation> locations = new ArrayList<>();
    private final EquatableItemStack item;
    private int amount;

    public NetworkItem(EquatableItemStack item) {
        this.item = item;
    }

    public void add(NetworkLocation location, ItemStack stack) {
        this.amount += stack.getCount();
        if (!this.locations.contains(location))
            this.locations.add(location);
    }

    public Collection<NetworkLocation> getLocations() {
        return this.locations;
    }

    public ItemStack asStack() {
        ItemStack stack = this.item.stack().copy();
        stack.setCount(this.amount);
        return stack;
    }
}
