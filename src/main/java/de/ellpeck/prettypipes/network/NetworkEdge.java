package de.ellpeck.prettypipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

public class NetworkEdge extends DefaultWeightedEdge implements INBTSerializable<CompoundTag> {

    public final List<BlockPos> pipes = new ArrayList<>();

    public NetworkEdge() {
    }

    public NetworkEdge(CompoundTag nbt) {
        this.deserializeNBT(nbt);
    }

    public BlockPos getStartPipe() {
        return this.pipes.get(0);
    }

    public BlockPos getEndPipe() {
        return this.pipes.get(this.pipes.size() - 1);
    }

    @Override
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        var list = new ListTag();
        for (var pos : this.pipes)
            list.add(NbtUtils.writeBlockPos(pos));
        nbt.put("pipes", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.pipes.clear();
        var list = nbt.getList("pipes", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++)
            this.pipes.add(NbtUtils.readBlockPos(list.getCompound(i)));
    }
}
