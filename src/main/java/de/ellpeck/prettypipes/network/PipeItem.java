package de.ellpeck.prettypipes.network;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.IPipeItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class PipeItem implements IPipeItem {

    public static final ResourceLocation TYPE = new ResourceLocation(PrettyPipes.ID, "pipe_item");

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
    protected ResourceLocation type;

    public PipeItem(ResourceLocation type, ItemStack stack, float speed) {
        this.type = type;
        this.stack = stack;
        this.speed = speed;
    }

    public PipeItem(ItemStack stack, float speed) {
        this(TYPE, stack, speed);
    }

    public PipeItem(ResourceLocation type, CompoundTag nbt) {
        this.type = type;
        this.path = new ArrayList<>();
        this.deserializeNBT(nbt);
    }

    @Override
    public ItemStack getContent() {
        return this.stack;
    }

    @Override
    public void setDestination(BlockPos startInventory, BlockPos destInventory, GraphPath<BlockPos, NetworkEdge> path) {
        this.startInventory = startInventory;
        this.destInventory = destInventory;
        this.path = compilePath(path);
        this.currGoalPos = this.getStartPipe();
        this.currentTile = 0;

        // initialize position if new
        if (this.x == 0 && this.y == 0 && this.z == 0) {
            this.x = Mth.lerp(0.5F, startInventory.getX(), this.currGoalPos.getX()) + 0.5F;
            this.y = Mth.lerp(0.5F, startInventory.getY(), this.currGoalPos.getY()) + 0.5F;
            this.z = Mth.lerp(0.5F, startInventory.getZ(), this.currGoalPos.getZ()) + 0.5F;
        }
    }

    @Override
    public void updateInPipe(PipeBlockEntity currPipe) {
        // this prevents pipes being updated after one another
        // causing an item that just switched to tick twice
        var worldTick = currPipe.getLevel().getGameTime();
        if (this.lastWorldTick == worldTick)
            return;
        this.lastWorldTick = worldTick;

        var motionLeft = this.speed;
        while (motionLeft > 0) {
            var currSpeed = Math.min(0.25F, motionLeft);
            motionLeft -= currSpeed;

            var myPos = new BlockPos(this.x, this.y, this.z);
            if (!myPos.equals(currPipe.getBlockPos()) && (currPipe.getBlockPos().equals(this.getDestPipe()) || !myPos.equals(this.startInventory))) {
                // we're done with the current pipe, so switch to the next one
                currPipe.getItems().remove(this);
                var next = this.getNextTile(currPipe, true);
                if (next == null) {
                    if (!currPipe.getLevel().isClientSide) {
                        if (currPipe.getBlockPos().equals(this.getDestPipe())) {
                            // ...or store in our destination container if we reached our destination
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
                var dist = (this.currGoalPos).distToLowCornerSqr(this.x - 0.5F, this.y - 0.5F, this.z - 0.5F);
                if (dist < currSpeed * currSpeed) {
                    // we're past the start of the pipe, so move to the center of the next pipe
                    BlockPos nextPos;
                    var next = this.getNextTile(currPipe, false);
                    if (next == null || next == currPipe) {
                        if (currPipe.getBlockPos().equals(this.getDestPipe())) {
                            nextPos = this.destInventory;
                        } else {
                            currPipe.getItems().remove(this);
                            if (!currPipe.getLevel().isClientSide)
                                this.onPathObstructed(currPipe, false);
                            return;
                        }
                    } else {
                        nextPos = next.getBlockPos();
                    }
                    var tolerance = 0.001F;
                    if (dist >= tolerance * tolerance) {
                        // when going around corners, we want to move right up to the corner
                        var motion = new Vec3(this.x - this.lastX, this.y - this.lastY, this.z - this.lastZ);
                        var diff = new Vec3(nextPos.getX() + 0.5F - this.x, nextPos.getY() + 0.5F - this.y, nextPos.getZ() + 0.5F - this.z);
                        if (motion.cross(diff).length() >= tolerance) {
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

            var dist = new Vec3(this.currGoalPos.getX() + 0.5F - this.x, this.currGoalPos.getY() + 0.5F - this.y, this.currGoalPos.getZ() + 0.5F - this.z);
            dist = dist.normalize();
            this.x += dist.x * currSpeed;
            this.y += dist.y * currSpeed;
            this.z += dist.z * currSpeed;
        }
    }

    protected void onPathObstructed(PipeBlockEntity currPipe, boolean tryReturn) {
        if (currPipe.getLevel().isClientSide)
            return;
        var network = PipeNetwork.get(currPipe.getLevel());
        if (tryReturn) {
            // first time: we try to return to our input chest
            if (!this.retryOnObstruction && network.routeItemToLocation(currPipe.getBlockPos(), this.destInventory, this.getStartPipe(), this.startInventory, this.stack, speed -> this)) {
                this.retryOnObstruction = true;
                return;
            }
            // second time: we arrived at our input chest, it is full, so we try to find a different goal location
            var remain = network.routeItem(currPipe.getBlockPos(), this.destInventory, this.stack, (stack, speed) -> this, false);
            if (!remain.isEmpty())
                this.drop(currPipe.getLevel(), remain.copy());
        } else {
            // if all re-routing attempts fail, we drop
            this.drop(currPipe.getLevel(), this.stack);
        }
    }

    @Override
    public void drop(Level world, ItemStack stack) {
        var item = new ItemEntity(world, this.x, this.y, this.z, stack.copy());
        item.level.addFreshEntity(item);
    }

    protected ItemStack store(PipeBlockEntity currPipe) {
        var dir = Utility.getDirectionFromOffset(this.destInventory, this.getDestPipe());
        var connectable = currPipe.getPipeConnectable(dir);
        if (connectable != null)
            return connectable.insertItem(currPipe.getBlockPos(), dir, this.stack, false);
        var handler = currPipe.getItemHandler(dir);
        if (handler != null)
            return ItemHandlerHelper.insertItemStacked(handler, this.stack, false);
        return this.stack;
    }

    protected PipeBlockEntity getNextTile(PipeBlockEntity currPipe, boolean progress) {
        if (this.path.size() <= this.currentTile + 1)
            return null;
        var pos = this.path.get(this.currentTile + 1);
        if (progress)
            this.currentTile++;
        var network = PipeNetwork.get(currPipe.getLevel());
        return network.getPipe(pos);
    }

    protected BlockPos getStartPipe() {
        return this.path.get(0);
    }

    @Override
    public BlockPos getDestPipe() {
        return this.path.get(this.path.size() - 1);
    }

    @Override
    public BlockPos getCurrentPipe() {
        return this.path.get(this.currentTile);
    }

    @Override
    public BlockPos getDestInventory() {
        return this.destInventory;
    }

    @Override
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        nbt.putString("type", this.type.toString());
        nbt.put("stack", this.stack.serializeNBT());
        nbt.putFloat("speed", this.speed);
        nbt.put("start_inv", NbtUtils.writeBlockPos(this.startInventory));
        nbt.put("dest_inv", NbtUtils.writeBlockPos(this.destInventory));
        nbt.put("curr_goal", NbtUtils.writeBlockPos(this.currGoalPos));
        nbt.putBoolean("drop_on_obstruction", this.retryOnObstruction);
        nbt.putInt("tile", this.currentTile);
        nbt.putFloat("x", this.x);
        nbt.putFloat("y", this.y);
        nbt.putFloat("z", this.z);
        var list = new ListTag();
        for (var pos : this.path)
            list.add(NbtUtils.writeBlockPos(pos));
        nbt.put("path", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.stack = ItemStack.of(nbt.getCompound("stack"));
        this.speed = nbt.getFloat("speed");
        this.startInventory = NbtUtils.readBlockPos(nbt.getCompound("start_inv"));
        this.destInventory = NbtUtils.readBlockPos(nbt.getCompound("dest_inv"));
        this.currGoalPos = NbtUtils.readBlockPos(nbt.getCompound("curr_goal"));
        this.retryOnObstruction = nbt.getBoolean("drop_on_obstruction");
        this.currentTile = nbt.getInt("tile");
        this.x = nbt.getFloat("x");
        this.y = nbt.getFloat("y");
        this.z = nbt.getFloat("z");
        this.path.clear();
        var list = nbt.getList("path", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++)
            this.path.add(NbtUtils.readBlockPos(list.getCompound(i)));
    }

    @Override
    public int getItemsOnTheWay(BlockPos goalInv) {
        return this.stack.getCount();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PipeBlockEntity tile, PoseStack matrixStack, Random random, float partialTicks, int light, int overlay, MultiBufferSource source) {
        matrixStack.translate(
                Mth.lerp(partialTicks, this.lastX, this.x),
                Mth.lerp(partialTicks, this.lastY, this.y),
                Mth.lerp(partialTicks, this.lastZ, this.z));

        if (this.stack.getItem() instanceof BlockItem) {
            var scale = 0.7F;
            matrixStack.scale(scale, scale, scale);
            matrixStack.translate(0, -0.2F, 0);
        } else {
            var scale = 0.45F;
            matrixStack.scale(scale, scale, scale);
            matrixStack.translate(0, -0.1F, 0);
        }

        random.setSeed(Item.getId(this.stack.getItem()) + this.stack.getDamageValue());
        var amount = this.getModelCount();

        for (var i = 0; i < amount; i++) {
            matrixStack.pushPose();
            if (amount > 1) {
                matrixStack.translate(
                        (random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F,
                        (random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F,
                        (random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F);
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(this.stack, ItemTransforms.TransformType.GROUND, light, overlay, matrixStack, source, 0);
            matrixStack.popPose();
        }
    }

    protected int getModelCount() {
        var i = 1;
        if (this.stack.getCount() > 48) {
            i = 5;
        } else if (this.stack.getCount() > 32) {
            i = 4;
        } else if (this.stack.getCount() > 16) {
            i = 3;
        } else if (this.stack.getCount() > 1) {
            i = 2;
        }
        return i;
    }

    protected static List<BlockPos> compilePath(GraphPath<BlockPos, NetworkEdge> path) {
        var graph = path.getGraph();
        List<BlockPos> ret = new ArrayList<>();
        var nodes = path.getVertexList();
        if (nodes.size() == 1) {
            // add the single pipe twice if there's only one
            // this is a dirty hack, but it works fine so eh
            for (var i = 0; i < 2; i++)
                ret.add(nodes.get(0));
            return ret;
        }
        for (var i = 0; i < nodes.size() - 1; i++) {
            var first = nodes.get(i);
            var second = nodes.get(i + 1);
            var edge = graph.getEdge(first, second);
            var add = (Consumer<Integer>) j -> {
                var pos = edge.pipes.get(j);
                if (!ret.contains(pos))
                    ret.add(pos);
            };
            // if the edge is the other way around, we need to loop through tiles
            // the other way also
            if (!graph.getEdgeSource(edge).equals(first)) {
                for (var j = edge.pipes.size() - 1; j >= 0; j--)
                    add.accept(j);
            } else {
                for (var j = 0; j < edge.pipes.size(); j++)
                    add.accept(j);
            }
        }
        return ret;
    }
}
