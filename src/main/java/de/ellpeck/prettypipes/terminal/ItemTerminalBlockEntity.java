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
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemTerminalBlockEntity extends BlockEntity implements IPipeConnectable, MenuProvider {

    public final ItemStackHandler items = new ItemStackHandler(12) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    };
    protected Map<EquatableItemStack, NetworkItem> networkItems;
    private final Queue<NetworkLock> existingRequests = new LinkedList<>();
    private final LazyOptional<IPipeConnectable> lazyThis = LazyOptional.of(() -> this);

    public ItemTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemTerminalBlockEntity(BlockPos pos, BlockState state) {
        this(Registry.itemTerminalBlockEntity, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ItemTerminalBlockEntity terminal) {
        if (terminal.level.isClientSide)
            return;
        var network = PipeNetwork.get(terminal.level);
        var pipe = terminal.getConnectedPipe();
        if (pipe == null)
            return;

        var update = false;
        var interval = pipe.pressurizer != null ? 2 : 10;
        if (terminal.level.getGameTime() % interval == 0) {
            for (var i = 6; i < 12; i++) {
                var extracted = terminal.items.extractItem(i, Integer.MAX_VALUE, true);
                if (extracted.isEmpty())
                    continue;
                var remain = network.routeItem(pipe.getBlockPos(), terminal.getBlockPos(), extracted, true);
                if (remain.getCount() == extracted.getCount())
                    continue;
                terminal.items.extractItem(i, extracted.getCount() - remain.getCount(), false);
                break;
            }

            if (!terminal.existingRequests.isEmpty()) {
                var request = terminal.existingRequests.remove();
                network.resolveNetworkLock(request);
                network.requestExistingItem(request.location, pipe.getBlockPos(), terminal.getBlockPos(), request, request.stack, ItemEquality.NBT);
                update = true;
            }
        }

        if (terminal.level.getGameTime() % 100 == 0 || update) {
            var lookingPlayers = terminal.getLookingPlayers();
            if (lookingPlayers.length > 0)
                terminal.updateItems(lookingPlayers);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        var network = PipeNetwork.get(this.level);
        for (var lock : this.existingRequests)
            network.resolveNetworkLock(lock);
        this.lazyThis.invalidate();
    }

    public String getInvalidTerminalReason() {
        var network = PipeNetwork.get(this.level);
        var pipes = Arrays.stream(Direction.values())
                .map(d -> network.getPipe(this.worldPosition.relative(d)))
                .filter(Objects::nonNull).count();
        if (pipes <= 0)
            return "info." + PrettyPipes.ID + ".no_pipe_connected";
        if (pipes > 1)
            return "info." + PrettyPipes.ID + ".too_many_pipes_connected";
        return null;
    }

    public PipeBlockEntity getConnectedPipe() {
        var network = PipeNetwork.get(this.level);
        for (var dir : Direction.values()) {
            var pipe = network.getPipe(this.worldPosition.relative(dir));
            if (pipe != null)
                return pipe;
        }
        return null;
    }

    public void updateItems(Player... playersToSync) {
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        this.networkItems = this.collectItems(ItemEquality.NBT);
        if (playersToSync.length > 0) {
            var clientItems = this.networkItems.values().stream().map(NetworkItem::asStack).collect(Collectors.toList());
            var clientCraftables = PipeNetwork.get(this.level).getAllCraftables(pipe.getBlockPos()).stream().map(Pair::getRight).collect(Collectors.toList());
            var currentlyCrafting = this.getCurrentlyCrafting().stream().sorted(Comparator.comparingInt(ItemStack::getCount).reversed()).collect(Collectors.toList());
            for (var player : playersToSync) {
                if (!(player.containerMenu instanceof ItemTerminalContainer container))
                    continue;
                if (container.tile != this)
                    continue;
                PacketHandler.sendTo(player, new PacketNetworkItems(clientItems, clientCraftables, currentlyCrafting));
            }
        }
    }

    public void requestItem(Player player, ItemStack stack, int nbtHash) {
        var network = PipeNetwork.get(this.level);
        network.startProfile("terminal_request_item");
        this.updateItems();
        if (nbtHash != 0) {
            var filter = stack;
            stack = this.networkItems.values().stream()
                    .map(NetworkItem::asStack)
                    // don't compare with nbt equality here or the whole hashing thing is pointless
                    .filter(s -> ItemEquality.compareItems(s, filter) && s.hasTag() && s.getTag().hashCode() == nbtHash)
                    .findFirst().orElse(filter);
        }
        var requested = this.requestItemImpl(stack, onItemUnavailable(player, false));
        if (requested > 0) {
            player.sendMessage(new TranslatableComponent("info." + PrettyPipes.ID + ".sending", requested, stack.getHoverName()).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN)), UUID.randomUUID());
        } else {
            onItemUnavailable(player, false).accept(stack);
        }
        network.endProfile();
    }

    public int requestItemImpl(ItemStack stack, Consumer<ItemStack> unavailableConsumer) {
        var item = this.networkItems.get(new EquatableItemStack(stack, ItemEquality.NBT));
        Collection<NetworkLocation> locations = item == null ? Collections.emptyList() : item.getLocations();
        var ret = requestItemLater(this.level, this.getConnectedPipe().getBlockPos(), locations, unavailableConsumer, stack, new Stack<>(), ItemEquality.NBT);
        this.existingRequests.addAll(ret.getLeft());
        return stack.getCount() - ret.getRight().getCount();
    }

    public Player[] getLookingPlayers() {
        return this.level.players().stream().filter(p -> p.containerMenu instanceof ItemTerminalContainer container && container.tile == this).toArray(Player[]::new);
    }

    private Map<EquatableItemStack, NetworkItem> collectItems(ItemEquality... equalityTypes) {
        var network = PipeNetwork.get(this.level);
        network.startProfile("terminal_collect_items");
        var pipe = this.getConnectedPipe();
        Map<EquatableItemStack, NetworkItem> items = new HashMap<>();
        for (var location : network.getOrderedNetworkItems(pipe.getBlockPos())) {
            for (var entry : location.getItems(this.level).entrySet()) {
                // make sure we can extract from this slot to display it
                if (!location.canExtract(this.level, entry.getKey()))
                    continue;
                var equatable = new EquatableItemStack(entry.getValue(), equalityTypes);
                var item = items.computeIfAbsent(equatable, NetworkItem::new);
                item.add(location, entry.getValue());
            }
        }
        network.endProfile();
        return items;
    }

    private List<ItemStack> getCurrentlyCrafting() {
        var network = PipeNetwork.get(this.level);
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return Collections.emptyList();
        var crafting = network.getCurrentlyCrafting(pipe.getBlockPos());
        return crafting.stream().map(Pair::getRight).collect(Collectors.toList());
    }

    public void cancelCrafting() {
        var network = PipeNetwork.get(this.level);
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        for (var craftable : network.getAllCraftables(pipe.getBlockPos())) {
            var otherPipe = network.getPipe(craftable.getLeft());
            if (otherPipe != null) {
                for (var lock : otherPipe.craftIngredientRequests)
                    network.resolveNetworkLock(lock);
                otherPipe.craftIngredientRequests.clear();
                otherPipe.craftResultRequests.clear();
            }
        }
        var lookingPlayers = this.getLookingPlayers();
        if (lookingPlayers.length > 0)
            this.updateItems(lookingPlayers);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put("items", this.items.serializeNBT());
        compound.put("requests", Utility.serializeAll(this.existingRequests));
    }

    @Override
    public void load(CompoundTag compound) {
        this.items.deserializeNBT(compound.getCompound("items"));
        this.existingRequests.clear();
        this.existingRequests.addAll(Utility.deserializeAll(compound.getList("requests", Tag.TAG_COMPOUND), NetworkLock::new));
        super.load(compound);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + PrettyPipes.ID + ".item_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new ItemTerminalContainer(Registry.itemTerminalContainer, window, player, this.worldPosition);
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
        var pos = pipePos.relative(direction);
        var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, this.level, pos);
        if (tile != null)
            return ItemHandlerHelper.insertItemStacked(tile.items, stack, simulate);
        return stack;
    }

    @Override
    public boolean allowsModules(BlockPos pipePos, Direction direction) {
        return true;
    }

    public static Pair<List<NetworkLock>, ItemStack> requestItemLater(Level world, BlockPos destPipe, Collection<NetworkLocation> locations, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain, ItemEquality... equalityTypes) {
        List<NetworkLock> requests = new ArrayList<>();
        var remain = stack.copy();
        var network = PipeNetwork.get(world);
        // check for existing items
        for (var location : locations) {
            var amount = location.getItemAmount(world, stack, equalityTypes);
            if (amount <= 0)
                continue;
            amount -= network.getLockedAmount(location.getPos(), stack, null, equalityTypes);
            if (amount > 0) {
                if (remain.getCount() < amount)
                    amount = remain.getCount();
                remain.shrink(amount);
                while (amount > 0) {
                    var copy = stack.copy();
                    copy.setCount(Math.min(stack.getMaxStackSize(), amount));
                    var lock = new NetworkLock(location, copy);
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

    public static Consumer<ItemStack> onItemUnavailable(Player player, boolean ignore) {
        return s -> {
            if (ignore)
                return;
            player.sendMessage(new TranslatableComponent("info." + PrettyPipes.ID + ".not_found", s.getHoverName()).setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED)), UUID.randomUUID());
        };
    }
}
