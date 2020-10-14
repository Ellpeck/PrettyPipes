package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.misc.ItemOrder;
import de.ellpeck.prettypipes.network.*;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketNetworkItems;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ItemTerminalTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity {

    public final ItemStackHandler items = new ItemStackHandler(12) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    };
    public Map<EquatableItemStack, NetworkItem> networkItems;
    public List<Pair<BlockPos, ItemStack>> craftables;
    private final Queue<NetworkLock> pendingRequests = new ArrayDeque<>();

    protected ItemTerminalTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public ItemTerminalTileEntity() {
        this(Registry.itemTerminalTileEntity);
    }

    @Override
    public void tick() {
        if (this.world.isRemote)
            return;
        PipeNetwork network = PipeNetwork.get(this.world);
        PipeTileEntity pipe = this.getConnectedPipe();
        if (pipe == null)
            return;

        boolean update = false;
        int interval = pipe.pressurizer != null ? 2 : 10;
        if (this.world.getGameTime() % interval == 0) {
            for (int i = 6; i < 12; i++) {
                ItemStack extracted = this.items.extractItem(i, Integer.MAX_VALUE, true);
                if (extracted.isEmpty())
                    continue;
                ItemStack remain = network.tryInsertItem(pipe.getPos(), this.pos, extracted, true);
                if (remain.getCount() == extracted.getCount())
                    continue;
                this.items.extractItem(i, extracted.getCount() - remain.getCount(), false);
                break;
            }

            if (!this.pendingRequests.isEmpty()) {
                NetworkLock request = this.pendingRequests.remove();
                network.resolveNetworkLock(request);
                network.requestItem(request.location, pipe.getPos(), this.pos, request.stack, ItemEqualityType.NBT);
                update = true;
            }
        }

        if (this.world.getGameTime() % 100 == 0 || update) {
            PlayerEntity[] lookingPlayers = this.getLookingPlayers();
            if (lookingPlayers.length > 0)
                this.updateItems(lookingPlayers);
        }
    }

    @Override
    public void remove() {
        super.remove();
        PipeNetwork network = PipeNetwork.get(this.world);
        for (NetworkLock lock : this.pendingRequests)
            network.resolveNetworkLock(lock);
    }

    public PipeTileEntity getConnectedPipe() {
        PipeNetwork network = PipeNetwork.get(this.world);
        for (Direction dir : Direction.values()) {
            PipeTileEntity pipe = network.getPipe(this.pos.offset(dir));
            if (pipe != null)
                return pipe;
        }
        return null;
    }

    public void updateItems(PlayerEntity... playersToSync) {
        PipeTileEntity pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        this.networkItems = this.collectItems();
        this.craftables = PipeNetwork.get(this.world).getOrderedCraftables(pipe.getPos());
        if (playersToSync.length > 0) {
            List<ItemStack> clientItems = this.networkItems.values().stream().map(NetworkItem::asStack).collect(Collectors.toList());
            List<ItemStack> clientCraftables = this.craftables.stream().map(Pair::getRight).collect(Collectors.toList());
            for (PlayerEntity player : playersToSync) {
                if (!(player.openContainer instanceof ItemTerminalContainer))
                    continue;
                ItemTerminalTileEntity tile = ((ItemTerminalContainer) player.openContainer).tile;
                if (tile != this)
                    continue;
                PacketHandler.sendTo(player, new PacketNetworkItems(clientItems, clientCraftables));
            }
        }
    }

    public void requestItem(PlayerEntity player, ItemStack stack) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_item");
        this.updateItems();
        int requested = this.requestItemImpl(stack);
        if (requested > 0) {
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".sending", requested, stack.getDisplayName()).setStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN)), UUID.randomUUID());
        } else {
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".not_found", stack.getDisplayName()).setStyle(Style.EMPTY.setFormatting(TextFormatting.RED)), UUID.randomUUID());
        }
        network.endProfile();
    }

    protected int requestItemImpl(ItemStack stack) {
        PipeNetwork network = PipeNetwork.get(this.world);
        EquatableItemStack equatable = new EquatableItemStack(stack);
        NetworkItem item = this.networkItems.get(equatable);
        if (item != null) {
            int remain = stack.getCount();
            for (NetworkLocation location : item.getLocations()) {
                int amount = location.getItemAmount(this.world, stack, ItemEqualityType.NBT);
                if (amount <= 0)
                    continue;
                amount -= network.getLockedAmount(location.getPos(), stack, ItemEqualityType.NBT);
                if (amount > 0) {
                    if (remain < amount)
                        amount = remain;
                    remain -= amount;
                    while (amount > 0) {
                        ItemStack copy = stack.copy();
                        copy.setCount(Math.min(stack.getMaxStackSize(), amount));
                        NetworkLock lock = new NetworkLock(location, copy);
                        this.pendingRequests.add(lock);
                        network.createNetworkLock(lock);
                        amount -= copy.getCount();

                    }
                    if (remain <= 0)
                        break;
                }
            }
            return stack.getCount() - remain;
        }
        return 0;
    }

    protected PlayerEntity[] getLookingPlayers() {
        return this.world.getPlayers().stream()
                .filter(p -> p.openContainer instanceof ItemTerminalContainer)
                .filter(p -> ((ItemTerminalContainer) p.openContainer).tile == this)
                .toArray(PlayerEntity[]::new);
    }

    private Map<EquatableItemStack, NetworkItem> collectItems() {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_collect_items");
        PipeTileEntity pipe = this.getConnectedPipe();
        Map<EquatableItemStack, NetworkItem> items = new HashMap<>();
        for (NetworkLocation location : network.getOrderedNetworkItems(pipe.getPos())) {
            for (ItemStack stack : location.getItems(this.world).values()) {
                EquatableItemStack equatable = new EquatableItemStack(stack);
                NetworkItem item = items.computeIfAbsent(equatable, NetworkItem::new);
                item.add(location, stack);
            }
        }
        network.endProfile();
        return items;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("items", this.items.serializeNBT());
        compound.put("requests", Utility.serializeAll(this.pendingRequests));
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        this.items.deserializeNBT(compound.getCompound("items"));
        this.pendingRequests.clear();
        this.pendingRequests.addAll(Utility.deserializeAll(compound.getList("requests", NBT.TAG_COMPOUND), NetworkLock::new));
        super.read(state, compound);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".item_terminal");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new ItemTerminalContainer(Registry.itemTerminalContainer, window, player, this.pos);
    }
}
