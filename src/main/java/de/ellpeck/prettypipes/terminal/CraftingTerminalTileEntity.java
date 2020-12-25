package de.ellpeck.prettypipes.terminal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CraftingTerminalTileEntity extends ItemTerminalTileEntity {

    public final ItemStackHandler craftItems = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            for (PlayerEntity playerEntity : CraftingTerminalTileEntity.this.getLookingPlayers())
                playerEntity.openContainer.onCraftMatrixChanged(null);
        }
    };
    public final ItemStackHandler ghostItems = new ItemStackHandler(9);

    public CraftingTerminalTileEntity() {
        super(Registry.craftingTerminalTileEntity);
    }

    public ItemStack getRequestedCraftItem(int slot) {
        ItemStack stack = this.craftItems.getStackInSlot(slot);
        if (!stack.isEmpty())
            return stack;
        return this.ghostItems.getStackInSlot(slot);
    }

    public boolean isGhostItem(int slot) {
        return this.craftItems.getStackInSlot(slot).isEmpty() && !this.ghostItems.getStackInSlot(slot).isEmpty();
    }

    public void setGhostItems(ListMultimap<Integer, ItemStack> stacks) {
        this.updateItems();
        for (int i = 0; i < this.ghostItems.getSlots(); i++) {
            List<ItemStack> items = stacks.get(i);
            if (items.isEmpty()) {
                this.ghostItems.setStackInSlot(i, ItemStack.EMPTY);
                continue;
            }
            ItemStack toSet = items.get(0);
            // if we have more than one item to choose from, we want to pick the one that we have most of in the system
            if (items.size() > 1) {
                int highestAmount = 0;
                for (ItemStack stack : items) {
                    int amount = 0;
                    // check existing items
                    NetworkItem network = this.networkItems.get(new EquatableItemStack(stack, ItemEqualityType.NBT));
                    if (network != null) {
                        amount = network.getLocations().stream()
                                .mapToInt(l -> l.getItemAmount(this.world, stack, ItemEqualityType.NBT))
                                .sum();
                    }
                    // check craftables
                    if (amount <= 0 && highestAmount <= 0) {
                        PipeTileEntity pipe = this.getConnectedPipe();
                        if (pipe != null)
                            amount = PipeNetwork.get(this.world).getCraftableAmount(pipe.getPos(), null, stack, new Stack<>(), ItemEqualityType.NBT);
                    }
                    if (amount > highestAmount) {
                        highestAmount = amount;
                        toSet = stack;
                    }
                }
            }
            this.ghostItems.setStackInSlot(i, toSet.copy());
        }

        if (!this.world.isRemote) {
            ListMultimap<Integer, ItemStack> clients = ArrayListMultimap.create();
            for (int i = 0; i < this.ghostItems.getSlots(); i++)
                clients.put(i, this.ghostItems.getStackInSlot(i));
            PacketHandler.sendToAllLoaded(this.world, this.pos, new PacketGhostSlot(this.pos, clients));
        }
    }

    public void requestCraftingItems(PlayerEntity player, int maxAmount) {
        PipeTileEntity pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_crafting");
        this.updateItems();
        // get the amount of crafts that we can do
        int lowestAvailable = getAvailableCrafts(pipe, this.craftItems.getSlots(), i -> ItemHandlerHelper.copyStackWithSize(this.getRequestedCraftItem(i), 1), this::isGhostItem, s -> {
            NetworkItem item = this.networkItems.get(s);
            return item != null ? item.getLocations() : Collections.emptyList();
        }, onItemUnavailable(player), new Stack<>(), ItemEqualityType.NBT);
        if (lowestAvailable > 0) {
            // if we're limiting the amount, pretend we only have that amount available
            if (maxAmount < lowestAvailable)
                lowestAvailable = maxAmount;
            for (int i = 0; i < this.craftItems.getSlots(); i++) {
                ItemStack requested = this.getRequestedCraftItem(i);
                if (requested.isEmpty())
                    continue;
                requested = requested.copy();
                requested.setCount(lowestAvailable);
                this.requestItemImpl(requested, onItemUnavailable(player));
            }
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".sending_ingredients", lowestAvailable).setStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN)), UUID.randomUUID());
        }
        network.endProfile();
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("craft_items", this.craftItems.serializeNBT());
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        this.craftItems.deserializeNBT(compound.getCompound("craft_items"));
        super.read(state, compound);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".crafting_terminal");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new CraftingTerminalContainer(Registry.craftingTerminalContainer, window, player, this.pos);
    }

    @Override
    public ItemStack insertItem(BlockPos pipePos, Direction direction, ItemStack remain, boolean simulate) {
        BlockPos pos = pipePos.offset(direction);
        CraftingTerminalTileEntity tile = Utility.getTileEntity(CraftingTerminalTileEntity.class, this.world, pos);
        if (tile != null) {
            remain = remain.copy();
            int lowestSlot = -1;
            do {
                for (int i = 0; i < tile.craftItems.getSlots(); i++) {
                    ItemStack stack = tile.getRequestedCraftItem(i);
                    int count = tile.isGhostItem(i) ? 0 : stack.getCount();
                    if (!ItemHandlerHelper.canItemStacksStack(stack, remain))
                        continue;
                    // ensure that a single non-stackable item can still enter a ghost slot
                    if (!stack.isStackable() && count >= 1)
                        continue;
                    if (lowestSlot < 0 || !tile.isGhostItem(lowestSlot) && count < tile.getRequestedCraftItem(lowestSlot).getCount())
                        lowestSlot = i;
                }
                if (lowestSlot >= 0) {
                    ItemStack copy = remain.copy();
                    copy.setCount(1);
                    // if there were remaining items inserting into the slot with lowest contents, we're overflowing
                    if (tile.craftItems.insertItem(lowestSlot, copy, simulate).getCount() > 0)
                        break;
                    remain.shrink(1);
                    if (remain.isEmpty())
                        return ItemStack.EMPTY;
                }
            }
            while (lowestSlot >= 0);
            return ItemHandlerHelper.insertItemStacked(tile.items, remain, simulate);
        }
        return remain;
    }

    public static int getAvailableCrafts(PipeTileEntity tile, int slots, Function<Integer, ItemStack> inputFunction, Predicate<Integer> isGhost, Function<EquatableItemStack, Collection<NetworkLocation>> locationsFunction, Consumer<ItemStack> unavailableConsumer, Stack<IModule> dependencyChain, ItemEqualityType... equalityTypes) {
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        // the highest amount we can craft with the items we have
        int lowestAvailable = Integer.MAX_VALUE;
        // this is the amount of items required for each ingredient when crafting ONE
        Map<EquatableItemStack, MutableInt> requiredItems = new HashMap<>();
        for (int i = 0; i < slots; i++) {
            ItemStack requested = inputFunction.apply(i);
            if (requested.isEmpty())
                continue;
            MutableInt amount = requiredItems.computeIfAbsent(new EquatableItemStack(requested, equalityTypes), s -> new MutableInt());
            amount.add(requested.getCount());
            // if no items fit into the crafting input, we still want to pretend they do for requesting
            int fit = Math.max(requested.getMaxStackSize() - (isGhost.test(i) ? 0 : requested.getCount()), 1);
            if (lowestAvailable > fit)
                lowestAvailable = fit;
        }
        for (Map.Entry<EquatableItemStack, MutableInt> entry : requiredItems.entrySet()) {
            EquatableItemStack stack = entry.getKey();

            // total amount of available items of this type
            int available = 0;
            for (NetworkLocation location : locationsFunction.apply(stack)) {
                int amount = location.getItemAmount(tile.getWorld(), stack.stack, equalityTypes);
                if (amount <= 0)
                    continue;
                amount -= network.getLockedAmount(location.getPos(), stack.stack, null, equalityTypes);
                available += amount;
            }
            // divide the total by the amount required to get the amount that
            // we have available for each crafting slot that contains this item
            available /= entry.getValue().intValue();

            // check how many craftable items we have and add those on if we need to
            if (available < lowestAvailable) {
                int craftable = network.getCraftableAmount(tile.getPos(), unavailableConsumer, stack.stack, dependencyChain, equalityTypes);
                if (craftable > 0)
                    available += craftable / entry.getValue().intValue();
            }

            // clamp to lowest available
            if (available < lowestAvailable)
                lowestAvailable = available;

            if (available <= 0 && unavailableConsumer != null)
                unavailableConsumer.accept(stack.stack);
        }
        return lowestAvailable;
    }
}
