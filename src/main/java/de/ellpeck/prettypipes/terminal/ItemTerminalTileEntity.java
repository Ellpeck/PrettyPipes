package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemOrder;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketNetworkItems;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
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
    private final Queue<Triple<NetworkLocation, Integer, Integer>> pendingRequests = new ArrayDeque<>();

    public ItemTerminalTileEntity() {
        super(Registry.itemTerminalTileEntity);
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
        if (this.world.getGameTime() % 10 == 0) {
            for (int i = 6; i < 12; i++) {
                ItemStack extracted = this.items.extractItem(i, Integer.MAX_VALUE, true);
                if (extracted.isEmpty())
                    continue;
                if (!network.tryInsertItem(pipe.getPos(), this.pos, extracted, true))
                    continue;
                this.items.extractItem(i, extracted.getCount(), false);
                break;
            }

            if (!this.pendingRequests.isEmpty()) {
                Triple<NetworkLocation, Integer, Integer> request = this.pendingRequests.remove();
                NetworkLocation location = request.getLeft();
                int slot = request.getMiddle();
                int amount = request.getRight();
                ItemStack extracted = location.handler.extractItem(slot, amount, true);
                if (network.routeItemToLocation(location.pipePos, location.pos, pipe.getPos(), this.pos, speed -> new PipeItem(extracted, speed))) {
                    location.handler.extractItem(slot, extracted.getCount(), false);
                    update = true;
                }
            }
        }

        if (this.world.getGameTime() % 100 == 0 || update) {
            PlayerEntity[] lookingPlayers = this.getLookingPlayers();
            if (lookingPlayers.length > 0)
                this.updateItems(lookingPlayers);
        }
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
        if (this.getConnectedPipe() == null)
            return;
        this.networkItems = this.collectItems();
        if (playersToSync.length > 0) {
            List<ItemStack> clientItems = this.networkItems.values().stream().map(NetworkItem::asStack).collect(Collectors.toList());
            for (PlayerEntity player : playersToSync) {
                if (!(player.openContainer instanceof ItemTerminalContainer))
                    continue;
                ItemTerminalTileEntity tile = ((ItemTerminalContainer) player.openContainer).tile;
                if (tile != this)
                    continue;

                CompoundNBT nbt = player.getPersistentData();
                ItemOrder order = ItemOrder.values()[nbt.getInt(PrettyPipes.ID + ":item_order")];
                boolean ascending = nbt.getBoolean(PrettyPipes.ID + ":ascending");
                PacketHandler.sendTo(player, new PacketNetworkItems(clientItems, order, ascending));
            }
        }
    }

    public void requestItem(PlayerEntity player, ItemStack stack) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_item");
        EquatableItemStack equatable = new EquatableItemStack(stack);
        NetworkItem item = this.networkItems.get(equatable);
        if (item != null) {
            int remain = stack.getCount();
            locations:
            for (NetworkLocation location : item.getLocations()) {
                for (int slot : location.getStackSlots(stack)) {
                    ItemStack extracted = location.handler.extractItem(slot, remain, true);
                    if (!extracted.isEmpty()) {
                        this.pendingRequests.add(Triple.of(location, slot, extracted.getCount()));
                        remain -= extracted.getCount();
                        if (remain <= 0)
                            break locations;
                    }
                }
            }
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".sending", stack.getCount() - remain, stack.getDisplayName()).setStyle(new Style().setColor(TextFormatting.GREEN)));
        } else {
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".not_found", stack.getDisplayName()).setStyle(new Style().setColor(TextFormatting.RED)));
        }
        network.endProfile();
    }

    private PlayerEntity[] getLookingPlayers() {
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
            for (ItemStack stack : location.getItems()) {
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
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.items.deserializeNBT(compound.getCompound("items"));
        super.read(compound);
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
