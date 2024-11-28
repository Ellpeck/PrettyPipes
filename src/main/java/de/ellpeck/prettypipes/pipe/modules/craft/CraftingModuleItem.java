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
import de.ellpeck.prettypipes.network.ActiveCraft;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
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
        if (!tile.activeCrafts.isEmpty()) {
            var activeCraft = tile.activeCrafts.getFirst();
            if (activeCraft.getLeft() == slot) {
                var craftData = activeCraft.getRight();

                // process crafting ingredient requests
                if (!craftData.ingredientsToRequest.isEmpty()) {
                    network.startProfile("crafting_ingredients");
                    var lock = craftData.ingredientsToRequest.getFirst();
                    var equalityTypes = ItemFilter.getEqualityTypes(tile);
                    var dest = tile.getAvailableDestination(Direction.values(), lock.stack, true, true);
                    if (dest != null) {
                        var ensureItemOrder = module.get(Contents.TYPE).ensureItemOrder;
                        // if we're ensuring the correct item order and the item is already on the way, don't do anything yet
                        if (!ensureItemOrder || network.getPipeItemsOnTheWay(dest.getLeft()).findAny().isEmpty()) {
                            var requestRemain = network.requestExistingItem(lock.location, tile.getBlockPos(), dest.getLeft(), lock, dest.getRight(), equalityTypes);
                            network.resolveNetworkLock(lock);
                            craftData.ingredientsToRequest.remove(lock);
                            craftData.inProgress = true;

                            var traveling = lock.stack.copy();
                            traveling.shrink(requestRemain.getCount());
                            craftData.travelingIngredients.add(traveling);

                            // if we couldn't fit all items into the destination, create another request for the rest
                            var remain = lock.stack.copy();
                            remain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                            if (!remain.isEmpty()) {
                                var remainRequest = new NetworkLock(lock.location, remain);
                                // if we're ensuring item order, we need to insert the remaining request at the start so that it gets processed first
                                var index = ensureItemOrder ? 0 : craftData.ingredientsToRequest.size();
                                craftData.ingredientsToRequest.add(index, remainRequest);
                                network.createNetworkLock(remainRequest);
                            }
                        }
                    }
                    network.endProfile();
                }

                // pull requested crafting results from the network once they are stored
                if (!craftData.resultStackRemain.isEmpty()) {
                    network.startProfile("crafting_results");
                    var items = network.getOrderedNetworkItems(tile.getBlockPos());
                    var equalityTypes = ItemFilter.getEqualityTypes(tile);
                    var destPipe = network.getPipe(craftData.resultDestPipe);
                    if (destPipe != null) {
                        var dest = destPipe.getAvailableDestinationOrConnectable(craftData.resultStackRemain, true, true);
                        if (dest != null) {
                            for (var item : items) {
                                var requestRemain = network.requestExistingItem(item, craftData.resultDestPipe, dest.getLeft(), null, dest.getRight(), equalityTypes);
                                craftData.resultStackRemain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                                if (craftData.resultStackRemain.isEmpty()) {
                                    tile.activeCrafts.remove(activeCraft);
                                    break;
                                }
                            }
                        }
                    }
                    network.endProfile();
                }
            }
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
    public Pair<ItemStack, Collection<ActiveCraft>> craft(ItemStack module, PipeBlockEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        // check if we can craft the required amount of items
        var craftableAmount = this.getCraftableAmount(module, tile, unavailableConsumer, stack, dependencyChain);
        if (craftableAmount <= 0)
            return Pair.of(stack, List.of());
        var slot = tile.getModuleSlot(module);

        var network = PipeNetwork.get(tile.getLevel());
        var items = network.getOrderedNetworkItems(tile.getBlockPos());

        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        // calculate how many crafting *operations* to do (as opposed to how many *items* to craft)
        var requiredCrafts = Mth.ceil(stack.getCount() / (float) resultAmount);
        var craftableCrafts = Mth.ceil(craftableAmount / (float) resultAmount);
        var toCraft = Math.min(craftableCrafts, requiredCrafts);

        var locks = new ArrayList<NetworkLock>();
        var crafts = new ArrayList<ActiveCraft>();
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
                var ret = network.requestLocksAndStartCrafting(tile.getBlockPos(), items, unavailableConsumer, copy, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
                locks.addAll(ret.getLeft());
                crafts.addAll(ret.getRight());
            }
        }
        // set crafting dependencies as in progress immediately so that, when canceling, they don't leave behind half-crafted inbetween dependencies
        // TODO to be more optimal, we should really do this when setting the main craft as in progress, but that would require storing references to all of the dependencies
        crafts.forEach(c -> c.inProgress = true);

        var remain = stack.copy();
        remain.shrink(resultAmount * toCraft);
        var result = stack.copy();
        result.shrink(remain.getCount());

        var activeCraft = new ActiveCraft(locks, destPipe, result);
        tile.activeCrafts.add(Pair.of(slot, activeCraft));
        crafts.add(activeCraft);

        return Pair.of(remain, crafts);
    }

    @Override
    public ItemStack store(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction) {
        var slot = tile.getModuleSlot(module);
        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var matchingCraft = tile.activeCrafts.stream()
            .filter(c -> c.getLeft() == slot && !c.getRight().getTravelingIngredient(stack, equalityTypes).isEmpty())
            .findAny().orElse(null);
        if (matchingCraft != null) {
            var data = matchingCraft.getRight();
            data.travelingIngredients.remove(data.getTravelingIngredient(stack, equalityTypes));

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

            // if we canceled the request and all input items are delivered (ie the machine actually got what it expected), remove it from the queue
            if (data.canceled && data.travelingIngredients.size() <= 0 && data.ingredientsToRequest.size() <= 0)
                tile.activeCrafts.remove(matchingCraft);
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
