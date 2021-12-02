package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.ItemStackHandler;

public class CraftingModuleContainer extends AbstractPipeContainer<CraftingModuleItem> {

    public ItemStackHandler input;
    public ItemStackHandler output;
    public boolean modified;

    public CraftingModuleContainer(MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        this.input = this.module.getInput(this.moduleStack);
        for (int i = 0; i < this.input.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.input, i, (176 - this.module.inputSlots * 18) / 2 + 1 + i % 9 * 18, 17 + 32 + i / 9 * 18, false) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    CraftingModuleContainer.this.modified = true;
                }

            });
        }

        this.output = this.module.getOutput(this.moduleStack);
        for (int i = 0; i < this.output.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.output, i, (176 - this.module.outputSlots * 18) / 2 + 1 + i % 9 * 18, 85 + i / 9 * 18, false) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    CraftingModuleContainer.this.modified = true;
                }
            });
        }
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        if (this.modified)
            this.module.save(this.input, this.output, this.moduleStack);
    }
}
