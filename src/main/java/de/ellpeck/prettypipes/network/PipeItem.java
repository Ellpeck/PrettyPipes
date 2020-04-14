package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;

public class PipeItem implements INBTSerializable<CompoundNBT> {

    public static final int PIPE_TIME = 40;

    public ItemStack stack;
    public float x;
    public float y;
    public float z;

    private final List<NetworkEdge> pathEdges;
    private BlockPos startPipe;
    private BlockPos destPipe;
    private BlockPos destInventory;
    private int pipeTimer;
    private int currentEdge;
    private int currentTile;

    public PipeItem(ItemStack stack, BlockPos startPipe, BlockPos startInventory, BlockPos destPipe, BlockPos destInventory, GraphPath<BlockPos, NetworkEdge> path) {
        this.stack = stack;
        this.startPipe = startPipe;
        this.destPipe = destPipe;
        this.destInventory = destInventory;
        this.pathEdges = path.getEdgeList();
        this.x = MathHelper.lerp(0.5F, startInventory.getX(), startPipe.getX()) + 0.5F;
        this.y = MathHelper.lerp(0.5F, startInventory.getY(), startPipe.getY()) + 0.5F;
        this.z = MathHelper.lerp(0.5F, startInventory.getZ(), startPipe.getZ()) + 0.5F;
    }

    public PipeItem(CompoundNBT nbt) {
        this.pathEdges = new ArrayList<>();
        this.deserializeNBT(nbt);
    }

    public void updateInPipe(PipeTileEntity currPipe) {
        this.pipeTimer++;
        BlockPos goalPos;
        if (this.pipeTimer >= PIPE_TIME) {
            // we're done with the current pipe, so switch to the next one
            currPipe.items.remove(this);
            PipeTileEntity next = this.getNextTile(currPipe, true);
            if (next == null) {
                // ..or store in our destination container if there is no next one
                this.store(currPipe);
                return;
            } else {
                next.items.add(this);
                this.pipeTimer = 0;
                goalPos = next.getPos();
            }
        } else if (this.pipeTimer >= PIPE_TIME / 2) {
            // we're past the start of the pipe, so move to the center of the next pipe
            PipeTileEntity next = this.getNextTile(currPipe, false);
            if (next == null) {
                goalPos = this.destInventory;
            } else {
                goalPos = next.getPos();
            }
        } else {
            // we're at the start of the pipe, so just move towards its center
            goalPos = currPipe.getPos();
        }

        float speed = 1 / (float) PIPE_TIME;
        Vec3d dist = new Vec3d(goalPos.getX() + 0.5F - this.x, goalPos.getY() + 0.5F - this.y, goalPos.getZ() + 0.5F - this.z);
        dist = dist.normalize();
        this.x += dist.x * speed;
        this.y += dist.y * speed;
        this.z += dist.z * speed;
    }

    private void store(PipeTileEntity currPipe) {
        if (currPipe.getWorld().isRemote)
            return;
        // TODO store in destination
    }

    private PipeTileEntity getNextTile(PipeTileEntity currPipe, boolean progress) {
        NetworkEdge edge = this.pathEdges.get(this.currentEdge);
        int currTile = this.currentTile;
        if (edge.pipes.size() > currTile + 1) {
            currTile++;
        } else {
            // are we at the end of our path?
            if (this.pathEdges.size() <= this.currentEdge + 1)
                return null;
            edge = this.pathEdges.get(this.currentEdge + 1);
            // we're setting the current tile to 1 since the 0th index of
            // the next edge is also the last index of the current edge
            currTile = 1;
            if (progress)
                this.currentEdge++;
        }
        if (progress)
            this.currentTile = currTile;

        // TODO invert the current tile index if the current edge is the other way around


        return edge.getPipe(currPipe.getWorld(), currTile);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("stack", this.stack.serializeNBT());
        nbt.put("start_pipe", NBTUtil.writeBlockPos(this.startPipe));
        nbt.put("dest_pipe", NBTUtil.writeBlockPos(this.destPipe));
        nbt.put("dest_inv", NBTUtil.writeBlockPos(this.destInventory));
        nbt.putInt("timer", this.pipeTimer);
        nbt.putInt("edge", this.currentEdge);
        nbt.putInt("tile", this.currentTile);
        nbt.putFloat("x", this.x);
        nbt.putFloat("y", this.y);
        nbt.putFloat("z", this.z);
        ListNBT list = new ListNBT();
        for (NetworkEdge edge : this.pathEdges)
            list.add(edge.serializeNBT());
        nbt.put("path", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.stack = ItemStack.read(nbt.getCompound("stack"));
        this.startPipe = NBTUtil.readBlockPos(nbt.getCompound("start_pipe"));
        this.destPipe = NBTUtil.readBlockPos(nbt.getCompound("dest_pipe"));
        this.destInventory = NBTUtil.readBlockPos(nbt.getCompound("dest_inv"));
        this.pipeTimer = nbt.getInt("timer");
        this.currentEdge = nbt.getInt("edge");
        this.currentTile = nbt.getInt("tile");
        this.x = nbt.getFloat("x");
        this.y = nbt.getFloat("y");
        this.z = nbt.getFloat("z");
        this.pathEdges.clear();
        ListNBT list = nbt.getList("path", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.pathEdges.add(new NetworkEdge(list.getCompound(i)));
    }
}
