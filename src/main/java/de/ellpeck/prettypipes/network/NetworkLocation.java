package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NetworkLocation implements INBTSerializable<CompoundTag> {

    public BlockPos pipePos;
    public Direction direction;
    private Map<Integer, ItemStack> itemCache;
    private IItemHandler handlerCache;

    public NetworkLocation(BlockPos pipePos, Direction direction) {
        this.pipePos = pipePos;
        this.direction = direction;
    }

    public NetworkLocation(CompoundTag nbt) {
        this.deserializeNBT(nbt);
    }

    public List<Integer> getStackSlots(World world, ItemStack stack, ItemEquality... equalityTypes) {
        if (this.isEmpty(world))
            return Collections.emptyList();
        return this.getItems(world).entrySet().stream()
                .filter(kv -> ItemEquality.compareItems(kv.getValue(), stack, equalityTypes) && this.canExtract(world, kv.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public int getItemAmount(World world, ItemStack stack, ItemEquality... equalityTypes) {
        if (this.isEmpty(world))
            return 0;
        return this.getItems(world).entrySet().stream()
                .filter(kv -> ItemEquality.compareItems(stack, kv.getValue(), equalityTypes) && this.canExtract(world, kv.getKey()))
                .mapToInt(kv -> kv.getValue().getCount()).sum();
    }

    public Map<Integer, ItemStack> getItems(World world) {
        if (this.itemCache == null) {
            IItemHandler handler = this.getItemHandler(world);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    // check if the slot is accessible to us
                    if (stack.isEmpty())
                        continue;
                    if (this.itemCache == null)
                        this.itemCache = new HashMap<>();
                    // use getStackInSlot since there might be more than 64 items in there
                    this.itemCache.put(i, stack);
                }
            }
        }
        return this.itemCache;
    }

    public boolean canExtract(World world, int slot) {
        IItemHandler handler = this.getItemHandler(world);
        return handler != null && !handler.extractItem(slot, 1, true).isEmpty();
    }

    public IItemHandler getItemHandler(World world) {
        if (this.handlerCache == null) {
            PipeNetwork network = PipeNetwork.get(world);
            PipeTileEntity pipe = network.getPipe(this.pipePos);
            this.handlerCache = pipe.getItemHandler(this.direction);
        }
        return this.handlerCache;
    }

    public boolean isEmpty(World world) {
        Map<Integer, ItemStack> items = this.getItems(world);
        return items == null || items.isEmpty();
    }

    public BlockPos getPos() {
        return this.pipePos.offset(this.direction);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("pipe_pos", NBTUtil.writeBlockPos(this.pipePos));
        nbt.putInt("direction", this.direction.getIndex());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.pipePos = NBTUtil.readBlockPos(nbt.getCompound("pipe_pos"));
        this.direction = Direction.byIndex(nbt.getInt("direction"));
    }
}
