package de.ellpeck.prettypipes.terminal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CraftingTerminalBlockEntity extends ItemTerminalBlockEntity {

    public final ItemStackHandler craftItems = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            for (var playerEntity : CraftingTerminalBlockEntity.this.getLookingPlayers())
                playerEntity.containerMenu.slotsChanged(null);
        }
    };
    public final ItemStackHandler ghostItems = new ItemStackHandler(9);

    public CraftingTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.craftingTerminalBlockEntity, pos, state);
    }

    public ItemStack getRequestedCraftItem(int slot) {
        var stack = this.craftItems.getStackInSlot(slot);
        if (!stack.isEmpty())
            return stack;
        return this.ghostItems.getStackInSlot(slot);
    }

    public boolean isGhostItem(int slot) {
        return this.craftItems.getStackInSlot(slot).isEmpty() && !this.ghostItems.getStackInSlot(slot).isEmpty();
    }

    public void setGhostItems(ListMultimap<Integer, ItemStack> stacks) {
        this.updateItems();
        for (var i = 0; i < this.ghostItems.getSlots(); i++) {
            var items = stacks.get(i);
            if (items.isEmpty()) {
                this.ghostItems.setStackInSlot(i, ItemStack.EMPTY);
                continue;
            }
            var toSet = items.get(0);
            // if we have more than one item to choose from, we want to pick the one that we have most of in the system
            if (items.size() > 1) {
                var highestAmount = 0;
                for (var stack : items) {
                    var amount = 0;
                    // check existing items
                    var network = this.networkItems.get(new EquatableItemStack(stack, ItemEquality.NBT));
                    if (network != null) {
                        amount = network.getLocations().stream()
                                .mapToInt(l -> l.getItemAmount(this.level, stack, ItemEquality.NBT))
                                .sum();
                    }
                    // check craftables
                    if (amount <= 0 && highestAmount <= 0) {
                        var pipe = this.getConnectedPipe();
                        if (pipe != null)
                            amount = PipeNetwork.get(this.level).getCraftableAmount(pipe.getBlockPos(), null, stack, new Stack<>(), ItemEquality.NBT);
                    }
                    if (amount > highestAmount) {
                        highestAmount = amount;
                        toSet = stack;
                    }
                }
            }
            this.ghostItems.setStackInSlot(i, toSet.copy());
        }

        if (!this.level.isClientSide) {
            ListMultimap<Integer, ItemStack> clients = ArrayListMultimap.create();
            for (var i = 0; i < this.ghostItems.getSlots(); i++)
                clients.put(i, this.ghostItems.getStackInSlot(i));
            PacketHandler.sendToAllLoaded(this.level, this.getBlockPos(), new PacketGhostSlot(this.getBlockPos(), clients));
        }
    }

    public void requestCraftingItems(Player player, int maxAmount, boolean force) {
        var pipe = this.getConnectedPipe();
        if (pipe == null)
            return;
        var network = PipeNetwork.get(this.level);
        network.startProfile("terminal_request_crafting");
        this.updateItems();
        // get the amount of crafts that we can do
        var lowestAvailable = getAvailableCrafts(pipe, this.craftItems.getSlots(), i -> ItemHandlerHelper.copyStackWithSize(this.getRequestedCraftItem(i), 1), this::isGhostItem, s -> {
            var item = this.networkItems.get(s);
            return item != null ? item.getLocations() : Collections.emptyList();
        }, onItemUnavailable(player, force), new Stack<>(), ItemEquality.NBT);
        // if we're forcing, just pretend we have one available
        if (lowestAvailable <= 0 && force)
            lowestAvailable = maxAmount;
        if (lowestAvailable > 0) {
            // if we're limiting the amount, pretend we only have that amount available
            if (maxAmount < lowestAvailable)
                lowestAvailable = maxAmount;
            for (var i = 0; i < this.craftItems.getSlots(); i++) {
                var requested = this.getRequestedCraftItem(i);
                if (requested.isEmpty())
                    continue;
                requested = requested.copy();
                requested.setCount(lowestAvailable);
                this.requestItemImpl(requested, onItemUnavailable(player, force));
            }
            player.sendMessage(new TranslatableComponent("info." + PrettyPipes.ID + ".sending_ingredients", lowestAvailable).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN)), UUID.randomUUID());
        }
        else{
            player.sendMessage(new TranslatableComponent("info." + PrettyPipes.ID + ".hold_alt"), UUID.randomUUID());
        }
        network.endProfile();
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put("craft_items", this.craftItems.serializeNBT());
    }

    @Override
    public void load(CompoundTag compound) {
        this.craftItems.deserializeNBT(compound.getCompound("craft_items"));
        super.load(compound);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + PrettyPipes.ID + ".crafting_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new CraftingTerminalContainer(Registry.craftingTerminalContainer, window, player, this.worldPosition);
    }

    @Override
    public ItemStack insertItem(BlockPos pipePos, Direction direction, ItemStack remain, boolean simulate) {
        var pos = pipePos.relative(direction);
        var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, this.level, pos);
        if (tile != null) {
            remain = remain.copy();
            var lowestSlot = -1;
            do {
                for (var i = 0; i < tile.craftItems.getSlots(); i++) {
                    var stack = tile.getRequestedCraftItem(i);
                    var count = tile.isGhostItem(i) ? 0 : stack.getCount();
                    if (!ItemHandlerHelper.canItemStacksStack(stack, remain))
                        continue;
                    // ensure that a single non-stackable item can still enter a ghost slot
                    if (!stack.isStackable() && count >= 1)
                        continue;
                    if (lowestSlot < 0 || !tile.isGhostItem(lowestSlot) && count < tile.getRequestedCraftItem(lowestSlot).getCount())
                        lowestSlot = i;
                }
                if (lowestSlot >= 0) {
                    var copy = remain.copy();
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

    public static int getAvailableCrafts(PipeBlockEntity tile, int slots, Function<Integer, ItemStack> inputFunction, Predicate<Integer> isGhost, Function<EquatableItemStack, Collection<NetworkLocation>> locationsFunction, Consumer<ItemStack> unavailableConsumer, Stack<ItemStack> dependencyChain, ItemEquality... equalityTypes) {
        var network = PipeNetwork.get(tile.getLevel());
        // the highest amount we can craft with the items we have
        var lowestAvailable = Integer.MAX_VALUE;
        // this is the amount of items required for each ingredient when crafting ONE
        Map<EquatableItemStack, MutableInt> requiredItems = new HashMap<>();
        for (var i = 0; i < slots; i++) {
            var requested = inputFunction.apply(i);
            if (requested.isEmpty())
                continue;
            var amount = requiredItems.computeIfAbsent(new EquatableItemStack(requested, equalityTypes), s -> new MutableInt());
            amount.add(requested.getCount());
            // if no items fit into the crafting input, we still want to pretend they do for requesting
            var fit = Math.max(requested.getMaxStackSize() - (isGhost.test(i) ? 0 : requested.getCount()), 1);
            if (lowestAvailable > fit)
                lowestAvailable = fit;
        }
        for (var entry : requiredItems.entrySet()) {
            var stack = entry.getKey();

            // total amount of available items of this type
            var available = 0;
            for (var location : locationsFunction.apply(stack)) {
                var amount = location.getItemAmount(tile.getLevel(), stack.stack(), equalityTypes);
                if (amount <= 0)
                    continue;
                amount -= network.getLockedAmount(location.getPos(), stack.stack(), null, equalityTypes);
                available += amount;
            }
            // divide the total by the amount required to get the amount that
            // we have available for each crafting slot that contains this item
            available /= entry.getValue().intValue();

            // check how many craftable items we have and add those on if we need to
            if (available < lowestAvailable) {
                var craftable = network.getCraftableAmount(tile.getBlockPos(), unavailableConsumer, stack.stack(), dependencyChain, equalityTypes);
                if (craftable > 0)
                    available += craftable / entry.getValue().intValue();
            }

            // clamp to the lowest available
            if (available < lowestAvailable)
                lowestAvailable = available;

            if (available <= 0 && unavailableConsumer != null)
                unavailableConsumer.accept(stack.stack());
        }
        return lowestAvailable;
    }
}
