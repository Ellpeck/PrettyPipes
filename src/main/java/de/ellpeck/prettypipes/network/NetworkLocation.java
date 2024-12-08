package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.misc.ItemEquality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;

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

    public NetworkLocation(HolderLookup.Provider provider, CompoundTag nbt) {
        this.deserializeNBT(provider, nbt);
    }

    public List<Integer> getStackSlots(Level world, ItemStack stack, ItemEquality... equalityTypes) {
        if (this.isEmpty(world))
            return Collections.emptyList();
        return this.getItems(world).entrySet().stream()
                .filter(kv -> ItemEquality.compareItems(kv.getValue(), stack, equalityTypes) && this.canExtract(world, kv.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public int getItemAmount(Level world, ItemStack stack, ItemEquality... equalityTypes) {
        if (this.isEmpty(world))
            return 0;
        return this.getItems(world).entrySet().stream()
                .filter(kv -> ItemEquality.compareItems(stack, kv.getValue(), equalityTypes) && this.canExtract(world, kv.getKey()))
                .mapToInt(kv -> kv.getValue().getCount()).sum();
    }

    public Map<Integer, ItemStack> getItems(Level world) {
        if (this.itemCache == null) {
            var handler = this.getItemHandler(world);
            if (handler != null) {
                for (var i = 0; i < handler.getSlots(); i++) {
                    var stack = handler.getStackInSlot(i);
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

    public boolean canExtract(Level world, int slot) {
        var handler = this.getItemHandler(world);
        return handler != null && !handler.extractItem(slot, 1, true).isEmpty();
    }

    public IItemHandler getItemHandler(Level world) {
        if (this.handlerCache == null) {
            var network = PipeNetwork.get(world);
            var pipe = network.getPipe(this.pipePos);
            this.handlerCache = pipe.getItemHandler(this.direction);
        }
        return this.handlerCache;
    }

    public boolean isEmpty(Level world) {
        var items = this.getItems(world);
        return items == null || items.isEmpty();
    }

    public BlockPos getPos() {
        return this.pipePos.relative(this.direction);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        nbt.put("pipe_pos", NbtUtils.writeBlockPos(this.pipePos));
        nbt.putInt("direction", this.direction.ordinal());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.pipePos = NbtUtils.readBlockPos(nbt, "pipe_pos").orElse(null);
        this.direction = Direction.values()[nbt.getInt("direction")];
    }

}
