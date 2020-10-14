package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

public class CraftingModuleItem extends ModuleItem {

    public final int inputSlots;
    public final int outputSlots;
    private final int speed;
    private final int maxExtraction;

    public CraftingModuleItem(String name, ModuleTier tier) {
        super(name);
        this.inputSlots = tier.forTier(1, 4, 9);
        this.outputSlots = tier.forTier(1, 2, 4);
        this.speed = tier.forTier(20, 10, 5);
        this.maxExtraction = tier.forTier(1, 16, 32);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return true;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new CraftingModuleContainer(Registry.craftingModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeTileEntity tile) {
        return false;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return false;
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {
        if (tile.getWorld().getGameTime() % this.speed != 0 || !tile.canWork())
            return;
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        // process crafting ingredient requests
        if (!tile.craftIngredientRequests.isEmpty()) {
            NetworkLock request = tile.craftIngredientRequests.remove();
            Pair<BlockPos, ItemStack> dest = tile.getAvailableDestination(request.stack, true, true);
            if (dest != null) {
                network.requestExistingItem(request.location, tile.getPos(), dest.getLeft(), dest.getRight(), ItemEqualityType.NBT);
                network.resolveNetworkLock(request);

                // if we couldn't fit all items into the destination, create another request for the rest
                ItemStack remain = request.stack.copy();
                remain.shrink(dest.getRight().getCount());
                if (!remain.isEmpty()) {
                    NetworkLock remainRequest = new NetworkLock(request.location, remain);
                    tile.craftIngredientRequests.add(remainRequest);
                    network.createNetworkLock(remainRequest);
                }
            }
        }
        // pull requested crafting results from the network once they are stored
        if (!tile.craftResultRequests.isEmpty()) {
            List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
            ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
            for (Triple<BlockPos, BlockPos, ItemStack> request : tile.craftResultRequests) {
                ItemStack reqItem = request.getRight();
                for (NetworkLocation item : items) {
                    int amount = item.getItemAmount(tile.getWorld(), reqItem, equalityTypes);
                    amount -= network.getLockedAmount(item.getPos(), reqItem, equalityTypes);
                    if (amount <= 0)
                        continue;
                    ItemStack remain = reqItem.copy();
                    if (remain.getCount() < amount)
                        amount = remain.getCount();
                    remain.shrink(amount);
                    while (amount > 0) {
                        ItemStack copy = reqItem.copy();
                        copy.setCount(Math.min(reqItem.getMaxStackSize(), amount));
                        // we don't need to do any checks here because we just calculated the max amount we can definitely extract
                        network.requestExistingItem(item, request.getLeft(), request.getMiddle(), copy, equalityTypes);
                        amount -= copy.getCount();
                    }
                    tile.craftResultRequests.remove(request);
                    // if we couldn't pull everything, log a new request
                    if (!remain.isEmpty())
                        tile.craftResultRequests.add(Triple.of(request.getLeft(), request.getMiddle(), remain));
                    return;
                }
            }
        }
    }

    @Override
    public List<ItemStack> getCraftables(ItemStack module, PipeTileEntity tile, boolean onlyReturnPossible) {
        ItemStackHandler output = this.getOutput(module);
        if (!onlyReturnPossible) {
            // if we only need to return the ones we *could* craft, it's easy
            List<ItemStack> ret = new ArrayList<>();
            for (int i = 0; i < output.getSlots(); i++) {
                ItemStack stack = output.getStackInSlot(i);
                if (!stack.isEmpty())
                    ret.add(stack);
            }
            return ret;
        }

        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
        List<Pair<BlockPos, ItemStack>> craftables = network.getOrderedCraftables(tile.getPos(), true);
        ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);
        ItemStackHandler input = this.getInput(module);

        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // figure out how many crafting operations we can actually do with the input items we have in the network
                int availableCrafts = CraftingTerminalTileEntity.getAvailableCrafts(tile.getWorld(), input.getSlots(), input::getStackInSlot, k -> false, s -> items, craftables, null, equalityTypes);
                if (availableCrafts > 0) {
                    ItemStack copy = stack.copy();
                    copy.setCount(stack.getCount() * availableCrafts);
                    ret.add(copy);
                }
            }
        }
        return ret;
    }

    @Override
    public ItemStack craft(ItemStack module, PipeTileEntity tile, BlockPos destPipe, BlockPos destInventory, ItemStack stack, ItemEqualityType... equalityTypes) {
        // check if we can craft the required amount of items
        List<ItemStack> craftables = this.getCraftables(module, tile, true);
        int craftableAmount = craftables.stream()
                .filter(c -> ItemEqualityType.compareItems(c, stack, equalityTypes))
                .mapToInt(ItemStack::getCount).sum();
        if (craftableAmount > 0) {
            PipeNetwork network = PipeNetwork.get(tile.getWorld());
            List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
            List<Pair<BlockPos, ItemStack>> allCraftables = network.getOrderedCraftables(tile.getPos(), true);

            int resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
            int requiredCrafts = MathHelper.ceil(stack.getCount() / (float) resultAmount);

            ItemStackHandler input = this.getInput(module);
            for (int i = 0; i < input.getSlots(); i++) {
                ItemStack in = input.getStackInSlot(i);
                if (in.isEmpty())
                    continue;
                ItemStack copy = in.copy();
                copy.setCount(in.getCount() * requiredCrafts);
                Pair<List<NetworkLock>, ItemStack> ret = ItemTerminalTileEntity.requestItemLater(tile.getWorld(), destPipe, destInventory, copy, items, allCraftables, equalityTypes);
                tile.craftIngredientRequests.addAll(ret.getLeft());
                tile.craftResultRequests.add(Triple.of(destPipe, destInventory, stack));
            }

            ItemStack remain = stack.copy();
            remain.shrink(craftableAmount);
            return remain;
        } else {
            return stack;
        }
    }

    public ItemStackHandler getInput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.inputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("input"));
        return handler;
    }

    public ItemStackHandler getOutput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.outputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("output"));
        return handler;
    }

    public void save(ItemStackHandler input, ItemStackHandler output, ItemStack module) {
        CompoundNBT tag = module.getOrCreateTag();
        if (input != null)
            tag.put("input", input.serializeNBT());
        if (output != null)
            tag.put("output", output.serializeNBT());
    }

    private int getResultAmountPerCraft(ItemStack module, ItemStack stack, ItemEqualityType... equalityTypes) {
        ItemStackHandler output = this.getOutput(module);
        int resultAmount = 0;
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack out = output.getStackInSlot(i);
            if (ItemEqualityType.compareItems(stack, out, equalityTypes))
                resultAmount += out.getCount();
        }
        return resultAmount;
    }
}
