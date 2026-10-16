package de.ellpeck.prettypipes.network;

import com.mojang.datafixers.util.Either;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEquality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public class ActiveCraft implements ValueIOSerializable {

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

    public ActiveCraft(ValueInput input) {
        this.deserialize(input);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putLong("pipe", this.pipe.asLong());
        output.putInt("module_slot", this.moduleSlot);
        output.put("ingredients_to_request", Utility.serializeAll(this.ingredientsToRequest, n -> {
            var tag = new CompoundTag();
            n.ifLeft(l -> tag.put("lock", l.serializeNBT(provider))).ifRight(s -> tag.put("stack", s.save(provider)));
            return tag;
        }));
        output.put("traveling_ingredients", Utility.serializeAll(this.travelingIngredients, s -> (CompoundTag) s.save(provider, new CompoundTag())));
        output.putLong("result_dest_pipe", this.resultDestPipe.asLong());
        output.put("result_stack_remain", this.resultStackRemain.saveOptional(provider));
        output.putBoolean("in_progress", this.inProgress);
        output.putBoolean("result_found", this.resultFound);
        output.putBoolean("canceled", this.canceled);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.pipe = BlockPos.of(input.getLongOr("pipe",0));
        this.moduleSlot = input.getIntOr("module_slot",0);
        this.ingredientsToRequest = Utility.deserializeAll(input.childrenList("ingredients_to_request"), t ->
            t.contains("lock") ? Either.left(new NetworkLock(provider, t.getCompound("lock"))) : Either.right(ItemStack.parseOptional(provider, t.getCompound("stack"))));
        this.travelingIngredients = Utility.deserializeAll(input.getList("traveling_ingredients", Tag.TAG_COMPOUND), t -> ItemStack.parse(provider, t).orElseThrow());
        this.resultDestPipe = BlockPos.of(input.getLongOr("result_dest_pipe", 0));
        this.resultStackRemain = ItemStack.parseOptional(provider, input.getCompound("result_stack_remain"));
        this.inProgress = input.getBooleanOr("in_progress", false);
        this.resultFound = input.getBooleanOr("result_found", false);
        this.canceled = input.getBooleanOr("canceled", false);
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
