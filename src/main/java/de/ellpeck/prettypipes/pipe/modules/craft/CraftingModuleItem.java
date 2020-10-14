package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.NetworkLock;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class CraftingModuleItem extends ModuleItem {

    public final int inputSlots;
    public final int outputSlots;

    public CraftingModuleItem(String name, ModuleTier tier) {
        super(name);
        this.inputSlots = tier.forTier(1, 4, 9);
        this.outputSlots = tier.forTier(1, 2, 4);
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
    public List<ItemStack> getCraftables(ItemStack module, PipeTileEntity tile) {
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
        List<Pair<BlockPos, ItemStack>> craftables = network.getOrderedCraftables(tile.getPos());
        ItemEqualityType[] equalityTypes = ItemFilter.getEqualityTypes(tile);

        ItemStackHandler output = this.getOutput(module);
        ItemStackHandler input = this.getInput(module);

        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (!stack.isEmpty()) {
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
        List<ItemStack> craftables = this.getCraftables(module, tile);
        int craftableAmount = craftables.stream()
                .filter(c -> ItemEqualityType.compareItems(c, stack, equalityTypes))
                .mapToInt(ItemStack::getCount).sum();
        if (craftableAmount > 0) {
            PipeNetwork network = PipeNetwork.get(tile.getWorld());
            List<NetworkLocation> items = network.getOrderedNetworkItems(tile.getPos());
            List<Pair<BlockPos, ItemStack>> allCraftables = network.getOrderedCraftables(tile.getPos());

            int resultAmount = this.getResultAmountPerCraft(module, stack, equalityTypes);
            int requiredCrafts = MathHelper.ceil(stack.getCount() / (float) resultAmount);

            ItemStackHandler input = this.getInput(module);
            for (int i = 0; i < input.getSlots(); i++) {
                ItemStack in = input.getStackInSlot(i).copy();
                if (in.isEmpty())
                    continue;
                in.setCount(in.getCount() * requiredCrafts);
                List<NetworkLock> requests = ItemTerminalTileEntity.requestItemLater(tile.getWorld(), destPipe, destInventory, in, items, allCraftables, equalityTypes);
                tile.craftIngredientRequests.addAll(requests);
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
