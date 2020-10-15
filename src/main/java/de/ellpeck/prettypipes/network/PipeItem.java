package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PipeItem implements INBTSerializable<CompoundNBT>, ILiquidContainer {

    public ItemStack stack;
    public float speed;
    public float x;
    public float y;
    public float z;
    public float lastX;
    public float lastY;
    public float lastZ;

    protected List<BlockPos> path;
    protected BlockPos startInventory;
    protected BlockPos destInventory;
    protected BlockPos currGoalPos;
    protected int currentTile;
    protected boolean retryOnObstruction;
    protected long lastWorldTick;

    public PipeItem(ItemStack stack, float speed) {
        this.stack = stack;
        this.speed = speed;
    }

    public PipeItem(CompoundNBT nbt) {
        this.path = new ArrayList<>();
        this.deserializeNBT(nbt);
    }

    public void setDestination(BlockPos startInventory, BlockPos destInventory, GraphPath<BlockPos, NetworkEdge> path) {
        this.startInventory = startInventory;
        this.destInventory = destInventory;
        this.path = compilePath(path);
        this.currGoalPos = this.getStartPipe();
        this.currentTile = 0;

        // initialize position if new
        if (this.x == 0 && this.y == 0 && this.z == 0) {
            this.x = MathHelper.lerp(0.5F, startInventory.getX(), this.currGoalPos.getX()) + 0.5F;
            this.y = MathHelper.lerp(0.5F, startInventory.getY(), this.currGoalPos.getY()) + 0.5F;
            this.z = MathHelper.lerp(0.5F, startInventory.getZ(), this.currGoalPos.getZ()) + 0.5F;
        }
    }

    public void updateInPipe(PipeTileEntity currPipe) {
        // this prevents pipes being updated after one another
        // causing an item that just switched to tick twice
        long worldTick = currPipe.getWorld().getGameTime();
        if (this.lastWorldTick == worldTick)
            return;
        this.lastWorldTick = worldTick;

        float motionLeft = this.speed;
        while (motionLeft > 0) {
            float currSpeed = Math.min(0.25F, motionLeft);
            motionLeft -= currSpeed;

            BlockPos myPos = new BlockPos(this.x, this.y, this.z);
            if (!myPos.equals(currPipe.getPos()) && (currPipe.getPos().equals(this.getDestPipe()) || !myPos.equals(this.startInventory))) {
                // we're done with the current pipe, so switch to the next one
                currPipe.getItems().remove(this);
                PipeTileEntity next = this.getNextTile(currPipe, true);
                if (next == null) {
                    if (!currPipe.getWorld().isRemote) {
                        if (currPipe.getPos().equals(this.getDestPipe())) {
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
                    next.getItems().add(this);
                    currPipe = next;
                }
            } else {
                double dist = Vector3d.copy(this.currGoalPos).squareDistanceTo(this.x - 0.5F, this.y - 0.5F, this.z - 0.5F);
                if (dist < currSpeed * currSpeed) {
                    // we're past the start of the pipe, so move to the center of the next pipe
                    BlockPos nextPos;
                    PipeTileEntity next = this.getNextTile(currPipe, false);
                    if (next == null) {
                        if (currPipe.getPos().equals(this.getDestPipe())) {
                            nextPos = this.destInventory;
                        } else {
                            currPipe.getItems().remove(this);
                            if (!currPipe.getWorld().isRemote)
                                this.onPathObstructed(currPipe, false);
                            return;
                        }
                    } else {
                        nextPos = next.getPos();
                    }
                    float tolerance = 0.001F;
                    if (dist >= tolerance * tolerance) {
                        // when going around corners, we want to move right up to the corner
                        Vector3d motion = new Vector3d(this.x - this.lastX, this.y - this.lastY, this.z - this.lastZ);
                        Vector3d diff = new Vector3d(nextPos.getX() + 0.5F - this.x, nextPos.getY() + 0.5F - this.y, nextPos.getZ() + 0.5F - this.z);
                        if (motion.crossProduct(diff).length() >= tolerance) {
                            currSpeed = (float) Math.sqrt(dist);
                        } else {
                            // we're not going around a corner, so continue
                            this.currGoalPos = nextPos;
                        }
                    } else {
                        // distance is very small, so continue
                        this.currGoalPos = nextPos;
                    }
                }
            }

            this.lastX = this.x;
            this.lastY = this.y;
            this.lastZ = this.z;

            Vector3d dist = new Vector3d(this.currGoalPos.getX() + 0.5F - this.x, this.currGoalPos.getY() + 0.5F - this.y, this.currGoalPos.getZ() + 0.5F - this.z);
            dist = dist.normalize();
            this.x += dist.x * currSpeed;
            this.y += dist.y * currSpeed;
            this.z += dist.z * currSpeed;
        }
    }

    protected void onPathObstructed(PipeTileEntity currPipe, boolean tryReturn) {
        if (currPipe.getWorld().isRemote)
            return;
        PipeNetwork network = PipeNetwork.get(currPipe.getWorld());
        if (tryReturn) {
            // first time: we try to return to our input chest
            if (!this.retryOnObstruction && network.routeItemToLocation(currPipe.getPos(), this.destInventory, this.getStartPipe(), this.startInventory, this.stack, speed -> this)) {
                this.retryOnObstruction = true;
                return;
            }
            // second time: we arrived at our input chest, it is full, so we try to find a different goal location
            ItemStack remain = network.routeItem(currPipe.getPos(), this.destInventory, this.stack, (stack, speed) -> this, false);
            if (!remain.isEmpty())
                this.drop(currPipe.getWorld(), remain.copy());
        } else {
            // if all re-routing attempts fail, we drop
            this.drop(currPipe.getWorld(), this.stack);
        }
    }

    public void drop(World world, ItemStack stack) {
        ItemEntity item = new ItemEntity(world, this.x, this.y, this.z, stack.copy());
        item.world.addEntity(item);
    }

    protected ItemStack store(PipeTileEntity currPipe) {
        Direction dir = Utility.getDirectionFromOffset(this.destInventory, this.getDestPipe());
        IPipeConnectable connectable = currPipe.getPipeConnectable(dir);
        if (connectable != null)
            return connectable.insertItem(currPipe.getWorld(), currPipe.getPos(), dir, this.stack, false);
        IItemHandler handler = currPipe.getItemHandler(dir);
        if (handler != null)
            return ItemHandlerHelper.insertItemStacked(handler, this.stack, false);
        return this.stack;
    }

    protected PipeTileEntity getNextTile(PipeTileEntity currPipe, boolean progress) {
        if (this.path.size() <= this.currentTile + 1)
            return null;
        BlockPos pos = this.path.get(this.currentTile + 1);
        if (progress)
            this.currentTile++;
        PipeNetwork network = PipeNetwork.get(currPipe.getWorld());
        return network.getPipe(pos);
    }

    protected BlockPos getStartPipe() {
        return this.path.get(0);
    }

    public BlockPos getDestPipe() {
        return this.path.get(this.path.size() - 1);
    }

    public BlockPos getCurrentPipe() {
        return this.path.get(this.currentTile);
    }

    public BlockPos getDestInventory() {
        return this.destInventory;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("stack", this.stack.serializeNBT());
        nbt.putFloat("speed", this.speed);
        nbt.put("start_inv", NBTUtil.writeBlockPos(this.startInventory));
        nbt.put("dest_inv", NBTUtil.writeBlockPos(this.destInventory));
        nbt.put("curr_goal", NBTUtil.writeBlockPos(this.currGoalPos));
        nbt.putBoolean("drop_on_obstruction", this.retryOnObstruction);
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
        this.speed = nbt.getFloat("speed");
        this.startInventory = NBTUtil.readBlockPos(nbt.getCompound("start_inv"));
        this.destInventory = NBTUtil.readBlockPos(nbt.getCompound("dest_inv"));
        this.currGoalPos = NBTUtil.readBlockPos(nbt.getCompound("curr_goal"));
        this.retryOnObstruction = nbt.getBoolean("drop_on_obstruction");
        this.currentTile = nbt.getInt("tile");
        this.x = nbt.getFloat("x");
        this.y = nbt.getFloat("y");
        this.z = nbt.getFloat("z");
        this.path.clear();
        ListNBT list = nbt.getList("path", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.path.add(NBTUtil.readBlockPos(list.getCompound(i)));
    }

    protected static List<BlockPos> compilePath(GraphPath<BlockPos, NetworkEdge> path) {
        Graph<BlockPos, NetworkEdge> graph = path.getGraph();
        List<BlockPos> ret = new ArrayList<>();
        List<BlockPos> nodes = path.getVertexList();
        if (nodes.size() == 1) {
            // add the single pipe twice if there's only one
            // this is a dirty hack but it works fine so eh
            for (int i = 0; i < 2; i++)
                ret.add(nodes.get(0));
            return ret;
        }
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

    @Override
    public boolean canContainFluid(IBlockReader worldIn, BlockPos pos, BlockState state, Fluid fluidIn) {
        return true;
    }

    @Override
    public boolean receiveFluid(IWorld worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
        return false;
    }
}
