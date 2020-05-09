package de.ellpeck.prettypipes.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public class NetworkLock implements INBTSerializable<CompoundNBT> {

    // identify locks by UUID since network locks can't be identified by location and locked item alone
    // (two locks could be set for the same item and the same amount if it exists twice in the chest)
    private UUID lockId = UUID.randomUUID();
    public NetworkLocation location;
    public ItemStack stack;

    public NetworkLock(NetworkLocation location, ItemStack stack) {
        this.location = location;
        this.stack = stack;
    }

    public NetworkLock(CompoundNBT nbt) {
        this.deserializeNBT(nbt);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUniqueId("id", this.lockId);
        nbt.put("location", this.location.serializeNBT());
        nbt.put("stack", this.stack.write(new CompoundNBT()));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.lockId = nbt.getUniqueId("id");
        this.location = new NetworkLocation(nbt.getCompound("location"));
        this.stack = ItemStack.read(nbt.getCompound("stack"));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NetworkLock) {
            NetworkLock that = (NetworkLock) o;
            return this.lockId.equals(that.lockId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lockId);
    }
}
