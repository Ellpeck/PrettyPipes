package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ItemTerminalTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity {

    public final ItemStackHandler items = new ItemStackHandler(12) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot >= 6;
        }
    };
    public Collection<NetworkItem> networkItems;

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
        }

        if (this.world.getGameTime() % 100 == 0) {
            PlayerEntity[] lookingPlayers = this.world.getPlayers().stream()
                    .filter(p -> p.openContainer instanceof ItemTerminalContainer)
                    .filter(p -> ((ItemTerminalContainer) p.openContainer).tile == this)
                    .toArray(PlayerEntity[]::new);
            if (lookingPlayers.length > 0)
                this.updateItems(lookingPlayers);
        }
    }

    private PipeTileEntity getConnectedPipe() {
        PipeNetwork network = PipeNetwork.get(this.world);
        for (Direction dir : Direction.values()) {
            PipeTileEntity pipe = network.getPipe(this.pos.offset(dir));
            if (pipe != null)
                return pipe;
        }
        return null;
    }

    public void updateItems(PlayerEntity... playersToSync) {
        this.networkItems = this.collectItems();
        List<ItemStack> clientItems = this.networkItems.stream().map(NetworkItem::asStack).collect(Collectors.toList());
        for (PlayerEntity player : playersToSync) {
            if (!(player.openContainer instanceof ItemTerminalContainer))
                continue;
            ItemTerminalTileEntity tile = ((ItemTerminalContainer) player.openContainer).tile;
            if (tile != this)
                continue;
            PacketHandler.sendTo(player, new PacketNetworkItems(clientItems));
        }
    }

    private Collection<NetworkItem> collectItems() {
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
        return items.values();
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
