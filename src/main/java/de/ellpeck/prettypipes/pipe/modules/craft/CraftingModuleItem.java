package de.ellpeck.prettypipes.pipe.modules.craft;

import com.mojang.datafixers.util.Either;
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
        super(name, new Properties().component(Contents.TYPE, new Contents(new ItemStackHandler(tier.forTier(1, 4, 9)), new ItemStackHandler(tier.forTier(1, 2, 4)), false, false, false)));
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
        var foundMainCraft = false;
        var crafts = tile.getActiveCrafts().iterator();
        while (crafts.hasNext()) {
            var craft = crafts.next();

            // handle main crafting, which only one recipe (in the whole pipe!) should be able to do at a time so that items between recipes don't mix
            if (!foundMainCraft) {
                // process crafting ingredient requests
                if (!craft.ingredientsToRequest.isEmpty()) {
                    if (craft.moduleSlot == slot) {
                        network.startProfile("crafting_ingredients");
                        var ingredient = craft.ingredientsToRequest.getFirst();
                        var toRequest = ingredient.map(l -> l.stack, s -> s).copy();
                        var dest = tile.getAvailableDestination(Direction.values(), toRequest, true, true);
                        if (dest != null) {
                            // if we're ensuring the correct item order and the item is already on the way, don't do anything yet
                            if (!module.get(Contents.TYPE).ensureItemOrder || craft.travelingIngredients.isEmpty()) {
                                var equalityTypes = ItemFilter.getEqualityTypes(tile);
                                var requested = ingredient.map(l -> {
                                    // we can ignore the return value here since we're using a lock, so we know that the item is already waiting for us there
                                    network.requestExistingItem(l.location, tile.getBlockPos(), dest.getLeft(), l, dest.getRight(), equalityTypes);
                                    network.resolveNetworkLock(l);
                                    return toRequest;
                                }, s -> {
                                    var remain = network.requestExistingItem(tile.getBlockPos(), dest.getLeft(), null, dest.getRight(), equalityTypes);
                                    var ret = s.copyWithCount(s.getCount() - remain.getCount());
                                    s.setCount(remain.getCount());
                                    return ret;
                                });
                                if (!requested.isEmpty()) {
                                    if (toRequest.getCount() - requested.getCount() <= 0)
                                        craft.ingredientsToRequest.remove(ingredient);
                                    craft.travelingIngredients.add(requested);
                                    craft.inProgress = true;
                                }
                            }
                        }
                        network.endProfile();
                    }
                    foundMainCraft = true;
                } else if (!craft.travelingIngredients.isEmpty()) {
                    foundMainCraft = true;
                } else if (!craft.resultFound) {
                    if (craft.moduleSlot == slot) {
                        // check whether the crafting results have arrived in storage
                        if (craft.resultStackRemain.isEmpty()) {
                            // the result stack is empty from the start if this was a partial craft whose results shouldn't be delivered anywhere
                            // (ie someone requested 3 sticks with ensureItemOrder, but the recipe always makes 4, so the 4th recipe has no destination)
                            crafts.remove();
                        } else {
                            // check if the result is in storage
                            var items = network.getOrderedNetworkItems(tile.getBlockPos());
                            var equalityTypes = ItemFilter.getEqualityTypes(tile);
                            network.startProfile("check_crafting_results");
                            var remain = craft.resultStackRemain.copy();
                            for (var item : items) {
                                remain.shrink(item.getItemAmount(tile.getLevel(), remain, equalityTypes) - network.getLockedAmount(item.getPos(), remain, null, equalityTypes));
                                if (remain.isEmpty())
                                    break;
                            }
                            craft.resultFound = remain.isEmpty();
                            network.endProfile();
                        }
                    }
                    foundMainCraft = true;
                }
            }

            // pull requested crafting results from the network once they are stored
            if (craft.resultFound && craft.moduleSlot == slot) {
                network.startProfile("pull_crafting_results");
                var destPipe = network.getPipe(craft.resultDestPipe);
                if (destPipe != null) {
                    var dest = destPipe.getAvailableDestinationOrConnectable(craft.resultStackRemain, true, true);
                    if (dest != null) {
                        var equalityTypes = ItemFilter.getEqualityTypes(tile);
                        var requestRemain = network.requestExistingItem(craft.resultDestPipe, dest.getLeft(), null, dest.getRight(), equalityTypes);
                        craft.resultStackRemain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                        if (craft.resultStackRemain.isEmpty()) {
                            crafts.remove();
                            break;
                        }
                    }
                }
                network.endProfile();
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
        network.startProfile("get_craftable_amount");
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
        network.endProfile();
        return craftable;
    }

    @Override
    public Pair<ItemStack, Collection<ActiveCraft>> craft(ItemStack module, PipeBlockEntity tile, BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        // check if we can craft the required amount of items
        var craftableAmount = this.getCraftableAmount(module, tile, unavailableConsumer, stack, dependencyChain);
        if (craftableAmount <= 0)
            return Pair.of(stack, List.of());

        var network = PipeNetwork.get(tile.getLevel());
        network.startProfile("craft");

        var items = network.getOrderedNetworkItems(tile.getBlockPos());
        var slot = tile.getModuleSlot(module);
        var contents = module.get(Contents.TYPE);

        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        // calculate how many crafting *operations* to do (as opposed to how many *items* to craft)
        var requiredCrafts = Mth.ceil(stack.getCount() / (float) resultAmount);
        var craftableCrafts = Mth.ceil(craftableAmount / (float) resultAmount);
        var toCraft = Math.min(craftableCrafts, requiredCrafts);

        var leftOfRequest = stack.getCount();
        var allCrafts = new ArrayList<ActiveCraft>();
        // if we're ensuring item order, all items for a single recipe should be sent in order first before starting on the next one!
        for (var c = contents.ensureItemOrder ? toCraft : 1; c > 0; c--) {
            var toRequest = new ArrayList<Either<NetworkLock, ItemStack>>();
            for (var i = 0; i < contents.input.getSlots(); i++) {
                var in = contents.input.getStackInSlot(i);
                if (in.isEmpty())
                    continue;
                var request = in.copy();
                if (!contents.ensureItemOrder)
                    request.setCount(in.getCount() * toCraft);
                var ret = network.requestLocksAndStartCrafting(tile.getBlockPos(), items, unavailableConsumer, request, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
                for (var lock : ret.getLeft())
                    toRequest.add(Either.left(lock));
                for (var dep : ret.getRight()) {
                    // if the dependency doesn't have a result stack, it means it's an extraneous craft and we don't need to mark it as a dependency!
                    if (dep.resultStackRemain.isEmpty())
                        continue;
                    // set crafting dependencies as in progress immediately so that, when canceling, they don't leave behind half-crafted intermediate dependencies
                    // TODO to be more optimal, we should really do this when setting the main craft as in progress, but that would require storing references to all of the dependencies
                    dep.inProgress = true;
                    // we don't want dependencies to send their crafted items to us automatically (instead, we request them ourselves to maintain ordering)
                    toRequest.add(Either.right(dep.resultStackRemain));
                    dep.resultStackRemain = ItemStack.EMPTY;
                    allCrafts.add(dep);
                }
            }
            var crafted = contents.ensureItemOrder ? resultAmount : resultAmount * toCraft;
            // items we started craft dependencies for are ones that will be sent to us (so we're waiting for them immediately!)
            var activeCraft = new ActiveCraft(tile.getBlockPos(), slot, toRequest, new ArrayList<>(), destPipe, stack.copyWithCount(Math.min(crafted, leftOfRequest)));
            tile.getActiveCrafts().add(activeCraft);
            allCrafts.add(activeCraft);
            leftOfRequest -= crafted;
        }
        network.endProfile();

        var remain = stack.copy();
        remain.shrink(resultAmount * toCraft);
        return Pair.of(remain, allCrafts);
    }

    @Override
    public ItemStack store(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction) {
        var slot = tile.getModuleSlot(module);
        var contents = module.get(Contents.TYPE);
        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var allCrafts = tile.getActiveCrafts();
        for (var craft : allCrafts.stream().filter(c -> c.moduleSlot == slot && !c.getTravelingIngredient(stack, equalityTypes).isEmpty()).toList()) {
            // TODO currently, we always shrink by the size of stack, even if the container can't actually accept the entire stack
            //  some containers' getAvailableDestination method returns an incorrect value (because it's a heuristic), so parts of this stack might be sent back,
            //  in which case we have to add it back to the collection of items we need to request (as an item stack since we won't have the lock anymore)
            var traveling = craft.getTravelingIngredient(stack, equalityTypes);
            traveling.shrink(stack.getCount());
            if (traveling.isEmpty())
                craft.travelingIngredients.remove(traveling);

            if (contents.insertSingles) {
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

            if (craft.ingredientsToRequest.isEmpty() && craft.travelingIngredients.isEmpty()) {
                if (contents.emitRedstone) {
                    tile.redstoneTicks = 5;
                    tile.getLevel().updateNeighborsAt(tile.getBlockPos(), tile.getBlockState().getBlock());
                }

                // if we canceled the request and all input items are delivered (ie the machine actually got what it expected), remove it from the queue
                if (craft.canceled)
                    allCrafts.remove(craft);
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
        // noinspection unchecked
        deps = (Stack<ItemStack>) deps.clone();
        deps.push(module);
        return deps;
    }

    public record Contents(ItemStackHandler input, ItemStackHandler output, boolean ensureItemOrder, boolean insertSingles, boolean emitRedstone) {

        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Utility.ITEM_STACK_HANDLER_CODEC.fieldOf("input").forGetter(d -> d.input),
            Utility.ITEM_STACK_HANDLER_CODEC.fieldOf("output").forGetter(d -> d.output),
            Codec.BOOL.optionalFieldOf("ensure_item_order", false).forGetter(d -> d.ensureItemOrder),
            Codec.BOOL.optionalFieldOf("insert_singles", false).forGetter(d -> d.insertSingles),
            Codec.BOOL.optionalFieldOf("emit_redstone", false).forGetter(d -> d.emitRedstone)
        ).apply(i, Contents::new));
        public static final DataComponentType<Contents> TYPE = DataComponentType.<Contents>builder().persistent(Contents.CODEC).cacheEncoding().build();

    }

}
