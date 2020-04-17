package de.ellpeck.prettypipes.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.modules.containers.MainPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.profiler.IProfiler;
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
    public final List<PipeItem> items = new ArrayList<>();
    private int priority;

    public PipeTileEntity() {
        super(Registry.pipeTileEntity);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("modules", this.modules.serializeNBT());
        ListNBT list = new ListNBT();
        for (PipeItem item : this.items)
            list.add(item.serializeNBT());
        compound.put("items", list);
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.modules.deserializeNBT(compound.getCompound("modules"));
        this.items.clear();
        ListNBT list = compound.getList("items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.items.add(new PipeItem(list.getCompound(i)));
        super.read(compound);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        // by default, this is just writeInternal, but we
        // want to sync the current pipe items on load too
        return this.write(new CompoundNBT());
    }

    @Override
    public void tick() {
        if (!this.world.isAreaLoaded(this.pos, 1))
            return;
        IProfiler profiler = this.world.getProfiler();

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

        profiler.startSection("ticking_items");
        for (int i = this.items.size() - 1; i >= 0; i--)
            this.items.get(i).updateInPipe(this);
        profiler.endSection();
    }

    public boolean isConnected(Direction dir) {
        return this.getBlockState().get(PipeBlock.DIRECTIONS.get(dir)).isConnected();
    }

    public BlockPos getAvailableDestination(ItemStack stack, boolean internal) {
        if (!internal && this.streamModules().anyMatch(m -> !m.getRight().canAcceptItem(m.getLeft(), this, stack)))
            return null;
        for (Direction dir : Direction.values()) {
            IItemHandler handler = this.getItemHandler(dir);
            if (handler == null)
                continue;
            if (!ItemHandlerHelper.insertItem(handler, stack, true).isEmpty())
                continue;
            if (this.streamModules().anyMatch(m -> !m.getRight().isAvailableDestination(m.getLeft(), this, stack, handler)))
                continue;
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

    public IItemHandler getItemHandler(Direction dir) {
        if (!this.isConnected(dir))
            return null;
        TileEntity tile = this.world.getTileEntity(this.pos.offset(dir));
        if (tile == null)
            return null;
        return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).orElse(null);
    }

    public boolean isConnectedInventory(Direction dir) {
        return this.getItemHandler(dir) != null;
    }

    public boolean isConnectedInventory() {
        return Arrays.stream(Direction.values()).anyMatch(this::isConnectedInventory);
    }

    private Stream<Pair<ItemStack, IModule>> streamModules() {
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
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".pipe");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new MainPipeContainer(Registry.pipeContainer, window, player, PipeTileEntity.this.pos);
    }
}
