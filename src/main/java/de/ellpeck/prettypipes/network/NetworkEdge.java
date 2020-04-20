package de.ellpeck.prettypipes.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

public class NetworkEdge extends DefaultWeightedEdge implements INBTSerializable<CompoundNBT> {

    public final List<BlockPos> pipes = new ArrayList<>();

    public NetworkEdge() {
    }

    public NetworkEdge(CompoundNBT nbt) {
        this.deserializeNBT(nbt);
    }

    public BlockPos getStartPipe() {
        return this.pipes.get(0);
    }

    public BlockPos getEndPipe() {
        return this.pipes.get(this.pipes.size() - 1);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT list = new ListNBT();
        for (BlockPos pos : this.pipes)
            list.add(NBTUtil.writeBlockPos(pos));
        nbt.put("pipes", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.pipes.clear();
        ListNBT list = nbt.getList("pipes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.pipes.add(NBTUtil.readBlockPos(list.getCompound(i)));
    }
}
