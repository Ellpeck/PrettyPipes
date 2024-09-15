package de.ellpeck.prettypipes.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Objects;
import java.util.UUID;

public class NetworkLock implements INBTSerializable<CompoundTag> {

    // identify locks by UUID since network locks can't be identified by location and locked item alone
    // (two locks could be set for the same item and the same amount if it exists twice in the chest)
    private UUID lockId = UUID.randomUUID();
    public NetworkLocation location;
    public ItemStack stack;

    public NetworkLock(NetworkLocation location, ItemStack stack) {
        this.location = location;
        this.stack = stack;
    }

    public NetworkLock(HolderLookup.Provider provider, CompoundTag nbt) {
        this.deserializeNBT(provider, nbt);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        nbt.putUUID("id", this.lockId);
        nbt.put("location", this.location.serializeNBT(provider));
        nbt.put("stack", this.stack.save(provider));
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.lockId = nbt.getUUID("id");
        this.location = new NetworkLocation(provider, nbt.getCompound("location"));
        this.stack = ItemStack.parseOptional(provider, nbt.getCompound("stack"));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NetworkLock that)
            return this.lockId.equals(that.lockId);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lockId);
    }

    @Override
    public String toString() {
        return "NetworkLock{" + "location=" + this.location.pipePos + ", stack=" + this.stack + '}';
    }

}
