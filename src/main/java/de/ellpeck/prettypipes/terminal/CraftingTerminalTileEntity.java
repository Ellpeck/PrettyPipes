package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.misc.EquatableItemStack;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.network.NetworkItem;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftingTerminalTileEntity extends ItemTerminalTileEntity {

    public final ItemStackHandler craftItems = new ItemStackHandler(9);

    public CraftingTerminalTileEntity() {
        super(Registry.craftingTerminalTileEntity);
    }

    public ItemStack getRequestedCraftItem(int slot) {
        // TODO put ghost slot contents here
        return this.craftItems.getStackInSlot(slot);
    }

    public void requestCraftingItems(PlayerEntity player, boolean all) {
        PipeNetwork network = PipeNetwork.get(this.world);
        network.startProfile("terminal_request_crafting");
        this.updateItems();

        // this is the amount of items required for each ingredient when crafting ONE
        Map<EquatableItemStack, MutableInt> requiredItems = new HashMap<>();
        for (int i = 0; i < this.craftItems.getSlots(); i++) {
            ItemStack requested = this.getRequestedCraftItem(i);
            if (requested.isEmpty())
                continue;
            MutableInt amount = requiredItems.computeIfAbsent(new EquatableItemStack(requested), s -> new MutableInt());
            amount.add(1);
        }
        // the highest amount we can craft with the items we have
        int lowestAvailable = Integer.MAX_VALUE;
        for (Map.Entry<EquatableItemStack, MutableInt> entry : requiredItems.entrySet()) {
            EquatableItemStack stack = entry.getKey();
            NetworkItem item = this.networkItems.get(stack);
            if (item != null) {
                // total amount of available items of this type
                int available = 0;
                for (NetworkLocation location : item.getLocations()) {
                    for (int slot : location.getStackSlots(this.world, stack.stack, ItemEqualityType.NBT)) {
                        ItemStack inSlot = location.getItemHandler(this.world).extractItem(slot, Integer.MAX_VALUE, true);
                        if (inSlot.isEmpty())
                            continue;
                        inSlot.shrink(network.getLockedAmount(location.getPos(), slot));
                        available += inSlot.getCount();
                    }
                }
                // divide the total by the amount required to get the amount that
                // we have available for each crafting slot that contains this item
                available /= entry.getValue().intValue();
                int fit = stack.stack.getMaxStackSize() - stack.stack.getCount();
                if (available > fit)
                    available = fit;
                if (available < lowestAvailable)
                    lowestAvailable = available;
            } else {
                lowestAvailable = 0;
            }
            if (lowestAvailable <= 0)
                player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".not_found", stack.stack.getDisplayName()).setStyle(new Style().setColor(TextFormatting.RED)));
        }
        if (lowestAvailable > 0) {
            // if we're only crafting one item, pretend we only have enough for one
            if (!all)
                lowestAvailable = 1;
            for (int i = 0; i < this.craftItems.getSlots(); i++) {
                ItemStack requested = this.getRequestedCraftItem(i);
                if (requested.isEmpty())
                    continue;
                requested = requested.copy();
                requested.setCount(lowestAvailable);
                this.requestItemImpl(requested);
            }
            player.sendMessage(new TranslationTextComponent("info." + PrettyPipes.ID + ".sending_ingredients", lowestAvailable).setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
        network.endProfile();
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("craft_items", this.craftItems.serializeNBT());
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.craftItems.deserializeNBT(compound.getCompound("craft_items"));
        super.read(compound);
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
