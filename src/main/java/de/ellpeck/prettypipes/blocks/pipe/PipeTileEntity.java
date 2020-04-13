package de.ellpeck.prettypipes.blocks.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.UpgradeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PipeTileEntity extends TileEntity implements INamedContainerProvider {

    public final ItemStackHandler upgrades = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }
    };

    public PipeTileEntity() {
        super(Registry.pipeTileEntity);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".pipe");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new PipeContainer(Registry.pipeContainer, window, player, this);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("upgrades", this.upgrades.serializeNBT());
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.upgrades.deserializeNBT(compound.getCompound("upgrades"));
        super.read(compound);
    }
}
