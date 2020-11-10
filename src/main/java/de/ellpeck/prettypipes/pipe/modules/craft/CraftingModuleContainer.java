package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

public class CraftingModuleContainer extends AbstractPipeContainer<CraftingModuleItem> {

    public ItemStackHandler input;
    public ItemStackHandler output;
    public boolean modified;

    public CraftingModuleContainer(ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.input = this.module.getInput(this.moduleStack);
        for (int i = 0; i < this.input.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.input, i, (176 - this.module.inputSlots * 18) / 2 + 1 + i % 9 * 18, 17 + 32 + i / 9 * 18, false) {
                @Override
                public void onSlotChanged() {
                    super.onSlotChanged();
                    CraftingModuleContainer.this.modified = true;
                }
            });
        }

        this.output = this.module.getOutput(this.moduleStack);
        for (int i = 0; i < this.output.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.output, i, (176 - this.module.outputSlots * 18) / 2 + 1 + i % 9 * 18, 85 + i / 9 * 18, false) {
                @Override
                public void onSlotChanged() {
                    super.onSlotChanged();
                    CraftingModuleContainer.this.modified = true;
                }
            });
        }
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        if (this.modified)
            this.module.save(this.input, this.output, this.moduleStack);
    }
}
