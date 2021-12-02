package de.ellpeck.prettypipes.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

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

    public NetworkLock(CompoundTag nbt) {
        this.deserializeNBT(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        nbt.putUUID("id", this.lockId);
        nbt.put("location", this.location.serializeNBT());
        nbt.put("stack", this.stack.save(new CompoundTag()));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.lockId = nbt.getUUID("id");
        this.location = new NetworkLocation(nbt.getCompound("location"));
        this.stack = ItemStack.of(nbt.getCompound("stack"));
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
}
