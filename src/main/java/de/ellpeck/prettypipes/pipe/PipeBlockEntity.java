package de.ellpeck.prettypipes.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.containers.MainPipeContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.common.capabilities.Capability;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipeBlockEntity extends BlockEntity implements MenuProvider, IPipeConnectable {

    public final ItemStackHandler modules = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            var item = stack.getItem();
            if (!(item instanceof IModule module))
                return false;
            return PipeBlockEntity.this.streamModules().allMatch(m -> module.isCompatible(stack, PipeBlockEntity.this, m.getRight()) && m.getRight().isCompatible(m.getLeft(), PipeBlockEntity.this, module));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PipeBlockEntity.this.setChanged();
        }
    };
    public final Queue<NetworkLock> craftIngredientRequests = new LinkedList<>();
    public final List<Pair<BlockPos, ItemStack>> craftResultRequests = new ArrayList<>();
    public PressurizerBlockEntity pressurizer;
    public BlockState cover;
    public int moduleDropCheck;
    protected List<IPipeItem> items;
    private int lastItemAmount;
    private int priority;
    private final LazyOptional<PipeBlockEntity> lazyThis = LazyOptional.of(() -> this);
    private final Lazy<Integer> workRandomizer = Lazy.of(() -> this.level.random.nextInt(200));

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.pipeBlockEntity, pos, state);
    }

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onChunkUnloaded() {
        PipeNetwork.get(this.level).uncachePipe(this.worldPosition);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put("modules", this.modules.serializeNBT());
        compound.putInt("module_drop_check", this.moduleDropCheck);
        compound.put("requests", Utility.serializeAll(this.craftIngredientRequests));
        if (this.cover != null)
            compound.put("cover", NbtUtils.writeBlockState(this.cover));
        var results = new ListTag();
        for (var triple : this.craftResultRequests) {
            var nbt = new CompoundTag();
            nbt.putLong("dest_pipe", triple.getLeft().asLong());
            nbt.put("item", triple.getRight().serializeNBT());
            results.add(nbt);
        }
        compound.put("craft_results", results);
    }

    @Override
    public void load(CompoundTag compound) {
        this.modules.deserializeNBT(compound.getCompound("modules"));
        this.moduleDropCheck = compound.getInt("module_drop_check");
        this.cover = compound.contains("cover") ? NbtUtils.readBlockState(this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(), compound.getCompound("cover")) : null;
        this.craftIngredientRequests.clear();
        this.craftIngredientRequests.addAll(Utility.deserializeAll(compound.getList("requests", Tag.TAG_COMPOUND), NetworkLock::new));
        this.craftResultRequests.clear();
        var results = compound.getList("craft_results", Tag.TAG_COMPOUND);
        for (var i = 0; i < results.size(); i++) {
            var nbt = results.getCompound(i);
            this.craftResultRequests.add(Pair.of(
                    BlockPos.of(nbt.getLong("dest_pipe")),
                    ItemStack.of(nbt.getCompound("item"))));
        }
        super.load(compound);
    }

    @Override
    public CompoundTag getUpdateTag() {
        // sync pipe items on load
        var nbt = this.saveWithoutMetadata();
        nbt.put("items", Utility.serializeAll(this.getItems()));
        return nbt;
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        this.load(nbt);
        var items = this.getItems();
        items.clear();
        items.addAll(Utility.deserializeAll(nbt.getList("items", Tag.TAG_COMPOUND), IPipeItem::load));
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    public List<IPipeItem> getItems() {
        if (this.items == null)
            this.items = PipeNetwork.get(this.level).getItemsInPipe(this.worldPosition);
        return this.items;
    }

    public void addNewItem(IPipeItem item) {
        // an item might be re-routed from a previous location, but it should still count as a new item then
        if (!this.getItems().contains(item))
            this.getItems().add(item);
        if (this.pressurizer != null)
            this.pressurizer.pressurizeItem(item.getContent(), false);
    }

    public boolean isConnected(Direction dir) {
        return this.getBlockState().getValue(PipeBlock.DIRECTIONS.get(dir)).isConnected();
    }

    public Pair<BlockPos, ItemStack> getAvailableDestinationOrConnectable(ItemStack stack, boolean force, boolean preventOversending) {
        var dest = this.getAvailableDestination(Direction.values(), stack, force, preventOversending);
        if (dest != null)
            return dest;
        // if there's no available destination, try inserting into terminals etc.
        for (var dir : Direction.values()) {
            var connectable = this.getPipeConnectable(dir);
            if (connectable == null)
                continue;
            var connectableRemain = connectable.insertItem(this.worldPosition, dir, stack, true);
            if (connectableRemain.getCount() != stack.getCount()) {
                var inserted = stack.copy();
                inserted.shrink(connectableRemain.getCount());
                return Pair.of(this.worldPosition.relative(dir), inserted);
            }
        }
        return null;
    }

    public Pair<BlockPos, ItemStack> getAvailableDestination(Direction[] directions, ItemStack stack, boolean force, boolean preventOversending) {
        if (!this.canWork())
            return null;
        for (var dir : directions) {
            var handler = this.getItemHandler(dir);
            if (handler == null)
                continue;
            if (!force && this.streamModules().anyMatch(m -> !m.getRight().canAcceptItem(m.getLeft(), this, stack, dir, handler)))
                continue;

            var startSlot = 0;
            var slotAmount = handler.getSlots();
            // the 0th slot of a storage drawer is a "catch-all" for any items that can be inserted into the drawer, and all
            // subsequent slots are the actual slots for the items. this causes a problem because the drawer will seem to have
            // space for more items that it actually does, so we restrict to only inspecting the 0th slot here.
            // see https://github.com/Ellpeck/PrettyPipes/issues/131#issuecomment-1288653623 and Discord convo with Quinteger
            if (handler.getClass().getName().equals("com.jaquadro.minecraft.storagedrawers.capabilities.DrawerItemHandler"))
                slotAmount = 1;

            // check how many items from the stack we can insert into this destination in total
            var remain = stack;
            for (var i = startSlot; i < slotAmount; i++) {
                remain = handler.insertItem(i, remain, true);
                if (remain.isEmpty())
                    break;
            }
            // did we insert anything?
            if (remain.getCount() == stack.getCount())
                continue;

            var toInsert = stack.copy();
            toInsert.shrink(remain.getCount());
            // limit to the max amount that modules allow us to insert
            var maxAmount = this.streamModules().mapToInt(m -> m.getRight().getMaxInsertionAmount(m.getLeft(), this, stack, handler)).min().orElse(Integer.MAX_VALUE);
            if (maxAmount < toInsert.getCount())
                toInsert.setCount(maxAmount);
            var offset = this.worldPosition.relative(dir);
            if (preventOversending || maxAmount < Integer.MAX_VALUE) {
                var network = PipeNetwork.get(this.level);
                // these are the items that are currently in the pipes, going to this inventory
                var onTheWay = network.getItemsOnTheWay(offset, null);
                if (onTheWay > 0) {
                    if (maxAmount < Integer.MAX_VALUE) {
                        // these are the items on the way, limited to items of the same type as stack
                        var onTheWaySame = network.getItemsOnTheWay(offset, stack);
                        // check if any modules are limiting us
                        if (toInsert.getCount() + onTheWaySame > maxAmount)
                            toInsert.setCount(maxAmount - onTheWaySame);
                    }
                    // totalSpace will be the amount of items that fit into the attached container
                    var totalSpace = 0;
                    var copy = stack.copy();
                    for (var i = startSlot; i < slotAmount; i++) {
                        var maxStackSize = copy.getMaxStackSize();
                        // if the container can store more than 64 items in this slot, then it's likely
                        // a barrel or similar, meaning that the slot limit matters more than the max stack size
                        var limit = handler.getSlotLimit(i);
                        if (limit > 64)
                            maxStackSize = limit;
                        copy.setCount(maxStackSize);
                        // this is an inaccurate check since it ignores the fact that some slots might
                        // have space for items of other types, but it'll be good enough for us
                        var left = handler.insertItem(i, copy, true);
                        totalSpace += maxStackSize - left.getCount();
                    }
                    // if the items on the way plus the items we're trying to move are too much, reduce
                    if (onTheWay + toInsert.getCount() > totalSpace)
                        toInsert.setCount(totalSpace - onTheWay);
                }
            }

            // we return the item that can actually be inserted, NOT the remainder!
            if (!toInsert.isEmpty())
                return Pair.of(offset, toInsert);
        }
        return null;
    }

    public int getPriority() {
        return this.priority;
    }

    public float getItemSpeed(ItemStack stack) {
        var moduleSpeed = (float) this.streamModules().mapToDouble(m -> m.getRight().getItemSpeedIncrease(m.getLeft(), this)).sum();
        var pressureSpeed = this.pressurizer != null && this.pressurizer.pressurizeItem(stack, true) ? 0.45F : 0;
        return 0.05F + moduleSpeed + pressureSpeed;
    }

    public boolean canWork() {
        return this.streamModules().allMatch(m -> m.getRight().canPipeWork(m.getLeft(), this));
    }

    public List<ItemStack> getAllCraftables() {
        return this.streamModules()
                .flatMap(m -> m.getRight().getAllCraftables(m.getLeft(), this).stream())
                .collect(Collectors.toList());
    }

    public int getCraftableAmount(Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        var total = 0;
        var modules = this.streamModules().iterator();
        while (modules.hasNext()) {
            var module = modules.next();
            // make sure we don't factor in recursive dependencies like ingot -> block -> ingot etc.
            if (!dependencyChain.contains(module.getLeft())) {
                var amount = module.getRight().getCraftableAmount(module.getLeft(), this, unavailableConsumer, stack, dependencyChain);
                if (amount > 0)
                    total += amount;
            }
        }
        return total;
    }

    public ItemStack craft(BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        var modules = this.streamModules().iterator();
        while (modules.hasNext()) {
            var module = modules.next();
            stack = module.getRight().craft(module.getLeft(), this, destPipe, unavailableConsumer, stack, dependencyChain);
            if (stack.isEmpty())
                break;
        }
        return stack;
    }

    public IItemHandler getItemHandler(Direction dir) {
        var handler = this.getNeighborCap(dir, Capabilities.ITEM_HANDLER);
        if (handler != null)
            return handler;
        return Utility.getBlockItemHandler(this.level, this.worldPosition.relative(dir), dir.getOpposite());
    }

    public <T> T getNeighborCap(Direction dir, Capability<T> cap) {
        if (!this.isConnected(dir))
            return null;
        var pos = this.worldPosition.relative(dir);
        var tile = this.level.getBlockEntity(pos);
        if (tile != null)
            return tile.getCapability(cap, dir.getOpposite()).orElse(null);
        return null;
    }

    public IPipeConnectable getPipeConnectable(Direction dir) {
        var tile = this.level.getBlockEntity(this.worldPosition.relative(dir));
        if (tile != null)
            return tile.getCapability(Registry.pipeConnectableCapability, dir.getOpposite()).orElse(null);
        return null;
    }

    public boolean canHaveModules() {
        for (var dir : Direction.values()) {
            if (this.getItemHandler(dir) != null)
                return true;
            var connectable = this.getPipeConnectable(dir);
            if (connectable != null && connectable.allowsModules(this.worldPosition, dir))
                return true;
        }
        return false;
    }

    public boolean canNetworkSee(Direction direction, IItemHandler handler) {
        return this.streamModules().allMatch(m -> m.getRight().canNetworkSee(m.getLeft(), this, direction, handler));
    }

    public Stream<Pair<ItemStack, IModule>> streamModules() {
        Stream.Builder<Pair<ItemStack, IModule>> builder = Stream.builder();
        for (var i = 0; i < this.modules.getSlots(); i++) {
            var stack = this.modules.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IModule module)
                builder.accept(Pair.of(stack, module));
        }
        return builder.build();
    }

    public void removeCover(Player player, InteractionHand hand) {
        if (this.level.isClientSide)
            return;
        var drops = Block.getDrops(this.cover, (ServerLevel) this.level, this.worldPosition, null, player, player.getItemInHand(hand));
        for (var drop : drops)
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), drop);
        this.cover = null;
    }

    public boolean shouldWorkNow(int speed) {
        return (this.level.getGameTime() + this.workRandomizer.get()) % speed == 0;
    }

    public int getNextNode(List<BlockPos> nodes, int index) {
        return this.streamModules()
                .map(m -> m.getRight().getCustomNextNode(m.getLeft(), this, nodes, index))
                .filter(m -> m != null && m >= 0).findFirst().orElse(index);
    }

    public List<ItemFilter> getFilters(Direction direction) {
        return this.streamModules().map(p -> {
            if (direction != null) {
                var dir = p.getRight().getDirectionSelector(p.getLeft(), this);
                if (dir != null && !dir.has(direction))
                    return null;
            }
            return p.getRight().getItemFilter(p.getLeft(), this);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.getItems().clear();
        var network = PipeNetwork.get(this.level);
        for (var lock : this.craftIngredientRequests)
            network.resolveNetworkLock(lock);
        this.lazyThis.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + PrettyPipes.ID + ".pipe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new MainPipeContainer(Registry.pipeContainer, window, player, PipeBlockEntity.this.worldPosition);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        // our render bounding box should always be the full block in case we're covered
        return new AABB(this.worldPosition);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == Registry.pipeConnectableCapability)
            return this.lazyThis.cast();
        return LazyOptional.empty();
    }

    @Override
    public ConnectionType getConnectionType(BlockPos pipePos, Direction direction) {
        var state = this.level.getBlockState(pipePos.relative(direction));
        if (state.getValue(PipeBlock.DIRECTIONS.get(direction.getOpposite())) == ConnectionType.BLOCKED)
            return ConnectionType.BLOCKED;
        return ConnectionType.CONNECTED;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity pipe) {
        // invalidate our pressurizer reference if it was removed
        if (pipe.pressurizer != null && pipe.pressurizer.isRemoved())
            pipe.pressurizer = null;

        if (!pipe.level.isAreaLoaded(pipe.worldPosition, 1))
            return;
        var profiler = pipe.level.getProfiler();

        if (!pipe.level.isClientSide) {
            // drop modules here to give a bit of time for blocks to update (iron -> gold chest etc.)
            if (pipe.moduleDropCheck > 0) {
                pipe.moduleDropCheck--;
                if (pipe.moduleDropCheck <= 0 && !pipe.canHaveModules())
                    Utility.dropInventory(pipe, pipe.modules);
            }

            profiler.push("ticking_modules");
            var prio = 0;
            var modules = pipe.streamModules().iterator();
            while (modules.hasNext()) {
                var module = modules.next();
                module.getRight().tick(module.getLeft(), pipe);
                prio += module.getRight().getPriority(module.getLeft(), pipe);
            }
            if (prio != pipe.priority) {
                pipe.priority = prio;
                // clear the cache so that it's reevaluated based on priority
                PipeNetwork.get(pipe.level).clearDestinationCache(Collections.singletonList(pipe.worldPosition));
            }
            profiler.pop();
        }

        profiler.push("ticking_items");
        var items = pipe.getItems();
        for (var i = items.size() - 1; i >= 0; i--)
            items.get(i).updateInPipe(pipe);
        if (items.size() != pipe.lastItemAmount) {
            pipe.lastItemAmount = items.size();
            pipe.level.updateNeighbourForOutputSignal(pipe.worldPosition, pipe.getBlockState().getBlock());
        }
        profiler.pop();
    }

}
