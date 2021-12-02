package de.ellpeck.prettypipes.pipe.modules.retrieval;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

public class RetrievalModuleItem extends ModuleItem {

    private final int maxExtraction;
    private final int speed;
    private final boolean preventOversending;
    public final int filterSlots;

    public RetrievalModuleItem(String name, ModuleTier tier) {
        super(name);
        this.maxExtraction = tier.forTier(1, 8, 16);
        this.speed = tier.forTier(40, 20, 10);
        this.filterSlots = tier.forTier(3, 6, 9);
        this.preventOversending = tier.forTier(false, true, true);
    }

    @Override
    public void tick(ItemStack module, PipeBlockEntity tile) {
        if (!tile.shouldWorkNow(this.speed) || !tile.canWork())
            return;
        PipeNetwork network = PipeNetwork.get(tile.getLevel());

        ItemEquality[] equalityTypes = ItemFilter.getEqualityTypes(tile);
        // loop through filters to see which items to pull
        for (ItemFilter subFilter : tile.getFilters()) {
            for (int f = 0; f < subFilter.getSlots(); f++) {
                ItemStack filtered = subFilter.getStackInSlot(f);
                if (filtered.isEmpty())
                    continue;
                ItemStack copy = filtered.copy();
                copy.setCount(this.maxExtraction);
                Pair<BlockPos, ItemStack> dest = tile.getAvailableDestination(copy, true, this.preventOversending);
                if (dest == null)
                    continue;
                ItemStack remain = dest.getRight().copy();
                // are we already waiting for crafting results? If so, don't request those again
                remain.shrink(network.getCurrentlyCraftingAmount(tile.getBlockPos(), copy, equalityTypes));
                if (network.requestItem(tile.getBlockPos(), dest.getLeft(), remain, equalityTypes).isEmpty())
                    break;
            }
        }
    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeBlockEntity tile) {
        return false;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeBlockEntity tile, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof RetrievalModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex) {
        return new RetrievalModuleContainer(Registry.retrievalModuleContainer, windowId, player, tile.getBlockPos(), moduleIndex);
    }

    @Override
    public ItemFilter getItemFilter(ItemStack module, PipeBlockEntity tile) {
        ItemFilter filter = new ItemFilter(this.filterSlots, module, tile);
        filter.canModifyWhitelist = false;
        filter.isWhitelist = true;
        return filter;
    }
}
