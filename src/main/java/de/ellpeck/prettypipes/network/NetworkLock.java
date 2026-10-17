package de.ellpeck.prettypipes.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.Objects;
import java.util.UUID;

public class NetworkLock implements ValueIOSerializable {

    // identify locks by UUID since network locks can't be identified by location and locked item alone
    // (two locks could be set for the same item and the same amount if it exists twice in the chest)
    private UUID lockId = UUID.randomUUID();
    public NetworkLocation location;
    public ItemStack stack;

    public NetworkLock(NetworkLocation location, ItemStack stack) {
        this.location = location;
        this.stack = stack;
    }

    public NetworkLock(ValueInput input) {
        this.deserialize(input);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("id", UUIDUtil.CODEC, this.lockId);
        this.location.serialize(output.child("location"));
        output.store("stack", ItemStack.CODEC, this.stack);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.lockId = input.read("id", UUIDUtil.CODEC).orElse(null);
        this.location = new NetworkLocation(input.childOrEmpty("location"));
        this.stack = input.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
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
