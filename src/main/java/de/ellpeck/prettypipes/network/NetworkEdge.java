package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkEdge extends DefaultWeightedEdge implements INBTSerializable<CompoundNBT> {

    public final World world;
    public BlockPos startPipe;
    public BlockPos endPipe;
    public final List<BlockPos> pipes = new ArrayList<>();
    private final Map<Integer, PipeTileEntity> tileCache = new HashMap<>();

    public NetworkEdge(World world) {
        this.world = world;
    }

    public PipeTileEntity getPipe(int index) {
        PipeTileEntity tile = this.tileCache.get(index);
        if (tile == null || tile.isRemoved()) {
            tile = Utility.getTileEntity(PipeTileEntity.class, this.world, this.pipes.get(index));
            this.tileCache.put(index, tile);
        }
        return tile;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("start", NBTUtil.writeBlockPos(this.startPipe));
        nbt.put("end", NBTUtil.writeBlockPos(this.endPipe));
        ListNBT list = new ListNBT();
        for (BlockPos pos : this.pipes)
            list.add(NBTUtil.writeBlockPos(pos));
        nbt.put("pipes", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.startPipe = NBTUtil.readBlockPos(nbt.getCompound("start"));
        this.endPipe = NBTUtil.readBlockPos(nbt.getCompound("end"));
        this.pipes.clear();
        ListNBT list = nbt.getList("pipes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.pipes.add(NBTUtil.readBlockPos(list.getCompound(i)));
    }
}
