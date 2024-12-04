package de.ellpeck.prettypipes.network;

import com.mojang.datafixers.util.Either;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEquality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public class ActiveCraft implements INBTSerializable<CompoundTag> {

    public BlockPos pipe;
    public int moduleSlot;
    public List<Either<NetworkLock, ItemStack>> ingredientsToRequest;
    public List<ItemStack> travelingIngredients;
    public BlockPos resultDestPipe;
    public ItemStack resultStackRemain;
    public boolean inProgress;
    public boolean resultFound;
    // we only remove canceled requests from the queue once their items are fully delivered to the crafting location, so that unfinished recipes don't get stuck in crafters etc.
    public boolean canceled;

    public ActiveCraft(BlockPos pipe, int moduleSlot, List<Either<NetworkLock, ItemStack>> ingredientsToRequest, List<ItemStack> travelingIngredients, BlockPos resultDestPipe, ItemStack resultStackRemain) {
        this.pipe = pipe;
        this.moduleSlot = moduleSlot;
        this.ingredientsToRequest = ingredientsToRequest;
        this.travelingIngredients = travelingIngredients;
        this.resultDestPipe = resultDestPipe;
        this.resultStackRemain = resultStackRemain;
    }

    public ActiveCraft(HolderLookup.Provider provider, CompoundTag tag) {
        this.deserializeNBT(provider, tag);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var ret = new CompoundTag();
        ret.putLong("pipe", this.pipe.asLong());
        ret.putInt("module_slot", this.moduleSlot);
        ret.put("ingredients_to_request", Utility.serializeAll(this.ingredientsToRequest, n -> {
            var tag = new CompoundTag();
            n.ifLeft(l -> tag.put("lock", l.serializeNBT(provider))).ifRight(s -> tag.put("stack", s.save(provider)));
            return tag;
        }));
        ret.put("traveling_ingredients", Utility.serializeAll(this.travelingIngredients, s -> (CompoundTag) s.save(provider, new CompoundTag())));
        ret.putLong("result_dest_pipe", this.resultDestPipe.asLong());
        ret.put("result_stack_remain", this.resultStackRemain.saveOptional(provider));
        ret.putBoolean("in_progress", this.inProgress);
        ret.putBoolean("result_found", this.resultFound);
        ret.putBoolean("canceled", this.canceled);
        return ret;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.pipe = BlockPos.of(nbt.getLong("pipe"));
        this.moduleSlot = nbt.getInt("module_slot");
        this.ingredientsToRequest = Utility.deserializeAll(nbt.getList("ingredients_to_request", Tag.TAG_COMPOUND), t ->
            t.contains("lock") ? Either.left(new NetworkLock(provider, t.getCompound("lock"))) : Either.right(ItemStack.parseOptional(provider, t.getCompound("stack"))));
        this.travelingIngredients = Utility.deserializeAll(nbt.getList("traveling_ingredients", Tag.TAG_COMPOUND), t -> ItemStack.parse(provider, t).orElseThrow());
        this.resultDestPipe = BlockPos.of(nbt.getLong("result_dest_pipe"));
        this.resultStackRemain = ItemStack.parseOptional(provider, nbt.getCompound("result_stack_remain"));
        this.inProgress = nbt.getBoolean("in_progress");
        this.resultFound = nbt.getBoolean("result_found");
        this.canceled = nbt.getBoolean("canceled");
    }

    @Override
    public String toString() {
        return "ActiveCraft{" +
            "pipe=" + this.pipe +
            ", moduleSlot=" + this.moduleSlot +
            ", travelingIngredients=" + this.travelingIngredients +
            ", ingredientsToRequest=" + this.ingredientsToRequest +
            ", resultDestPipe=" + this.resultDestPipe +
            ", resultStackRemain=" + this.resultStackRemain +
            ", inProgress=" + this.inProgress +
            ", resultFound=" + this.resultFound +
            ", canceled=" + this.canceled + '}';
    }

    public ItemStack getTravelingIngredient(ItemStack stack, ItemEquality... equalityTypes) {
        for (var traveling : this.travelingIngredients) {
            if (ItemEquality.compareItems(stack, traveling, equalityTypes))
                return traveling;
        }
        return ItemStack.EMPTY;
    }

    public boolean markCanceledOrResolve(PipeNetwork network, boolean force) {
        if (force || !this.inProgress) {
            for (var lock : this.ingredientsToRequest)
                lock.ifLeft(network::resolveNetworkLock);
            return true;
        } else {
            this.canceled = true;
            return false;
        }
    }

}
