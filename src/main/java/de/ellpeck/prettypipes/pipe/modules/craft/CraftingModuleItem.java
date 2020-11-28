package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CraftingModuleItem extends ModuleItem {

    public final int inputSlots;
    public final int outputSlots;
    private final int speed;

    public CraftingModuleItem(String name, ModuleTier tier) {
        super(name);
        this.inputSlots = tier.forTier(1, 4, 9);
        this.outputSlots = tier.forTier(1, 2, 4);
        this.speed = tier.forTier(20, 10, 5);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return true;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new CraftingModuleContainer(Registry.craftingModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeTileEntity tile) {
        return false;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return false;
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {
        if (!tile.shouldWorkNow(this.speed) || !tile.canWork())
            return;
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        // process crafting ingredient requests
        if (!tile.craftIngredientRequests.isEmpty()) {
            network.startProfile("crafting_ingredients");
            NetworkLock request = tile.craftIngredientRequests.peek();
            ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
            Pair<BlockPos, ItemStack> dest = tile.getAvailableDestination(request.stack, true, true);
            if (dest != null) {
                ItemStack requestRemain = network.requestExistingItem(request.location, tile.getPos(), dest.getLeft(), request, dest.getRight(), equalityTypes);
                network.resolveNetworkLock(request);
                tile.craftIngredientRequests.remove();

                // if we couldn't fit all items into the destination, create another request for the rest
                ItemStack remain = request.stack.copy();
                remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                if (!remain.isEmpty()) {
                    NetworkLock remainRequest = new NetworkLock(request.location, remain);
                    tile.craftIngredientRequests.add(remainRequest);
                    network.createNetworkLock(remainRequest);
                }
            }
            network.endProfile();
        }
        // pull requested crafting results from the network once they are stored
        if (!tile.craftResultRequests.isEmpty()) {
            network.startProfile("crafting_results");
            List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
            ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
            for (Pair<BlockPos, ItemStack> request : tile.craftResultRequests) {
                ItemStack remain = request.getRight().copy();
                PipeTileEntity destPipe = network.getPipe(request.getLeft());
                if (destPipe != null) {
                    Pair<BlockPos, ItemStack> dest = destPipe.getAvailableDestinationOrConnectable(remain, true, true);
                    if (dest == null)
                        continue;
                    for (NetworkLocation item : items) {
                        ItemStack requestRemain = network.requestExistingItem(item, request.getLeft(), dest.getLeft(), null, dest.getRight(), equalityTypes);
                        remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                        if (remain.isEmpty())
                            break;
                    }
                    if (remain.getCount() != request.getRight().getCount()) {
                        tile.craftResultRequests.remove(request);
                        // if we couldn't pull everything, log a new request
                        if (!remain.isEmpty())
                            tile.craftResultRequests.add(Pair.of(request.getLeft(), remain));
                        network.endProfile();
                        return;
                    }
                }
            }
            network.endProfile();
        }
    }

    @Override
    public List<ItemStack> getAllCraftables(ItemStack module, PipeTileEntity tile) {
        List<ItemStack> ret = new ArrayList<>();
        ItemStackHandler output = this.getOutput(module);
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (!stack.isEmpty())
                ret.add(stack);
        }
        return ret;
    }

    @Override
    public int getCraftableAmount(ItemStack module, PipeTileEntity tile, Consumer<ItemStack> unavailableConsumer, ItemStack stack) {
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
        ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
        ItemStackHandler input = this.getInput(module);

        int craftable = 0;
        ItemStackHandler output = this.getOutput(module);
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack out = output.getStackInSlot(i);
            if (!out.isEmpty() && ItemEqualityType.compareItems(out, stack, equalityTypes)) {
                // figure out how many crafting operations we can actually do with the input items we have in the network
                int availableCrafts = CraftingTerminalTileEntity.getAvailableCrafts(tile, input.getSlots(), input::getStackInSlot, k -> true, s -> items, unavailableConsumer, equalityTypes);
                if (availableCrafts > 0)
                    craftable += out.getCount() * availableCrafts;
            }
        }
        return craftable;
    }

    @Override
    public ItemStack craft(ItemStack module, PipeTileEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack) {
        // check if we can craft the required amount of items
        int craftableAmount = this.getCraftableAmount(module, tile, unavailableConsumer, stack);
        if (craftableAmount <= 0)
            return stack;

        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());

        ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
        int resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        int requiredCrafts = MathHelper.ceil(stack.getCount() / (float) resultAmount);
        int toCraft = Math.min(craftableAmount, requiredCrafts);

        ItemStackHandler input = this.getInput(module);
        for (int i = 0; i < input.getSlots(); i++) {
            ItemStack in = input.getStackInSlot(i);
            if (in.isEmpty())
                continue;
            ItemStack copy = in.copy();
            copy.setCount(in.getCount() * toCraft);
            Pair<List<NetworkLock>, ItemStack> ret = ItemTerminalTileEntity.requestItemLater(tile.getWorld(), tile.getPos(), items, unavailableConsumer, copy, equalityTypes);
            tile.craftIngredientRequests.addAll(ret.getLeft());
        }

        ItemStack remain = stack.copy();
        remain.shrink(resultAmount * toCraft);

        ItemStack result = stack.copy();
        result.shrink(remain.getCount());
        tile.craftResultRequests.add(Pair.of(destPipe, result));

        return remain;
    }

    public ItemStackHandler getInput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.inputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("input"));
        return handler;
    }

    public ItemStackHandler getOutput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.outputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("output"));
        return handler;
    }

    public void save(ItemStackHandler input, ItemStackHandler output, ItemStack module) {
        CompoundNBT tag = module.getOrCreateTag();
        if (input != null)
            tag.put("input", input.serializeNBT());
        if (output != null)
            tag.put("output", output.serializeNBT());
    }

    private int getResultAmountPerCraft(ItemStack module, ItemStack stack, ItemEqualityType... equalityTypes) {
        ItemStackHandler output = this.getOutput(module);
        int resultAmount = 0;
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack out = output.getStackInSlot(i);
            if (ItemEqualityType.compareItems(stack, out, equalityTypes))
                resultAmount += out.getCount();
        }
        return resultAmount;
    }
}
