package de.ellpeck.prettypipes.terminal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketGhostSlot;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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
        items:
        for (int i = 0; i < this.ghostItems.getSlots(); i++) {
            List<ItemStack> items = stacks.get(i);
            if (items.isEmpty()) {
                this.ghostItems.setStackInSlot(i, ItemStack.EMPTY);
                continue;
            }
            if (items.size() > 1) {
                // set the item into the ghost slot that already has a variant of itself available in the system
                for (ItemStack stack : items) {
                    EquatableItemStack equatable = new EquatableItemStack(stack);
                    NetworkItem network = this.networkItems.get(equatable);
                    if (network == null)
                        continue;
                    if (network.getLocations().stream().anyMatch(l -> l.getItemAmount(this.world, stack, ItemEqualityType.NBT) > 0)) {
                        this.ghostItems.setStackInSlot(i, stack);
                        continue items;
                    }
                }
            }
            // if the ghost slot wasn't set, then we don't have the item in the system
            // so just pick a random one to put into the slot
            this.ghostItems.setStackInSlot(i, items.get(0));
        }

        if (!this.world.isRemote) {
            ListMultimap<Integer, ItemStack> clients = ArrayListMultimap.create();
            for (int i = 0; i < this.ghostItems.getSlots(); i++)
                clients.put(i, this.ghostItems.getStackInSlot(i));
            PacketHandler.sendToAllLoaded(this.world, this.pos, new PacketGhostSlot(this.pos, clients));
        }
    }

    public void requestCraftingItems(PlayerEntity player, int maxAmount) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_crafting");
        this.updateItems();

        // the highest amount we can craft with the items we have
        int lowestAvailable = Integer.MAX_VALUE;
        // this is the amount of items required for each ingredient when crafting ONE
        Map<EquatableItemStack, MutableInt> requiredItems = new HashMap<>();
        for (int i = 0; i < this.craftItems.getSlots(); i++) {
            ItemStack requested = this.getRequestedCraftItem(i);
            if (requested.isEmpty())
                continue;
            MutableInt amount = requiredItems.computeIfAbsent(new EquatableItemStack(requested), s -> new MutableInt());
            amount.add(1);
            int fit = requested.getMaxStackSize() - (this.isGhostItem(i) ? 0 : requested.getCount());
            if (lowestAvailable > fit)
                lowestAvailable = fit;
        }
        for (Map.Entry<EquatableItemStack, MutableInt> entry : requiredItems.entrySet()) {
            EquatableItemStack stack = entry.getKey();
            NetworkItem item = this.networkItems.get(stack);
            // total amount of available items of this type
            int available = 0;
            if (item != null) {
                for (NetworkLocation location : item.getLocations()) {
                    int amount = location.getItemAmount(this.world, stack.stack, ItemEqualityType.NBT);
                    if (amount <= 0)
                        continue;
                    amount -= network.getLockedAmount(location.getPos(), stack.stack, ItemEqualityType.NBT);
                    available += amount;
                }
                // divide the total by the amount required to get the amount that
                // we have available for each crafting slot that contains this item
                available /= entry.getValue().intValue();
                if (available < lowestAvailable)
                    lowestAvailable = available;
            } else {
                lowestAvailable = 0;
            }
            if (available <= 0)
                player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".not_found", stack.stack.getDisplayName()).setStyle(Style.EMPTY.setFormatting(TextFormatting.RED)), UUID.randomUUID());
        }
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
                this.requestItemImpl(requested);
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
}
