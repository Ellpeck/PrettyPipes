package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

public class ExtractionModuleItem extends ModuleItem {

    private final int maxExtraction;
    private final int speed;

    public ExtractionModuleItem(ModuleTier tier) {
        this.maxExtraction = tier.forTier(1, 8, 64);
        this.speed = tier.forTier(20, 15, 10);
    }

    @Override
    public void tick(PipeTileEntity tile) {
        if (tile.getWorld().getGameTime() % this.speed != 0)
            return;
        PipeNetwork network = PipeNetwork.get(tile.getWorld());
        for (Direction dir : Direction.values()) {
            IItemHandler handler = tile.getItemHandler(dir);
            if (handler == null)
                continue;
            for (int j = 0; j < handler.getSlots(); j++) {
                ItemStack stack = handler.extractItem(j, this.maxExtraction, true);
                if (!stack.isEmpty() && network.tryInsertItem(tile.getPos(), tile.getPos().offset(dir), stack)) {
                    handler.extractItem(j, this.maxExtraction, false);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canAcceptItem(PipeTileEntity tile, ItemStack stack) {
        return false;
    }
}
