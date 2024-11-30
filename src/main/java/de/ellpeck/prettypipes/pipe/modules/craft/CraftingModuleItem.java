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
        var crafts = tile.getActiveCrafts();
        if (!crafts.isEmpty()) {
            var craft = crafts.getFirst();
            if (craft.moduleSlot == slot) {
                if (!craft.ingredientsToRequest.isEmpty()) {
                    // process crafting ingredient requests
                    network.startProfile("crafting_ingredients");
                    var lock = craft.ingredientsToRequest.getFirst();
                    var equalityTypes = ItemFilter.getEqualityTypes(tile);
                    var dest = tile.getAvailableDestination(Direction.values(), lock.stack, true, true);
                    if (dest != null) {
                        // if we're ensuring the correct item order and the item is already on the way, don't do anything yet
                        if (!module.get(Contents.TYPE).ensureItemOrder || network.getPipeItemsOnTheWay(dest.getLeft()).findAny().isEmpty()) {
                            network.requestExistingItem(lock.location, tile.getBlockPos(), dest.getLeft(), lock, dest.getRight(), equalityTypes);
                            network.resolveNetworkLock(lock);
                            craft.ingredientsToRequest.remove(lock);
                            craft.travelingIngredients.add(lock.stack.copy());
                            craft.inProgress = true;
                        }
                    }
                    network.endProfile();
                } else if (craft.travelingIngredients.size() <= 0) {
                    // pull requested crafting results from the network once they are stored
                    network.startProfile("crafting_results");
                    var items = network.getOrderedNetworkItems(tile.getBlockPos());
                    var equalityTypes = ItemFilter.getEqualityTypes(tile);
                    var destPipe = network.getPipe(craft.resultDestPipe);
                    if (destPipe != null) {
                        var dest = destPipe.getAvailableDestinationOrConnectable(craft.resultStackRemain, true, true);
                        if (dest != null) {
                            for (var item : items) {
                                var requestRemain = network.requestExistingItem(item, craft.resultDestPipe, dest.getLeft(), null, dest.getRight(), equalityTypes);
                                craft.resultStackRemain.shrink(dest.getRight().getCount() - requestRemain.getCount());
                                if (craft.resultStackRemain.isEmpty()) {
                                    crafts.remove(craft);
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
        var contents = module.get(Contents.TYPE);

        var network = PipeNetwork.get(tile.getLevel());
        var items = network.getOrderedNetworkItems(tile.getBlockPos());

        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        var resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
        // calculate how many crafting *operations* to do (as opposed to how many *items* to craft)
        var requiredCrafts = Mth.ceil(stack.getCount() / (float) resultAmount);
        var craftableCrafts = Mth.ceil(craftableAmount / (float) resultAmount);
        var toCraft = Math.min(craftableCrafts, requiredCrafts);

        var allCrafts = new ArrayList<ActiveCraft>();
        // if we're ensuring item order, all items for a single recipe should be sent in order first before starting on the next one!
        for (var c = contents.ensureItemOrder ? toCraft : 1; c > 0; c--) {
            var locks = new ArrayList<NetworkLock>();
            for (var i = 0; i < contents.input.getSlots(); i++) {
                var in = contents.input.getStackInSlot(i);
                if (in.isEmpty())
                    continue;
                var copy = in.copy();
                if (!contents.ensureItemOrder)
                    copy.setCount(in.getCount() * toCraft);
                var ret = network.requestLocksAndStartCrafting(tile.getBlockPos(), items, unavailableConsumer, copy, CraftingModuleItem.addDependency(dependencyChain, module), equalityTypes);
                // set crafting dependencies as in progress immediately so that, when canceling, they don't leave behind half-crafted inbetween dependencies
                // TODO to be more optimal, we should really do this when setting the main craft as in progress, but that would require storing references to all of the dependencies
                ret.getRight().forEach(a -> a.inProgress = true);
                locks.addAll(ret.getLeft());
                allCrafts.addAll(ret.getRight());
            }
            var result = stack.copyWithCount(contents.ensureItemOrder ? resultAmount : resultAmount * toCraft);
            var activeCraft = new ActiveCraft(tile.getBlockPos(), slot, locks, destPipe, result);
            tile.getActiveCrafts().add(activeCraft);
            allCrafts.add(activeCraft);
        }

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
        var ourCrafts = allCrafts.stream().filter(c -> c.moduleSlot == slot && !c.getTravelingIngredient(stack, equalityTypes).isEmpty()).iterator();
        while (ourCrafts.hasNext()) {
            var craft = ourCrafts.next();
            craft.travelingIngredients.remove(craft.getTravelingIngredient(stack, equalityTypes));

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

            if (craft.travelingIngredients.size() <= 0 && craft.ingredientsToRequest.size() <= 0) {
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
