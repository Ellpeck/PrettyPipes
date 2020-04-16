package de.ellpeck.prettypipes.pipe.extraction;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

public class ExtractionModuleItem extends ModuleItem {

    private final int maxExtraction;
    private final int speed;
    public final int filterSlots;

    public ExtractionModuleItem(String name, ModuleTier tier) {
        super(name);
        this.maxExtraction = tier.forTier(1, 8, 64);
        this.speed = tier.forTier(20, 15, 10);
        this.filterSlots = tier.forTier(3, 6, 9);
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {
        if (tile.getWorld().getGameTime() % this.speed != 0)
            return;
        ItemFilter filter = new ItemFilter(this.filterSlots, module);

        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        for (Direction dir : Direction.values()) {
            IItemHandler handler = tile.getItemHandler(dir);
            if (handler == null)
                continue;
            for (int j = 0; j < handler.getSlots(); j++) {
                ItemStack stack = handler.extractItem(j, this.maxExtraction, true);
                if (stack.isEmpty())
                    continue;
                if (!filter.isAllowed(stack))
                    continue;
                if (network.tryInsertItem(tile.getPos(), tile.getPos().offset(dir), stack)) {
                    handler.extractItem(j, this.maxExtraction, false);
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
        return !(other instanceof ExtractionModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new ExtractionModuleContainer(Registry.extractionModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }
}
