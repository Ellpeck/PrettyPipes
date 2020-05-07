package de.ellpeck.prettypipes.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.containers.MainPipeContainer;
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
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;
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
    private List<PipeItem> items;
    private int lastItemAmount;
    private int priority;

    public PipeTileEntity() {
        super(Registry.pipeTileEntity);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("modules", this.modules.serializeNBT());
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.modules.deserializeNBT(compound.getCompound("modules"));
        super.read(compound);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        // sync pipe items on load
        CompoundNBT nbt = this.write(new CompoundNBT());
        nbt.put("items", PipeItem.serializeAll(this.getItems()));
        return nbt;
    }

    @Override
    public void handleUpdateTag(CompoundNBT nbt) {
        this.read(nbt);
        List<PipeItem> items = this.getItems();
        items.clear();
        items.addAll(PipeItem.deserializeAll(nbt.getList("items", Constants.NBT.TAG_COMPOUND)));
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

    public boolean isConnected(Direction dir) {
        return this.getBlockState().get(PipeBlock.DIRECTIONS.get(dir)).isConnected();
    }

    public BlockPos getAvailableDestination(ItemStack stack, boolean internal, boolean preventOversending) {
        if (!this.canWork())
            return null;
        if (!internal && this.streamModules().anyMatch(m -> !m.getRight().canAcceptItem(m.getLeft(), this, stack)))
            return null;
        for (Direction dir : Direction.values()) {
            IItemHandler handler = this.getItemHandler(dir);
            if (handler == null)
                continue;
            if (!ItemHandlerHelper.insertItem(handler, stack, true).isEmpty())
                continue;
            int maxAmount = this.streamModules().mapToInt(m -> m.getRight().getMaxInsertionAmount(m.getLeft(), this, stack, handler)).min().orElse(Integer.MAX_VALUE);
            if (maxAmount < stack.getCount())
                continue;
            if (preventOversending || maxAmount < Integer.MAX_VALUE) {
                // these are the items that are currently in the pipes, going to this pipe
                int onTheWay = PipeNetwork.get(this.world).getItemsOnTheWay(this.pos, stack);
                if (onTheWay > 0) {
                    // check if any modules are limiting us
                    if (onTheWay + stack.getCount() > maxAmount)
                        continue;
                    ItemStack copy = stack.copy();
                    copy.setCount(copy.getMaxStackSize());
                    // totalSpace will be the amount of items that fit into the attached container
                    int totalSpace = 0;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack remain = handler.insertItem(i, copy, true);
                        totalSpace += copy.getMaxStackSize() - remain.getCount();
                    }
                    // if the items on the way plus the items we're trying to move are too much, abort
                    if (onTheWay + stack.getCount() > totalSpace)
                        continue;
                }
            }
            return this.pos.offset(dir);
        }
        return null;
    }

    public int getPriority() {
        return this.priority;
    }

    public float getItemSpeed() {
        float speed = (float) this.streamModules().mapToDouble(m -> m.getRight().getItemSpeedIncrease(m.getLeft(), this)).sum();
        return 0.05F + speed;
    }

    public boolean canWork() {
        return this.streamModules().allMatch(m -> m.getRight().canPipeWork(m.getLeft(), this));
    }

    public IItemHandler getItemHandler(Direction dir) {
        if (!this.isConnected(dir))
            return null;
        TileEntity tile = this.world.getTileEntity(this.pos.offset(dir));
        if (tile == null)
            return null;
        // if we don't do this, then chests get really weird
        if (tile instanceof ChestTileEntity) {
            BlockState state = this.world.getBlockState(tile.getPos());
            if (state.getBlock() instanceof ChestBlock)
                return new InvWrapper(ChestBlock.func_226916_a_((ChestBlock) state.getBlock(), state, this.world, tile.getPos(), true));
        }
        return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).orElse(null);
    }

    public boolean isConnectedInventory(Direction dir) {
        return this.getItemHandler(dir) != null;
    }

    public boolean isConnectedInventory() {
        return Arrays.stream(Direction.values()).anyMatch(this::isConnectedInventory);
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
}
