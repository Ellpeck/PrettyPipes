package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CraftingModuleContainer extends AbstractPipeContainer<CraftingModuleItem> {

    public ItemStackHandler input;
    public ItemStackHandler output;
    public boolean modified;

    public CraftingModuleContainer(MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    @Override
    protected void addSlots() {
        var contents = this.moduleStack.get(CraftingModuleItem.Contents.TYPE);
        this.input = Utility.copy(contents.input());
        for (var i = 0; i < this.input.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.input, i, (176 - this.input.getSlots() * 18) / 2 + 1 + i % 9 * 18, 17 + 32 + i / 9 * 18, false) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    CraftingModuleContainer.this.modified = true;
                }

            });
        }

        this.output = Utility.copy(contents.output());
        for (var i = 0; i < this.output.getSlots(); i++) {
            this.addSlot(new FilterSlot(this.output, i, (176 - this.output.getSlots() * 18) / 2 + 1 + i % 9 * 18, 85 + i / 9 * 18, false) {
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
        if (this.modified) {
            this.moduleStack.set(CraftingModuleItem.Contents.TYPE, new CraftingModuleItem.Contents(this.input, this.output));
            this.tile.setChanged();
        }
    }

}
