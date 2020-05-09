package de.ellpeck.prettypipes.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collection;

public class NetworkLock implements INBTSerializable<CompoundNBT> {

    public NetworkLocation location;
    public int slot;
    public int amount;

    public NetworkLock(NetworkLocation location, int slot, int amount) {
        this.location = location;
        this.slot = slot;
        this.amount = amount;
    }

    public NetworkLock(CompoundNBT nbt) {
        this.deserializeNBT(nbt);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("location", this.location.serializeNBT());
        nbt.putInt("slot", this.slot);
        nbt.putInt("amount", this.amount);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.location = new NetworkLocation(nbt.getCompound("location"));
        this.slot = nbt.getInt("slot");
        this.amount = nbt.getInt("amount");
    }
}
