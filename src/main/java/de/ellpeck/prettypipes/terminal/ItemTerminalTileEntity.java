package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketNetworkItems;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemTerminalTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity, IPipeConnectable {

    public final ItemStackHandler items = new ItemStackHandler(12) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    };
    protected Map<EquatableItemStack, NetworkItem> networkItems;
    private final Queue<NetworkLock> existingRequests = new LinkedList<>();
    private final LazyOptional<IPipeConnectable> lazyThis = LazyOptional.of(() -> this);

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
                ItemStack remain = network.routeItem(pipe.getPos(), this.pos, extracted, true);
                if (remain.getCount() == extracted.getCount())
                    continue;
                this.items.extractItem(i, extracted.getCount() - remain.getCount(), false);
                break;
            }

            if (!this.existingRequests.isEmpty()) {
                NetworkLock request = this.existingRequests.remove();
                network.resolveNetworkLock(request);
                network.requestExistingItem(request.location, pipe.getPos(), this.pos, request, request.stack, ItemEquality.NBT);
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
        for (NetworkLock lock : this.existingRequests)
            network.resolveNetworkLock(lock);
        this.lazyThis.invalidate();
    }

    public String getInvalidTerminalReason() {
        PipeNetwork network = PipeNetwork.get(this.world);
        long pipes = Arrays.stream(Direction.values())
                .map(d -> network.getPipe(this.pos.offset(d)))
                .filter(Objects::nonNull).count();
        if (pipes <= 0)
            return "info." + PrettyPipes.ID + ".no_pipe_connected";
        if (pipes > 1)
            return "info." + PrettyPipes.ID + ".too_many_pipes_connected";
        return null;
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
        this.networkItems = this.collectItems(ItemEquality.NBT);
        if (playersToSync.length > 0) {
            List<ItemStack> clientItems = this.networkItems.values().stream().map(NetworkItem::asStack).collect(Collectors.toList());
            List<ItemStack> clientCraftables = PipeNetwork.get(this.world).getAllCraftables(pipe.getPos()).stream().map(Pair::getRight).collect(Collectors.toList());
            List<ItemStack> currentlyCrafting = this.getCurrentlyCrafting().stream().sorted(Comparator.comparingInt(ItemStack::getCount).reversed()).collect(Collectors.toList());
            for (PlayerEntity player : playersToSync) {
                if (!(player.openContainer instanceof ItemTerminalContainer))
                    continue;
                ItemTerminalTileEntity tile = ((ItemTerminalContainer) player.openContainer).tile;
                if (tile != this)
                    continue;
                PacketHandler.sendTo(player, new PacketNetworkItems(clientItems, clientCraftables, currentlyCrafting));
            }
        }
    }

    public void requestItem(PlayerEntity player, ItemStack stack) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_item");
        this.updateItems();
        int requested = this.requestItemImpl(stack, onItemUnavailable(player));
        if (requested > 0) {
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".sending", requested, stack.getDisplayName()).setStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN)), UUID.randomUUID());
        } else {
            onItemUnavailable(player).accept(stack);
        }
        network.endProfile();
    }

    public int requestItemImpl(ItemStack stack, Consumer<ItemStack> unavailableConsumer) {
        NetworkItem item = this.networkItems.get(new EquatableItemStack(stack, ItemEquality.NBT));
        Collection<NetworkLocation> locations = item == null ? Collections.emptyList() : item.getLocations();
        Pair<List<NetworkLock>, ItemStack> ret = requestItemLater(this.world, this.getConnectedPipe().getPos(), locations, unavailableConsumer, stack, new Stack<>(), ItemEquality.NBT);
        this.existingRequests.addAll(ret.getLeft());
        return stack.getCount() - ret.getRight().getCount();
    }

    protected PlayerEntity[] getLookingPlayers() {
        return this.world.getPlayers().stream()
                .filter(p -> p.openContainer instanceof ItemTerminalContainer)
                .filter(p -> ((ItemTerminalContainer) p.openContainer).tile == this)
                .toArray(PlayerEntity[]::new);
    }

    private Map<EquatableItemStack, NetworkItem> collectItems(ItemEquality... equalityTypes) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_collect_items");
        PipeTileEntity pipe = this.getConnectedPipe();
        Map<EquatableItemStack, NetworkItem> items = new HashMap<>();
        for (NetworkLocation location : network.getOrderedNetworkItems(pipe.getPos())) {
            for (Map.Entry<Integer, ItemStack> entry : location.getItems(this.world).entrySet()) {
                // make sure we can extract from this slot to display it
                if (!location.canExtract(this.world, entry.getKey()))
                    continue;
                EquatableItemStack equatable = new EquatableItemStack(entry.getValue(), equalityTypes);
                NetworkItem item = items.computeIfAbsent(equatable, NetworkItem::new);
                item.add(location, entry.getValue());
            }
        }
        network.endProfile();
        return items;
    }

    private List<ItemStack> getCurrentlyCrafting() {
        PipeNetwork network = PipeNetwork.get(this.world);
        PipeTileEntity pipe = this.getConnectedPipe();
        if (pipe == null)
            return Collections.emptyList();
        List<Pair<BlockPos, ItemStack>> crafting = network.getCurrentlyCrafting(pipe.getPos());
        return crafting.stream().map(Pair::getRight).collect(Collectors.toList());
    }

    public void cancelCrafting() {
        PipeNetwork network = PipeNetwork.get(this.world);
        PipeTileEntity pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        for (Pair<BlockPos, ItemStack> craftable : network.getAllCraftables(pipe.getPos())) {
            PipeTileEntity otherPipe = network.getPipe(craftable.getLeft());
            if (otherPipe != null) {
                for (NetworkLock lock : otherPipe.craftIngredientRequests)
                    network.resolveNetworkLock(lock);
                otherPipe.craftIngredientRequests.clear();
                otherPipe.craftResultRequests.clear();
            }
        }
        PlayerEntity[] lookingPlayers = this.getLookingPlayers();
        if (lookingPlayers.length > 0)
            this.updateItems(lookingPlayers);
    }

    @Override
    public CompoundTag write(CompoundTag compound) {
        compound.put("items", this.items.serializeNBT());
        compound.put("requests", Utility.serializeAll(this.existingRequests));
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundTag compound) {
        this.items.deserializeNBT(compound.getCompound("items"));
        this.existingRequests.clear();
        this.existingRequests.addAll(Utility.deserializeAll(compound.getList("requests", NBT.TAG_COMPOUND), NetworkLock::new));
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

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == Registry.pipeConnectableCapability)
            return this.lazyThis.cast();
        return LazyOptional.empty();
    }

    @Override
    public ConnectionType getConnectionType(BlockPos pipePos, Direction direction) {
        return ConnectionType.CONNECTED;
    }

    @Override
    public ItemStack insertItem(BlockPos pipePos, Direction direction, ItemStack stack, boolean simulate) {
        BlockPos pos = pipePos.offset(direction);
        ItemTerminalTileEntity tile = Utility.getBlockEntity(ItemTerminalTileEntity.class, world, pos);
        if (tile != null)
            return ItemHandlerHelper.insertItemStacked(tile.items, stack, simulate);
        return stack;
    }

    @Override
    public boolean allowsModules(BlockPos pipePos, Direction direction) {
        return true;
    }

    public static Pair<List<NetworkLock>, ItemStack> requestItemLater(World world, BlockPos destPipe, Collection<NetworkLocation> locations, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain, ItemEquality... equalityTypes) {
        List<NetworkLock> requests = new ArrayList<>();
        ItemStack remain = stack.copy();
        PipeNetwork network = PipeNetwork.get(world);
        // check for existing items
        for (NetworkLocation location : locations) {
            int amount = location.getItemAmount(world, stack, equalityTypes);
            if (amount <= 0)
                continue;
            amount -= network.getLockedAmount(location.getPos(), stack, null, equalityTypes);
            if (amount > 0) {
                if (remain.getCount() < amount)
                    amount = remain.getCount();
                remain.shrink(amount);
                while (amount > 0) {
                    ItemStack copy = stack.copy();
                    copy.setCount(Math.min(stack.getMaxStackSize(), amount));
                    NetworkLock lock = new NetworkLock(location, copy);
                    network.createNetworkLock(lock);
                    requests.add(lock);
                    amount -= copy.getCount();
                }
                if (remain.isEmpty())
                    break;
            }
        }
        // check for craftable items
        if (!remain.isEmpty())
            remain = network.requestCraftedItem(destPipe, unavailableConsumer, remain, dependencyChain, equalityTypes);
        return Pair.of(requests, remain);
    }

    public static Consumer<ItemStack> onItemUnavailable(PlayerEntity player) {
        return s -> player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".not_found", s.getDisplayName()).setStyle(Style.EMPTY.setFormatting(TextFormatting.RED)), UUID.randomUUID());
    }
}
