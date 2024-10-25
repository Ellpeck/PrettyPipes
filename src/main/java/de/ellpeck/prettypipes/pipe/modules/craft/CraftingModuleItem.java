package de.ellpeck.prettypipes.pipe.modules.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
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
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Consumer;

public class CraftingModuleItem extends ModuleItem {

    private final int speed;

    public CraftingModuleItem(String name, ModuleTier tier) {
        super(name, new Properties().component(Contents.TYPE, new Contents(new ItemStackHandler(tier.forTier(1, 4, 9)), new ItemStackHandler(tier.forTier(1, 2, 4)), false, false)));
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
        var slot = tile.getModuleSlot(module);
        var network = PipeNetwork.get(tile.getLevel());
        // process crafting ingredient requests
        if (!tile.craftIngredientRequests.isEmpty()) {
            network.startProfile("crafting_ingredients");
            var request = tile.craftIngredientRequests.getFirst();
            if (request.getLeft() == slot) {
                var lock = request.getRight();
                var equalityTypes = ItemFilter.getEqualityTypes(tile);
                var dest = tile.getAvailableDestination(Direction.values(), lock.stack, true, true);
                if (dest != null) {
                    var ensureItemOrder = module.get(Contents.TYPE).ensureItemOrder;
                    // if we're ensuring the correct item order and the item is already on the way, don't do anything yet
                    if (!ensureItemOrder || network.getPipeItemsOnTheWay(dest.getLeft()).findAny().isEmpty()) {
                        var requestRemain = network.requestExistingItem(lock.location, tile.getBlockPos(), dest.getLeft(), lock, dest.getRight(), equalityTypes);
                        network.resolveNetworkLock(lock);
                        tile.craftIngredientRequests.remove(request);

                        // if we couldn't fit all items into the destination, create another request for the rest
                        var remain = lock.stack.copy();
                        remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                        if (!remain.isEmpty()) {
                            var remainRequest = new NetworkLock(lock.location, remain);
                            // if we're ensuring item order, we need to insert the remaining request at the start so that it gets processed first
                            var index = ensureItemOrder ? 0 : tile.craftResultRequests.size();
                            tile.craftIngredientRequests.add(index, Pair.of(slot, remainRequest));
                            network.createNetworkLock(remainRequest);
                        }
                    }
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
                if (request.getLeft() == slot) {
                    var remain = request.getRight().copy();
                    var destPipe = network.getPipe(request.getMiddle());
                    if (destPipe != null) {
                        var dest = destPipe.getAvailableDestinationOrConnectable(remain, true, true);
                        if (dest == null)
                            continue;
                        for (var item : items) {
                            var requestRemain = network.requestExistingItem(item, request.getMiddle(), dest.getLeft(), null, dest.getRight(), equalityTypes);
                            remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                            if (remain.isEmpty())
                                break;
                        }
                        if (remain.getCount() != request.getRight().getCount()) {
                            tile.craftResultRequests.remove(request);
                            // if we couldn't pull everything, log a new request
                            if (!remain.isEmpty())
                                tile.craftResultRequests.add(Triple.of(slot, request.getMiddle(), remain));
                            network.endProfile();
                            return;
                        }
                    }
                }
            }
            network.endProfile();
        }
    }

    @Override
    public List<ItemStack> getAllCraftables(ItemStack module, PipeBlockEntity tile) {
        List<ItemStack> ret = new ArrayList<>();
        var output = module.get(Contents.TYPE).output;
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
        var content = module.get(Contents.TYPE);

        var craftable = 0;
        for (var i = 0; i < content.output.getSlots(); i++) {
            var out = content.output.getStackInSlot(i);
            if (!out.isEmpty() && ItemEquality.compareItems(out, stack, equalityTypes)) {
                // figure out how many crafting operations we can actually do with the input items we have in the network
                var availableCrafts = CraftingTerminalBlockEntity.getAvailableCrafts(tile, content.input.getSlots(), content.input::getStackInSlot, k -> true, s -> items, unavailableConsumer, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
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
        var slot = tile.getModuleSlot(module);

        var network = PipeNetwork.get(tile.getLevel());
        var items = network.getOrderedNetworkItems(tile.getBlockPos());

        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        // calculate how many crafting *operations* to do (as opposed to how many *items* to craft)
        var requiredCrafts = Mth.ceil(stack.getCount() / (float) resultAmount);
        var craftableCrafts = Mth.ceil(craftableAmount / (float) resultAmount);
        var toCraft = Math.min(craftableCrafts, requiredCrafts);

        var contents = module.get(Contents.TYPE);
        // if we're ensuring item order, all items for a single recipe should be sent in order first before starting on the next one!
        for (var c = contents.ensureItemOrder ? toCraft : 1; c > 0; c--) {
            for (var i = 0; i < contents.input.getSlots(); i++) {
                var in = contents.input.getStackInSlot(i);
                if (in.isEmpty())
                    continue;
                var copy = in.copy();
                if (!contents.ensureItemOrder)
                    copy.setCount(in.getCount() * toCraft);
                var ret = ItemTerminalBlockEntity.requestItemLater(tile.getLevel(), tile.getBlockPos(), items, unavailableConsumer, copy, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
                for (var lock : ret.getLeft())
                    tile.craftIngredientRequests.add(Pair.of(slot, lock));
            }
        }

        var remain = stack.copy();
        remain.shrink(resultAmount * toCraft);

        var result = stack.copy();
        result.shrink(remain.getCount());
        tile.craftResultRequests.add(Triple.of(slot, destPipe, result));

        return remain;
    }

    @Override
    public ItemStack store(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction) {
        if (module.get(Contents.TYPE).insertSingles) {
            var handler = tile.getItemHandler(direction);
            if (handler != null) {
                while (!stack.isEmpty()) {
                    var remain = ItemHandlerHelper.insertItem(handler, stack.copyWithCount(1), false);
                    if (!remain.isEmpty())
                        break;
                    stack.shrink(1);
                }
            }
        }
        return stack;
    }

    private int getResultAmountPerCraft(ItemStack module, ItemStack stack, ItemEquality... equalityTypes) {
        var output = module.get(Contents.TYPE).output;
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

    public record Contents(ItemStackHandler input, ItemStackHandler output, boolean ensureItemOrder, boolean insertSingles) {

        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Utility.ITEM_STACK_HANDLER_CODEC.fieldOf("input").forGetter(d -> d.input),
            Utility.ITEM_STACK_HANDLER_CODEC.fieldOf("output").forGetter(d -> d.output),
            Codec.BOOL.optionalFieldOf("ensure_item_order", false).forGetter(d -> d.ensureItemOrder),
            Codec.BOOL.optionalFieldOf("insert_singles", false).forGetter(d -> d.insertSingles)
        ).apply(i, Contents::new));
        public static final DataComponentType<Contents> TYPE = DataComponentType.<Contents>builder().persistent(Contents.CODEC).cacheEncoding().build();

    }

}
