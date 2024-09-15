package de.ellpeck.prettypipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

public class NetworkEdge extends DefaultWeightedEdge implements INBTSerializable<CompoundTag> {

    public final List<BlockPos> pipes = new ArrayList<>();

    public NetworkEdge() {
    }

    public NetworkEdge(HolderLookup.Provider provider, CompoundTag nbt) {
        this.deserializeNBT(provider, nbt);
    }

    public BlockPos getStartPipe() {
        return this.pipes.getFirst();
    }

    public BlockPos getEndPipe() {
        return this.pipes.getLast();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        var list = new ListTag();
        for (var pos : this.pipes)
            list.add(NbtUtils.writeBlockPos(pos));
        nbt.put("pipes", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.pipes.clear();
        var list = nbt.getList("pipes", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++)
            this.pipes.add(NbtUtils.readBlockPos(list.getCompound(i)));
    }

}
