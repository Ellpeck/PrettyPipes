package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.network.*;
import de.ellpeck.prettypipes.packets.PacketNetworkItems;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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
            var currentlyCrafting = this.getCurrentlyCrafting(false).stream().sorted(Comparator.comparingInt(ItemStack::getCount).reversed()).collect(Collectors.toList());
            for (var player : playersToSync) {
                if (!(player.containerMenu instanceof ItemTerminalContainer container))
                    continue;
                if (container.tile != this)
                    continue;
                ((ServerPlayer) player).connection.send(new PacketNetworkItems(clientItems, clientCraftables, currentlyCrafting));
            }
        }
    }

    public void requestItem(Player player, ItemStack stack, int componentsHash) {
        var network = PipeNetwork.get(this.level);
        network.startProfile("terminal_request_item");
        this.updateItems();
        if (componentsHash != 0) {
            var filter = stack;
            stack = this.networkItems.values().stream()
                .map(NetworkItem::asStack)
                // don't compare with nbt equality here or the data hashing thing is pointless
                .filter(s -> ItemEquality.compareItems(s, filter) && !s.isComponentsPatchEmpty() && s.getComponents().hashCode() == componentsHash)
                .findFirst().orElse(filter);
            stack.setCount(filter.getCount());
        }
        var requested = this.requestItemImpl(stack, ItemTerminalBlockEntity.onItemUnavailable(player, false));
        if (requested > 0) {
            player.sendSystemMessage(Component.translatable("info." + PrettyPipes.ID + ".sending", requested, stack.getHoverName()).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN)));
        } else {
            ItemTerminalBlockEntity.onItemUnavailable(player, false).accept(stack);
        }
        network.endProfile();
    }

    public int requestItemImpl(ItemStack stack, Consumer<ItemStack> unavailableConsumer) {
        var item = this.networkItems.get(new EquatableItemStack(stack, ItemEquality.NBT));
        Collection<NetworkLocation> locations = item == null ? Collections.emptyList() : item.getLocations();
        var network = PipeNetwork.get(this.level);
        var ret = network.requestLocksAndCrafts(this.getConnectedPipe().getBlockPos(), locations, unavailableConsumer, stack, new Stack<>(), ItemEquality.NBT);
        this.existingRequests.addAll(ret.getLeft());
        return stack.getCount() - ret.getMiddle().getCount();
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

    private List<ItemStack> getCurrentlyCrafting(boolean includeCanceled) {
        var network = PipeNetwork.get(this.level);
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return Collections.emptyList();
        var crafting = network.getCurrentlyCrafting(pipe.getBlockPos(), includeCanceled);
        return crafting.stream().map(Pair::getRight).collect(Collectors.toList());
    }

    public void cancelCrafting() {
        var network = PipeNetwork.get(this.level);
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        for (var craftable : network.getAllCraftables(pipe.getBlockPos())) {
            var otherPipe = network.getPipe(craftable.getLeft());
            if (otherPipe != null)
                otherPipe.activeCrafts.removeIf(c -> c.getRight().markCanceledOrResolve(network));
        }
        var lookingPlayers = this.getLookingPlayers();
        if (lookingPlayers.length > 0)
            this.updateItems(lookingPlayers);
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider pRegistries) {
        super.saveAdditional(compound, pRegistries);
        compound.put("items", this.items.serializeNBT(pRegistries));
        compound.put("requests", Utility.serializeAll(this.existingRequests, i -> i.serializeNBT(pRegistries)));
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider pRegistries) {
        this.items.deserializeNBT(pRegistries, compound.getCompound("items"));
        this.existingRequests.clear();
        this.existingRequests.addAll(Utility.deserializeAll(compound.getList("requests", Tag.TAG_COMPOUND), l -> new NetworkLock(pRegistries, l)));
        super.loadAdditional(compound, pRegistries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + PrettyPipes.ID + ".item_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new ItemTerminalContainer(Registry.itemTerminalContainer, window, player, this.worldPosition);
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

    public static Consumer<ItemStack> onItemUnavailable(Player player, boolean ignore) {
        return s -> {
            if (ignore)
                return;
            player.sendSystemMessage(Component.translatable("info." + PrettyPipes.ID + ".not_found", s.getHoverName()).setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED)));
        };
    }

}
