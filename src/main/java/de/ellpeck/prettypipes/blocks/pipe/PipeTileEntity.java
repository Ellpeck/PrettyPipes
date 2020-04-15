package de.ellpeck.prettypipes.blocks.pipe;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.UpgradeItem;
import de.ellpeck.prettypipes.network.NetworkEdge;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PipeTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity {

    public final ItemStackHandler upgrades = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }
    };
    public final List<PipeItem> items = new ArrayList<>();

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
        ListNBT list = new ListNBT();
        for (PipeItem item : this.items)
            list.add(item.serializeNBT());
        compound.put("items", list);
        return super.write(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        this.upgrades.deserializeNBT(compound.getCompound("upgrades"));
        this.items.clear();
        ListNBT list = compound.getList("items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            this.items.add(new PipeItem(list.getCompound(i)));
        super.read(compound);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        // by default, this is just writeInternal, but we
        // want to sync the current pipe items on load too
        return this.write(new CompoundNBT());
    }

    @Override
    public void tick() {
        if (!this.world.isAreaLoaded(this.pos, 1))
            return;

        for (int i = this.items.size() - 1; i >= 0; i--)
            this.items.get(i).updateInPipe(this);

        // TODO make this extraction module stuff proper
        PipeNetwork network = PipeNetwork.get(this.world);
        for (int i = 0; i < this.upgrades.getSlots(); i++) {
            if (this.upgrades.getStackInSlot(i).getItem() != Registry.extractionUpgradeItem)
                continue;
            BlockState state = this.getBlockState();
            for (Direction dir : Direction.values()) {
                if (!state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
                    continue;
                IItemHandler handler = this.getItemHandler(dir);
                if (handler != null) {
                    for (int j = 0; j < handler.getSlots(); j++) {
                        ItemStack stack = handler.extractItem(j, 64, true);
                        if (!stack.isEmpty() && network.tryInsertItem(this.pos, this.pos.offset(dir), stack)) {
                            handler.extractItem(j, 64, false);
                            return;
                        }
                    }
                }
            }
            return;
        }
    }

    public BlockPos getAvailableDestination(ItemStack stack) {
        for (int i = 0; i < this.upgrades.getSlots(); i++) {
            if (this.upgrades.getStackInSlot(i).getItem() == Registry.extractionUpgradeItem)
                return null;
        }
        for (Direction dir : Direction.values()) {
            IItemHandler handler = this.getItemHandler(dir);
            if (handler == null)
                continue;
            if (ItemHandlerHelper.insertItem(handler, stack, true).isEmpty())
                return this.pos.offset(dir);
        }
        return null;
    }

    // TODO module priority
    public int getPriority() {
        return 0;
    }

    private IItemHandler getItemHandler(Direction dir) {
        BlockState state = this.getBlockState();
        if (!state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
            return null;
        TileEntity tile = this.world.getTileEntity(this.pos.offset(dir));
        if (tile == null)
            return null;
        return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).orElse(null);
    }

    public boolean isConnectedInventory(Direction dir) {
        return this.getItemHandler(dir) != null;
    }

    public boolean isConnectedInventory() {
        return Arrays.stream(Direction.values()).anyMatch(this::isConnectedInventory);
    }
}
