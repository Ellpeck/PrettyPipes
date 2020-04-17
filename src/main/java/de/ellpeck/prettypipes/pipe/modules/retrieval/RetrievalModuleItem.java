package de.ellpeck.prettypipes.pipe.modules.retrieval;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.modules.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class RetrievalModuleItem extends ModuleItem {
    private final int maxExtraction;
    private final int speed;
    public final int filterSlots;

    public RetrievalModuleItem(String name, ModuleTier tier) {
        super(name);
        this.maxExtraction = tier.forTier(1, 8, 16);
        this.speed = tier.forTier(40, 20, 10);
        this.filterSlots = tier.forTier(3, 6, 9);
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {
        if (tile.getWorld().getGameTime() % this.speed != 0)
            return;
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        List<NetworkLocation> locations = null;

        ItemFilter filter = new ItemFilter(this.filterSlots, module, tile);
        filter.isWhitelist = true;
        // loop through filter to see which items to pull
        for (int f = 0; f < filter.getSlots(); f++) {
            ItemStack filtered = filter.getStackInSlot(f);
            if (filtered.isEmpty())
                continue;
            ItemStack copy = filtered.copy();
            copy.setCount(this.maxExtraction);
            BlockPos dest = tile.getAvailableDestination(copy, true);
            if (dest == null)
                continue;
            // loop through locations to find a location that has the item
            if (locations == null)
                locations = network.getOrderedNetworkItems(tile.getPos());
            for (NetworkLocation location : locations) {
                Pair<Direction, Integer> item = location.getStackLocation(filtered);
                if (item == null)
                    continue;
                // get that location's pipe and inventory
                PipeTileEntity pipe = network.getPipe(location.pipePos);
                IItemHandler handler = pipe.getItemHandler(item.getKey());
                // try to extract from that location's inventory and send the item
                ItemStack stack = handler.extractItem(item.getValue(), this.maxExtraction, true);
                if (network.routeItemToLocation(location.pipePos, location.pipePos.offset(item.getKey()), tile.getPos(), dest, speed -> new PipeItem(stack, speed))) {
                    handler.extractItem(item.getValue(), stack.getCount(), false);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return !(other instanceof RetrievalModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new RetrievalModuleContainer(Registry.retrievalModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }
}
