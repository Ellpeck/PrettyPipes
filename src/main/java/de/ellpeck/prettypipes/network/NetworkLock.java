package de.ellpeck.prettypipes.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collection;

public class NetworkLock implements INBTSerializable<CompoundNBT> {

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
        nbt.put("location", this.location.serializeNBT());
        nbt.put("stack", this.stack.write(new CompoundNBT()));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.location = new NetworkLocation(nbt.getCompound("location"));
        this.stack = ItemStack.read(nbt.getCompound("stack"));
    }
}
