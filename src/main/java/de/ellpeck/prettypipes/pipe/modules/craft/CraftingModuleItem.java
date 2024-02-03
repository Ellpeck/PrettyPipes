package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
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
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return true;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex) {
        return new CraftingModuleContainer(Registry.craftingModuleContainer, windowId, player, tile.getBlockPos(), moduleIndex);
    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeBlockEntity tile, Direction direction, IItemHandler handler) {
        return false;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction, IItemHandler destination) {
        return false;
    }

    @Override
    public void tick(ItemStack module, PipeBlockEntity tile) {
        if (!tile.shouldWorkNow(this.speed) || !tile.canWork())
            return;
        var network = PipeNetwork.get(tile.getLevel());
        // process crafting ingredient requests
        if (!tile.craftIngredientRequests.isEmpty()) {
            network.startProfile("crafting_ingredients");
            var request = tile.craftIngredientRequests.peek();
            var equalityTypes = ItemFilter.getEqualityTypes(tile);
            var dest = tile.getAvailableDestination(Direction.values(), request.stack, true, true);
            if (dest != null) {
                var requestRemain = network.requestExistingItem(request.location, tile.getBlockPos(), dest.getLeft(), request, dest.getRight(), equalityTypes);
                network.resolveNetworkLock(request);
                tile.craftIngredientRequests.remove();

                // if we couldn't fit all items into the destination, create another request for the rest
                var remain = request.stack.copy();
                remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                if (!remain.isEmpty()) {
                    var remainRequest = new NetworkLock(request.location, remain);
                    tile.craftIngredientRequests.add(remainRequest);
                    network.createNetworkLock(remainRequest);
                }
            }
            network.endProfile();
        }
        // pull requested crafting results from the network once they are stored
        if (!tile.craftResultRequests.isEmpty()) {
            network.startProfile("crafting_results");
            var items = network.getOrderedNetworkItems(tile.getBlockPos());
            var equalityTypes = ItemFilter.getEqualityTypes(tile);
            for (var request : tile.craftResultRequests) {
                var remain = request.getRight().copy();
                var destPipe = network.getPipe(request.getLeft());
                if (destPipe != null) {
                    var dest = destPipe.getAvailableDestinationOrConnectable(remain, true, true);
                    if (dest == null)
                        continue;
                    for (var item : items) {
                        var requestRemain = network.requestExistingItem(item, request.getLeft(), dest.getLeft(), null, dest.getRight(), equalityTypes);
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
    public List<ItemStack> getAllCraftables(ItemStack module, PipeBlockEntity tile) {
        List<ItemStack> ret = new ArrayList<>();
        var output = this.getOutput(module);
        for (var i = 0; i < output.getSlots(); i++) {
            var stack = output.getStackInSlot(i);
            if (!stack.isEmpty())
                ret.add(stack);
        }
        return ret;
    }

    @Override
    public int getCraftableAmount(ItemStack module, PipeBlockEntity tile, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        var network = PipeNetwork.get(tile.getLevel());
        var items = network.getOrderedNetworkItems(tile.getBlockPos());
        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var input = this.getInput(module);

        var craftable = 0;
        var output = this.getOutput(module);
        for (var i = 0; i < output.getSlots(); i++) {
            var out = output.getStackInSlot(i);
            if (!out.isEmpty() && ItemEquality.compareItems(out, stack, equalityTypes)) {
                // figure out how many crafting operations we can actually do with the input items we have in the network
                var availableCrafts = CraftingTerminalBlockEntity.getAvailableCrafts(tile, input.getSlots(), input::getStackInSlot, k -> true, s -> items, unavailableConsumer, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
                if (availableCrafts > 0)
                    craftable += out.getCount() * availableCrafts;
            }
        }
        return craftable;
    }

    @Override
    public ItemStack craft(ItemStack module, PipeBlockEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        // check if we can craft the required amount of items
        var craftableAmount = this.getCraftableAmount(module, tile, unavailableConsumer, stack, dependencyChain);
        if (craftableAmount <= 0)
            return stack;

        var network = PipeNetwork.get(tile.getLevel());
        var items = network.getOrderedNetworkItems(tile.getBlockPos());

        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        // calculate how many crafting *operations* to do (as opposed to how many *items* to craft)
        var requiredCrafts = Mth.ceil(stack.getCount() / (float) resultAmount);
        var craftableCrafts = Mth.ceil(craftableAmount / (float) resultAmount);
        var toCraft = Math.min(craftableCrafts, requiredCrafts);

        var input = this.getInput(module);
        for (var i = 0; i < input.getSlots(); i++) {
            var in = input.getStackInSlot(i);
            if (in.isEmpty())
                continue;
            var copy = in.copy();
            copy.setCount(in.getCount() * toCraft);
            var ret = ItemTerminalBlockEntity.requestItemLater(tile.getLevel(), tile.getBlockPos(), items, unavailableConsumer, copy, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
            tile.craftIngredientRequests.addAll(ret.getLeft());
        }

        var remain = stack.copy();
        remain.shrink(resultAmount * toCraft);

        var result = stack.copy();
        result.shrink(remain.getCount());
        tile.craftResultRequests.add(Pair.of(destPipe, result));

        return remain;
    }

    public ItemStackHandler getInput(ItemStack module) {
        var handler = new ItemStackHandler(this.inputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("input"));
        return handler;
    }

    public ItemStackHandler getOutput(ItemStack module) {
        var handler = new ItemStackHandler(this.outputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("output"));
        return handler;
    }

    public void save(ItemStackHandler input, ItemStackHandler output, ItemStack module) {
        var tag = module.getOrCreateTag();
        if (input != null)
            tag.put("input", input.serializeNBT());
        if (output != null)
            tag.put("output", output.serializeNBT());
    }

    private int getResultAmountPerCraft(ItemStack module, ItemStack stack, ItemEquality... equalityTypes) {
        var output = this.getOutput(module);
        var resultAmount = 0;
        for (var i = 0; i < output.getSlots(); i++) {
            var out = output.getStackInSlot(i);
            if (ItemEquality.compareItems(stack, out, equalityTypes))
                resultAmount += out.getCount();
        }
        return resultAmount;
    }

    private static Stack<ItemStack> addDependency(Stack<ItemStack> deps, ItemStack module) {
        deps = (Stack<ItemStack>) deps.clone();
        deps.push(module);
        return deps;
    }
}
