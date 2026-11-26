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
        var request = output.childrenList("ingredients_to_request");
        for (var ing : this.ingredientsToRequest)
            ing.ifLeft(l -> l.serialize(request.addChild().child("lock"))).ifRight(r -> request.addChild().store("stack", ItemStack.CODEC, r));
        var traveling = output.list("traveling_ingredients", ItemStack.CODEC);
        for (var ing : this.travelingIngredients)
            traveling.add(ing);
        output.putLong("result_dest_pipe", this.resultDestPipe.asLong());
        output.store("result_stack_remain", ItemStack.CODEC, this.resultStackRemain);
        output.putBoolean("in_progress", this.inProgress);
        output.putBoolean("result_found", this.resultFound);
        output.putBoolean("canceled", this.canceled);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.pipe = BlockPos.of(input.getLongOr("pipe", 0));
        this.moduleSlot = input.getIntOr("module_slot", 0);
        this.ingredientsToRequest = input.childrenListOrEmpty("ingredients_to_request").stream().map(t -> t.keySet().contains("lock") ?
            Either.<NetworkLock, ItemStack>left(new NetworkLock(t.child("lock").orElseThrow())) :
            Either.<NetworkLock, ItemStack>right(t.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY))
        ).toList();
        this.travelingIngredients = input.listOrEmpty("traveling_ingredients", ItemStack.CODEC).stream().toList();
        this.resultDestPipe = BlockPos.of(input.getLongOr("result_dest_pipe", 0));
        this.resultStackRemain = input.read("result_stack_remain", ItemStack.CODEC).orElse(ItemStack.EMPTY);
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

    public ItemStack getTravelingIngredient(HolderLookup.Provider provider, ItemStack stack, ItemEquality... equalityTypes) {
        for (var traveling : this.travelingIngredients) {
            if (ItemEquality.compareItems(provider, stack, traveling, equalityTypes))
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
