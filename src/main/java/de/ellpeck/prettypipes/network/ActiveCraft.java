package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEquality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class ActiveCraft implements INBTSerializable<CompoundTag> {

    public List<ItemStack> travelingIngredients = new ArrayList<>();
    public List<NetworkLock> ingredientsToRequest;
    public BlockPos resultDestPipe;
    public ItemStack resultStackRemain;
    public boolean inProgress;
    // we only remove canceled requests from the queue once their items are fully delivered to the crafting location, so that unfinished recipes don't get stuck in crafters etc.
    public boolean canceled;

    public ActiveCraft(List<NetworkLock> ingredientsToRequest, BlockPos resultDestPipe, ItemStack resultStackRemain) {
        this.ingredientsToRequest = ingredientsToRequest;
        this.resultDestPipe = resultDestPipe;
        this.resultStackRemain = resultStackRemain;
    }

    public ActiveCraft(HolderLookup.Provider provider, CompoundTag tag) {
        this.deserializeNBT(provider, tag);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var ret = new CompoundTag();
        ret.put("ingredients_to_request", Utility.serializeAll(this.ingredientsToRequest, n -> n.serializeNBT(provider)));
        ret.put("traveling_ingredients", Utility.serializeAll(this.travelingIngredients, s -> (CompoundTag) s.save(provider, new CompoundTag())));
        ret.putLong("result_dest_pipe", this.resultDestPipe.asLong());
        ret.put("result_stack_remain", this.resultStackRemain.saveOptional(provider));
        ret.putBoolean("in_progress", this.inProgress);
        ret.putBoolean("canceled", this.canceled);
        return ret;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.ingredientsToRequest = Utility.deserializeAll(nbt.getList("ingredients_to_request", Tag.TAG_COMPOUND), t -> new NetworkLock(provider, t));
        this.travelingIngredients = Utility.deserializeAll(nbt.getList("traveling_ingredients", Tag.TAG_COMPOUND), t -> ItemStack.parse(provider, t).orElseThrow());
        this.resultDestPipe = BlockPos.of(nbt.getLong("result_dest_pipe"));
        this.resultStackRemain = ItemStack.parseOptional(provider, nbt.getCompound("result_stack_remain"));
        this.inProgress = nbt.getBoolean("in_progress");
        this.canceled = nbt.getBoolean("canceled");
    }

    public ItemStack getTravelingIngredient(ItemStack stack, ItemEquality... equalityTypes) {
        for (var traveling : this.travelingIngredients) {
            if (ItemEquality.compareItems(stack, traveling, equalityTypes))
                return traveling;
        }
        return ItemStack.EMPTY;
    }

    public boolean markCanceledOrResolve(PipeNetwork network) {
        if (this.inProgress) {
            this.canceled = true;
            return false;
        } else {
            for (var lock : this.ingredientsToRequest)
                network.resolveNetworkLock(lock);
            return true;
        }
    }

}
