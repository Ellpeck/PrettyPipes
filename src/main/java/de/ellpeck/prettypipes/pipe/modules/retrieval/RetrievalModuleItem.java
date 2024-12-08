package de.ellpeck.prettypipes.pipe.modules.retrieval;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.DirectionSelector;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;

public class RetrievalModuleItem extends ModuleItem {

    private final int maxExtraction;
    private final int speed;
    private final boolean preventOversending;
    public final int filterSlots;

    public RetrievalModuleItem(String name, ModuleTier tier) {
        super(name, new Properties());
        this.maxExtraction = tier.forTier(1, 8, 16);
        this.speed = tier.forTier(40, 20, 10);
        this.filterSlots = tier.forTier(3, 6, 9);
        this.preventOversending = tier.forTier(false, true, true);
    }

    @Override
    public void tick(ItemStack module, PipeBlockEntity tile) {
        if (!tile.shouldWorkNow(this.speed) || !tile.canWork())
            return;
        var directions = this.getDirectionSelector(module, tile).directions();
        var network = PipeNetwork.get(tile.getLevel());
        var equalityTypes = ItemFilter.getEqualityTypes(tile);
        // loop through filters to see which items to pull
        Arrays.stream(directions).flatMap(d -> tile.getFilters(d).stream()).distinct().forEach(f -> {
            for (var i = 0; i < f.content.getSlots(); i++) {
                var filtered = f.content.getStackInSlot(i);
                if (filtered.isEmpty())
                    continue;
                var copy = filtered.copy();
                copy.setCount(this.maxExtraction);
                var dest = tile.getAvailableDestination(directions, copy, true, this.preventOversending);
                if (dest == null)
                    continue;
                var remain = dest.getRight().copy();
                // are we already waiting for crafting results? If so, don't request those again
                remain.shrink(network.getCurrentlyCraftingAmount(tile.getBlockPos(), copy, true, equalityTypes));
                if (network.requestItem(tile.getBlockPos(), dest.getLeft(), remain, equalityTypes).isEmpty())
                    break;
            }
        });
    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeBlockEntity tile, Direction direction, IItemHandler handler) {
        return !this.getDirectionSelector(module, tile).has(direction);
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction, IItemHandler destination) {
        return !this.getDirectionSelector(module, tile).has(direction);
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
        var filter = new ItemFilter(this.filterSlots, module, tile);
        filter.canModifyWhitelist = false;
        filter.isWhitelist = true;
        return filter;
    }

    @Override
    public DirectionSelector getDirectionSelector(ItemStack module, PipeBlockEntity tile) {
        return new DirectionSelector(module, tile);
    }

}
