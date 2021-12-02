package de.ellpeck.prettypipes.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.containers.MainPipeContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.profiler.IProfiler;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipeTileEntity extends BlockEntity implements INamedContainerProvider, ITickableTileEntity, IPipeConnectable {

    public final ItemStackHandler modules = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            var item = stack.getItem();
            if (!(item instanceof IModule module))
                return false;
            return PipeTileEntity.this.streamModules().allMatch(m -> module.isCompatible(stack, PipeTileEntity.this, m.getRight()) && m.getRight().isCompatible(m.getLeft(), PipeTileEntity.this, module));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
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
    private final LazyOptional<PipeTileEntity> lazyThis = LazyOptional.of(() -> this);
    private final Lazy<Integer> workRandomizer = Lazy.of(() -> this.level.random.nextInt(200));

    @Override
    public void onChunkUnloaded() {
        PipeNetwork.get(this.level).uncachePipe(this.worldPosition);
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.put("modules", this.modules.serializeNBT());
        compound.putInt("module_drop_check", this.moduleDropCheck);
        compound.put("requests", Utility.serializeAll(this.craftIngredientRequests));
        if (this.cover != null)
            compound.put("cover", NbtUtils.writeBlockState(this.cover));
        ListTag results = new ListTag();
        for (Pair<BlockPos, ItemStack> triple : this.craftResultRequests) {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("dest_pipe", triple.getLeft().asLong());
            nbt.put("item", triple.getRight().serializeNBT());
            results.add(nbt);
        }
        compound.put("craft_results", results);
        return super.save(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        this.modules.deserializeNBT(compound.getCompound("modules"));
        this.moduleDropCheck = compound.getInt("module_drop_check");
        this.cover = compound.contains("cover") ? NbtUtils.readBlockState(compound.getCompound("cover")) : null;
        this.craftIngredientRequests.clear();
        this.craftIngredientRequests.addAll(Utility.deserializeAll(compound.getList("requests", NBT.TAG_COMPOUND), NetworkLock::new));
        this.craftResultRequests.clear();
        ListTag results = compound.getList("craft_results", NBT.TAG_COMPOUND);
        for (int i = 0; i < results.size(); i++) {
            CompoundTag nbt = results.getCompound(i);
            this.craftResultRequests.add(Pair.of(
                    BlockPos.of(nbt.getLong("dest_pipe")),
                    ItemStack.of(nbt.getCompound("item"))));
        }
        super.load(compound);
    }

    @Override
    public CompoundTag getUpdateTag() {
        // sync pipe items on load
        CompoundTag nbt = this.write(new CompoundTag());
        nbt.put("items", Utility.serializeAll(this.getItems()));
        return nbt;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundTag nbt) {
        this.read(state, nbt);
        List<IPipeItem> items = this.getItems();
        items.clear();
        items.addAll(Utility.deserializeAll(nbt.getList("items", NBT.TAG_COMPOUND), IPipeItem::load));
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        this.read(this.getBlockState(), pkt.getNbtCompound());
    }

    @Override
    public void tick() {
        // invalidate our pressurizer reference if it was removed
        if (this.pressurizer != null && this.pressurizer.isRemoved())
            this.pressurizer = null;

        if (!this.world.isAreaLoaded(this.pos, 1))
            return;
        IProfiler profiler = this.world.getProfiler();

        if (!this.world.isRemote) {
            // drop modules here to give a bit of time for blocks to update (iron -> gold chest etc.)
            if (this.moduleDropCheck > 0) {
                this.moduleDropCheck--;
                if (this.moduleDropCheck <= 0 && !this.canHaveModules())
                    Utility.dropInventory(this, this.modules);
            }

            profiler.startSection("ticking_modules");
            int prio = 0;
            Iterator<Pair<ItemStack, IModule>> modules = this.streamModules().iterator();
            while (modules.hasNext()) {
                Pair<ItemStack, IModule> module = modules.next();
                module.getRight().tick(module.getLeft(), this);
                prio += module.getRight().getPriority(module.getLeft(), this);
            }
            if (prio != this.priority) {
                this.priority = prio;
                // clear the cache so that it's reevaluated based on priority
                PipeNetwork.get(this.world).clearDestinationCache(this.pos);
            }
            profiler.endSection();
        }

        profiler.startSection("ticking_items");
        List<IPipeItem> items = this.getItems();
        for (int i = items.size() - 1; i >= 0; i--)
            items.get(i).updateInPipe(this);
        if (items.size() != this.lastItemAmount) {
            this.lastItemAmount = items.size();
            this.world.updateComparatorOutputLevel(this.pos, this.getBlockState().getBlock());
        }
        profiler.endSection();
    }

    public List<IPipeItem> getItems() {
        if (this.items == null)
            this.items = PipeNetwork.get(this.world).getItemsInPipe(this.pos);
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
        return this.getBlockState().get(PipeBlock.DIRECTIONS.get(dir)).isConnected();
    }

    public Pair<BlockPos, ItemStack> getAvailableDestinationOrConnectable(ItemStack stack, boolean force, boolean preventOversending) {
        Pair<BlockPos, ItemStack> dest = this.getAvailableDestination(stack, force, preventOversending);
        if (dest != null)
            return dest;
        // if there's no available destination, try inserting into terminals etc.
        for (Direction dir : Direction.values()) {
            IPipeConnectable connectable = this.getPipeConnectable(dir);
            if (connectable == null)
                continue;
            ItemStack connectableRemain = connectable.insertItem(this.getPos(), dir, stack, true);
            if (connectableRemain.getCount() != stack.getCount()) {
                ItemStack inserted = stack.copy();
                inserted.shrink(connectableRemain.getCount());
                return Pair.of(this.getPos().offset(dir), inserted);
            }
        }
        return null;
    }

    public Pair<BlockPos, ItemStack> getAvailableDestination(ItemStack stack, boolean force, boolean preventOversending) {
        if (!this.canWork())
            return null;
        if (!force && this.streamModules().anyMatch(m -> !m.getRight().canAcceptItem(m.getLeft(), this, stack)))
            return null;
        for (Direction dir : Direction.values()) {
            IItemHandler handler = this.getItemHandler(dir);
            if (handler == null)
                continue;
            ItemStack remain = ItemHandlerHelper.insertItem(handler, stack, true);
            // did we insert anything?
            if (remain.getCount() == stack.getCount())
                continue;
            ItemStack toInsert = stack.copy();
            toInsert.shrink(remain.getCount());
            // limit to the max amount that modules allow us to insert
            int maxAmount = this.streamModules().mapToInt(m -> m.getRight().getMaxInsertionAmount(m.getLeft(), this, stack, handler)).min().orElse(Integer.MAX_VALUE);
            if (maxAmount < toInsert.getCount())
                toInsert.setCount(maxAmount);
            BlockPos offset = this.pos.offset(dir);
            if (preventOversending || maxAmount < Integer.MAX_VALUE) {
                PipeNetwork network = PipeNetwork.get(this.world);
                // these are the items that are currently in the pipes, going to this inventory
                int onTheWay = network.getItemsOnTheWay(offset, null);
                if (onTheWay > 0) {
                    if (maxAmount < Integer.MAX_VALUE) {
                        // these are the items on the way, limited to items of the same type as stack
                        int onTheWaySame = network.getItemsOnTheWay(offset, stack);
                        // check if any modules are limiting us
                        if (toInsert.getCount() + onTheWaySame > maxAmount)
                            toInsert.setCount(maxAmount - onTheWaySame);
                    }
                    // totalSpace will be the amount of items that fit into the attached container
                    int totalSpace = 0;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack copy = stack.copy();
                        int maxStackSize = copy.getMaxStackSize();
                        // if the container can store more than 64 items in this slot, then it's likely
                        // a barrel or similar, meaning that the slot limit matters more than the max stack size
                        int limit = handler.getSlotLimit(i);
                        if (limit > 64)
                            maxStackSize = limit;
                        copy.setCount(maxStackSize);
                        // this is an inaccurate check since it ignores the fact that some slots might
                        // have space for items of other types, but it'll be good enough for us
                        ItemStack left = handler.insertItem(i, copy, true);
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
        float moduleSpeed = (float) this.streamModules().mapToDouble(m -> m.getRight().getItemSpeedIncrease(m.getLeft(), this)).sum();
        float pressureSpeed = this.pressurizer != null && this.pressurizer.pressurizeItem(stack, true) ? 0.45F : 0;
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
        int total = 0;
        Iterator<Pair<ItemStack, IModule>> modules = this.streamModules().iterator();
        while (modules.hasNext()) {
            Pair<ItemStack, IModule> module = modules.next();
            // make sure we don't factor in recursive dependencies like ingot -> block -> ingot etc.
            if (dependencyChain.stream().noneMatch(d -> ItemEquality.compareItems(module.getLeft(), d, ItemEquality.NBT))) {
                int amount = module.getRight().getCraftableAmount(module.getLeft(), this, unavailableConsumer, stack, dependencyChain);
                if (amount > 0)
                    total += amount;
            }
        }
        return total;
    }

    public ItemStack craft(BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain) {
        Iterator<Pair<ItemStack, IModule>> modules = this.streamModules().iterator();
        while (modules.hasNext()) {
            Pair<ItemStack, IModule> module = modules.next();
            stack = module.getRight().craft(module.getLeft(), this, destPipe, unavailableConsumer, stack, dependencyChain);
            if (stack.isEmpty())
                break;
        }
        return stack;
    }

    public IItemHandler getItemHandler(Direction dir) {
        IItemHandler handler = this.getNeighborCap(dir, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        if (handler != null)
            return handler;
        return Utility.getBlockItemHandler(this.world, this.pos.offset(dir), dir.getOpposite());
    }

    public <T> T getNeighborCap(Direction dir, Capability<T> cap) {
        if (!this.isConnected(dir))
            return null;
        BlockPos pos = this.pos.offset(dir);
        TileEntity tile = this.world.getTileEntity(pos);
        if (tile != null)
            return tile.getCapability(cap, dir.getOpposite()).orElse(null);
        return null;
    }

    public IPipeConnectable getPipeConnectable(Direction dir) {
        TileEntity tile = this.world.getTileEntity(this.pos.offset(dir));
        if (tile != null)
            return tile.getCapability(Registry.pipeConnectableCapability, dir.getOpposite()).orElse(null);
        return null;
    }

    public boolean isConnectedInventory(Direction dir) {
        return this.getItemHandler(dir) != null;
    }

    public boolean canHaveModules() {
        for (Direction dir : Direction.values()) {
            if (this.isConnectedInventory(dir))
                return true;
            IPipeConnectable connectable = this.getPipeConnectable(dir);
            if (connectable != null && connectable.allowsModules(this.pos, dir))
                return true;
        }
        return false;
    }

    public boolean canNetworkSee() {
        return this.streamModules().allMatch(m -> m.getRight().canNetworkSee(m.getLeft(), this));
    }

    public Stream<Pair<ItemStack, IModule>> streamModules() {
        Stream.Builder<Pair<ItemStack, IModule>> builder = Stream.builder();
        for (int i = 0; i < this.modules.getSlots(); i++) {
            ItemStack stack = this.modules.getStackInSlot(i);
            if (stack.isEmpty())
                continue;
            builder.accept(Pair.of(stack, (IModule) stack.getItem()));
        }
        return builder.build();
    }

    public void removeCover(PlayerEntity player, Hand hand) {
        if (this.world.isRemote)
            return;
        List<ItemStack> drops = Block.getDrops(this.cover, (ServerWorld) this.world, this.pos, null, player, player.getHeldItem(hand));
        for (ItemStack drop : drops)
            Block.spawnAsEntity(this.world, this.pos, drop);
        this.cover = null;
    }

    public boolean shouldWorkNow(int speed) {
        return (this.world.getGameTime() + this.workRandomizer.get()) % speed == 0;
    }

    public int getNextNode(List<BlockPos> nodes, int index) {
        return this.streamModules()
                .map(m -> m.getRight().getCustomNextNode(m.getLeft(), this, nodes, index))
                .filter(Objects::nonNull).findFirst().orElse(index);
    }

    public List<ItemFilter> getFilters() {
        return this.streamModules()
                .map(p -> p.getRight().getItemFilter(p.getLeft(), this))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void remove() {
        super.remove();
        this.getItems().clear();
        PipeNetwork network = PipeNetwork.get(this.world);
        for (NetworkLock lock : this.craftIngredientRequests)
            network.resolveNetworkLock(lock);
        this.lazyThis.invalidate();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".pipe");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new MainPipeContainer(Registry.pipeContainer, window, player, PipeTileEntity.this.pos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        // our render bounding box should always be the full block in case we're covered
        return new AxisAlignedBB(this.pos);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == Registry.pipeConnectableCapability)
            return this.lazyThis.cast();
        return LazyOptional.empty();
    }

    @Override
    public ConnectionType getConnectionType(BlockPos pipePos, Direction direction) {
        BlockState state = this.world.getBlockState(pipePos.offset(direction));
        if (state.get(PipeBlock.DIRECTIONS.get(direction.getOpposite())) == ConnectionType.BLOCKED)
            return ConnectionType.BLOCKED;
        return ConnectionType.CONNECTED;
    }
}
