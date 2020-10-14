package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkLocation implements INBTSerializable<CompoundNBT> {

    public BlockPos pipePos;
    public Direction direction;
    private Map<Integer, ItemStack> itemCache;
    private IItemHandler handlerCache;

    public NetworkLocation(BlockPos pipePos, Direction direction) {
        this.pipePos = pipePos;
        this.direction = direction;
    }

    public NetworkLocation(CompoundNBT nbt) {
        this.deserializeNBT(nbt);
    }

    public List<Integer> getStackSlots(World world, ItemStack stack, ItemEqualityType... equalityTypes) {
        if (this.isEmpty(world))
            return Collections.emptyList();
        return this.getItems(world).entrySet().stream()
                .filter(e -> ItemEqualityType.compareItems(e.getValue(), stack, equalityTypes))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int getItemAmount(World world, ItemStack stack, ItemEqualityType... equalityTypes) {
        if (this.isEmpty(world))
            return 0;
        return this.getItems(world).values().stream()
                .filter(i -> ItemEqualityType.compareItems(stack, i, equalityTypes))
                .mapToInt(ItemStack::getCount).sum();
    }

    public Map<Integer, ItemStack> getItems(World world) {
        if (this.itemCache == null) {
            IItemHandler handler = this.getItemHandler(world);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack found = handler.extractItem(i, Integer.MAX_VALUE, true);
                    if (found.isEmpty())
                        continue;
                    if (this.itemCache == null)
                        this.itemCache = new HashMap<>();
                    this.itemCache.put(i, found);
                }
            }
        }
        return this.itemCache;
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
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("pipe_pos", NBTUtil.writeBlockPos(this.pipePos));
        nbt.putInt("direction", this.direction.getIndex());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.pipePos = NBTUtil.readBlockPos(nbt.getCompound("pipe_pos"));
        this.direction = Direction.byIndex(nbt.getInt("direction"));
    }
}
