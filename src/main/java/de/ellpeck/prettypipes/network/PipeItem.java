package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PipeItem implements INBTSerializable<CompoundNBT> {

    public static final int PIPE_TIME = 20;

    public ItemStack stack;
    public float x;
    public float y;
    public float z;
    public float lastX;
    public float lastY;
    public float lastZ;

    private List<BlockPos> path;
    private BlockPos startPipe;
    private BlockPos startInventory;
    private BlockPos destPipe;
    private BlockPos destInventory;
    private int pipeTimer;
    private int currentTile;
    private boolean dropOnObstruction;
    private long lastWorldTick;

    public PipeItem(ItemStack stack, BlockPos startPipe, BlockPos startInventory) {
        this.stack = stack;
        this.startInventory = startInventory;
        this.x = MathHelper.lerp(0.5F, startInventory.getX(), startPipe.getX()) + 0.5F;
        this.y = MathHelper.lerp(0.5F, startInventory.getY(), startPipe.getY()) + 0.5F;
        this.z = MathHelper.lerp(0.5F, startInventory.getZ(), startPipe.getZ()) + 0.5F;
    }

    public PipeItem(CompoundNBT nbt) {
        this.path = new ArrayList<>();
        this.deserializeNBT(nbt);
    }

    public void setDestination(BlockPos startPipe, BlockPos destPipe, BlockPos destInventory, GraphPath<BlockPos, NetworkEdge> path) {
        this.startPipe = startPipe;
        this.destPipe = destPipe;
        this.destInventory = destInventory;
        this.path = compilePath(path);
        this.currentTile = 0;
        this.pipeTimer = 0;
    }

    public void updateInPipe(PipeTileEntity currPipe) {
        // this prevents pipes being updated after one another
        // causing an item that just switched to tick twice
        long worldTick = currPipe.getWorld().getGameTime();
        if (this.lastWorldTick == worldTick)
            return;
        this.lastWorldTick = worldTick;

        this.pipeTimer++;
        BlockPos goalPos;
        if (this.pipeTimer >= PIPE_TIME) {
            // we're done with the current pipe, so switch to the next one
            currPipe.items.remove(this);
            PipeTileEntity next = this.getNextTile(currPipe, true);
            if (next == null) {
                if (!currPipe.getWorld().isRemote) {
                    if (this.reachedDestination()) {
                        // ..or store in our destination container if we reached our destination
                        this.stack = this.store(currPipe);
                        if (!this.stack.isEmpty())
                            this.onPathObstructed(currPipe, true);
                    } else {
                        this.onPathObstructed(currPipe, false);
                    }
                }
                return;
            } else {
                next.items.add(this);
                this.pipeTimer = 0;
                goalPos = next.getPos();
            }
        } else if (this.pipeTimer > PIPE_TIME / 2) {
            // we're past the start of the pipe, so move to the center of the next pipe
            PipeTileEntity next = this.getNextTile(currPipe, false);
            if (next == null) {
                if (this.reachedDestination()) {
                    goalPos = this.destInventory;
                } else {
                    currPipe.items.remove(this);
                    if (!currPipe.getWorld().isRemote)
                        this.onPathObstructed(currPipe, false);
                    return;
                }
            } else {
                goalPos = next.getPos();
            }
        } else {
            // we're at the start of the pipe, so just move towards its center
            goalPos = currPipe.getPos();
        }

        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;

        Vec3d dist = new Vec3d(goalPos.getX() + 0.5F - this.x, goalPos.getY() + 0.5F - this.y, goalPos.getZ() + 0.5F - this.z);
        dist = dist.normalize();
        this.x += dist.x / PIPE_TIME;
        this.y += dist.y / PIPE_TIME;
        this.z += dist.z / PIPE_TIME;
    }

    private void onPathObstructed(PipeTileEntity currPipe, boolean tryReturn) {
        if (!this.dropOnObstruction && tryReturn) {
            PipeNetwork network = PipeNetwork.get(currPipe.getWorld());
            if (network.routeItemToLocation(currPipe.getPos(), this.startPipe, this.startInventory, () -> this)) {
                this.dropOnObstruction = true;
                return;
            }
        }
        this.drop(currPipe.getWorld());
    }

    public void drop(World world) {
        ItemEntity item = new ItemEntity(world, this.x, this.y, this.z, this.stack.copy());
        item.world.addEntity(item);
    }

    private ItemStack store(PipeTileEntity currPipe) {
        TileEntity tile = currPipe.getWorld().getTileEntity(this.destInventory);
        if (tile == null)
            return this.stack;
        Direction dir = Utility.getDirectionFromOffset(this.destPipe, this.destInventory);
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir).orElse(null);
        if (handler == null)
            return this.stack;
        return ItemHandlerHelper.insertItemStacked(handler, this.stack, false);
    }

    private boolean reachedDestination() {
        return this.currentTile >= this.path.size() - 1;
    }

    private PipeTileEntity getNextTile(PipeTileEntity currPipe, boolean progress) {
        if (this.path.size() <= this.currentTile + 1)
            return null;
        BlockPos pos = this.path.get(this.currentTile + 1);
        if (progress)
            this.currentTile++;
        PipeNetwork network = PipeNetwork.get(currPipe.getWorld());
        return network.getPipe(pos);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("stack", this.stack.serializeNBT());
        nbt.put("start_pipe", NBTUtil.writeBlockPos(this.startPipe));
        nbt.put("start_inventory", NBTUtil.writeBlockPos(this.startInventory));
        nbt.put("dest_pipe", NBTUtil.writeBlockPos(this.destPipe));
        nbt.put("dest_inv", NBTUtil.writeBlockPos(this.destInventory));
        nbt.putBoolean("drop_on_obstruction", this.dropOnObstruction);
        nbt.putInt("timer", this.pipeTimer);
        nbt.putInt("tile", this.currentTile);
        nbt.putFloat("x", this.x);
        nbt.putFloat("y", this.y);
        nbt.putFloat("z", this.z);
        ListNBT list = new ListNBT();
        for (BlockPos pos : this.path)
            list.add(NBTUtil.writeBlockPos(pos));
        nbt.put("path", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.stack = ItemStack.read(nbt.getCompound("stack"));
        this.startPipe = NBTUtil.readBlockPos(nbt.getCompound("start_pipe"));
        this.startInventory = NBTUtil.readBlockPos(nbt.getCompound("start_inventory"));
        this.destPipe = NBTUtil.readBlockPos(nbt.getCompound("dest_pipe"));
        this.destInventory = NBTUtil.readBlockPos(nbt.getCompound("dest_inv"));
        this.dropOnObstruction = nbt.getBoolean("drop_on_obstruction");
        this.pipeTimer = nbt.getInt("timer");
        this.currentTile = nbt.getInt("tile");
        this.x = nbt.getFloat("x");
        this.y = nbt.getFloat("y");
        this.z = nbt.getFloat("z");
        this.path.clear();
        ListNBT list = nbt.getList("path", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.path.add(NBTUtil.readBlockPos(list.getCompound(i)));
    }

    private static List<BlockPos> compilePath(GraphPath<BlockPos, NetworkEdge> path) {
        Graph<BlockPos, NetworkEdge> graph = path.getGraph();
        List<BlockPos> ret = new ArrayList<>();
        List<BlockPos> nodes = path.getVertexList();
        for (int i = 0; i < nodes.size() - 1; i++) {
            BlockPos first = nodes.get(i);
            BlockPos second = nodes.get(i + 1);
            NetworkEdge edge = graph.getEdge(first, second);
            Consumer<Integer> add = j -> {
                BlockPos pos = edge.pipes.get(j);
                if (!ret.contains(pos))
                    ret.add(pos);
            };
            // if the edge is the other way around, we need to loop through tiles
            // the other way also
            if (!graph.getEdgeSource(edge).equals(first)) {
                for (int j = edge.pipes.size() - 1; j >= 0; j--)
                    add.accept(j);
            } else {
                for (int j = 0; j < edge.pipes.size(); j++)
                    add.accept(j);
            }
        }
        return ret;
    }
}
