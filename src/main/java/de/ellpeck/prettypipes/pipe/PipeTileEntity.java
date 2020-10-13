package de.ellpeck.prettypipes.pipe;

import com.google.common.base.Predicates;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.containers.MainPipeContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlock;
import de.ellpeck.prettypipes.pressurizer.PressurizerTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.profiler.IProfiler;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class PipeTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity {

    public final ItemStackHandler modules = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            Item item = stack.getItem();
            if (!(item instanceof IModule))
                return false;
            IModule module = (IModule) item;
            return PipeTileEntity.this.streamModules().allMatch(m -> module.isCompatible(stack, PipeTileEntity.this, m.getRight()) && m.getRight().isCompatible(m.getLeft(), PipeTileEntity.this, module));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    protected List<PipeItem> items;
    private PressurizerTileEntity pressurizer;
    private int lastItemAmount;
    private int priority;

    public PipeTileEntity() {
        this(Registry.pipeTileEntity);
    }

    protected PipeTileEntity(TileEntityType<?> type) {
        super(type);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("modules", this.modules.serializeNBT());
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        this.modules.deserializeNBT(compound.getCompound("modules"));
        super.read(state, compound);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        // sync pipe items on load
        CompoundNBT nbt = this.write(new CompoundNBT());
        nbt.put("items", Utility.serializeAll(this.getItems()));
        return nbt;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
        this.read(state, nbt);
        List<PipeItem> items = this.getItems();
        items.clear();
        items.addAll(Utility.deserializeAll(nbt.getList("items", NBT.TAG_COMPOUND), PipeItem::new));
    }

    @Override
    public void tick() {
        if (!this.world.isAreaLoaded(this.pos, 1))
            return;
        IProfiler profiler = this.world.getProfiler();

        if (!this.world.isRemote) {
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

            if (this.world.getGameTime() % 40 == 0) {
                profiler.startSection("caching_data");
                // figure out if we're pressurized
                this.pressurizer = this.findPressurizer();
                profiler.endSection();
            }
        }

        profiler.startSection("ticking_items");
        List<PipeItem> items = this.getItems();
        for (int i = items.size() - 1; i >= 0; i--)
            items.get(i).updateInPipe(this);
        if (items.size() != this.lastItemAmount) {
            this.lastItemAmount = items.size();
            this.world.updateComparatorOutputLevel(this.pos, this.getBlockState().getBlock());
        }
        profiler.endSection();
    }

    public List<PipeItem> getItems() {
        if (this.items == null)
            this.items = PipeNetwork.get(this.world).getItemsInPipe(this.pos);
        return this.items;
    }

    public void addNewItem(PipeItem item) {
        // an item might be re-routed from a previous location, but it should still count as a new item then
        if (!this.getItems().contains(item))
            this.getItems().add(item);
        if (this.pressurizer != null && !this.pressurizer.isRemoved())
            this.pressurizer.pressurizeItem(item.stack, false);
    }

    public boolean isConnected(Direction dir) {
        return this.getBlockState().get(PipeBlock.DIRECTIONS.get(dir)).isConnected();
    }

    public Pair<BlockPos, ItemStack> getAvailableDestination(ItemStack stack, boolean force, boolean preventOversending) {
        if (!this.canWork())
            return null;
        if (!force && this.streamModules().anyMatch(m -> !m.getRight().canAcceptItem(m.getLeft(), this, stack)))
            return null;
        for (Direction dir : Direction.values()) {
            IItemHandler handler = this.getItemHandler(dir, null);
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
                    ItemStack copy = stack.copy();
                    copy.setCount(copy.getMaxStackSize());
                    // totalSpace will be the amount of items that fit into the attached container
                    int totalSpace = 0;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        // this is an inaccurate check since it ignores the fact that some slots might
                        // have space for items of other types, but it'll be good enough for us
                        ItemStack left = handler.insertItem(i, copy, true);
                        totalSpace += copy.getMaxStackSize() - left.getCount();
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
        float pressureSpeed = this.pressurizer != null && !this.pressurizer.isRemoved() && this.pressurizer.pressurizeItem(stack, true) ? 0.45F : 0;
        return 0.05F + moduleSpeed + pressureSpeed;
    }

    public boolean canWork() {
        return this.streamModules().allMatch(m -> m.getRight().canPipeWork(m.getLeft(), this));
    }

    public IItemHandler getItemHandler(Direction dir, PipeItem item) {
        if (!this.isConnected(dir))
            return null;
        BlockPos pos = this.pos.offset(dir);
        TileEntity tile = this.world.getTileEntity(pos);
        if (tile != null) {
            // if we don't do this, then chests get really weird
            if (tile instanceof ChestTileEntity) {
                BlockState state = this.world.getBlockState(tile.getPos());
                if (state.getBlock() instanceof ChestBlock)
                    return new InvWrapper(ChestBlock.getChestInventory((ChestBlock) state.getBlock(), state, this.world, tile.getPos(), true));
            }
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).orElse(null);
            if (handler != null)
                return handler;
        }
        IPipeConnectable connectable = this.getPipeConnectable(dir);
        if (connectable != null)
            return connectable.getItemHandler(this.world, this.pos, dir, item);
        return null;
    }

    public IPipeConnectable getPipeConnectable(Direction dir) {
        BlockState state = this.world.getBlockState(this.pos.offset(dir));
        if (state.getBlock() instanceof IPipeConnectable)
            return (IPipeConnectable) state.getBlock();
        return null;
    }

    public boolean isConnectedInventory(Direction dir) {
        return this.getItemHandler(dir, null) != null;
    }

    public boolean canHaveModules() {
        for (Direction dir : Direction.values()) {
            if (this.isConnectedInventory(dir))
                return true;
            IPipeConnectable connectable = this.getPipeConnectable(dir);
            if (connectable != null && connectable.allowsModules(this.world, this.pos, dir))
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

    @Override
    public void remove() {
        super.remove();
        this.getItems().clear();
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

    private PressurizerTileEntity findPressurizer() {
        PipeNetwork network = PipeNetwork.get(this.world);
        for (BlockPos node : network.getOrderedNetworkNodes(this.pos)) {
            PipeTileEntity pipe = network.getPipe(node);
            for (Direction dir : Direction.values()) {
                IPipeConnectable connectable = pipe.getPipeConnectable(dir);
                if (connectable instanceof PressurizerBlock)
                    return Utility.getTileEntity(PressurizerTileEntity.class, this.world, node.offset(dir));
            }
        }
        return null;
    }
}
