package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class PressurizerBlockEntity extends BlockEntity implements MenuProvider, IPipeConnectable {

    private final ModifiableEnergyStorage storage = new ModifiableEnergyStorage(64000, 512, 0);
    private final LazyOptional<IEnergyStorage> lazyStorage = LazyOptional.of(() -> this.storage);
    private final LazyOptional<IPipeConnectable> lazyThis = LazyOptional.of(() -> this);
    private int lastEnergy;

    public PressurizerBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.pressurizerBlockEntity, pos, state);
    }

    public boolean pressurizeItem(ItemStack stack, boolean simulate) {
        int amount = 100 * stack.getCount();
        return this.storage.extractInternal(amount, simulate) >= amount;
    }

    public float getEnergyPercentage() {
        return this.getEnergy() / (float) this.getMaxEnergy();
    }

    public int getEnergy() {
        return this.storage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return this.storage.getMaxEnergyStored();
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.putInt("energy", this.getEnergy());
        return super.save(compound);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.storage.setEnergyStored(nbt.getInt("energy"));
        super.load(nbt);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + PrettyPipes.ID + ".pressurizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new PressurizerContainer(Registry.pressurizerContainer, window, player, this.worldPosition);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return this.lazyStorage.cast();
        } else if (cap == Registry.pipeConnectableCapability) {
            return this.lazyThis.cast();
        } else {
            return LazyOptional.empty();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.lazyStorage.invalidate();
        this.lazyThis.invalidate();
    }

    // TODO tick
    /*@Override
    public void tick() {
        if (this.world.isRemote)
            return;
        // notify pipes in network about us
        if (this.world.getGameTime() % 10 == 0) {
            PipeNetwork network = PipeNetwork.get(this.world);
            for (Direction dir : Direction.values()) {
                BlockPos offset = this.pos.offset(dir);
                for (BlockPos node : network.getOrderedNetworkNodes(offset)) {
                    if (!this.world.isBlockLoaded(node))
                        continue;
                    PipeBlockEntity pipe = network.getPipe(node);
                    if (pipe != null)
                        pipe.pressurizer = this;
                }
            }
        }

        // send energy update
        if (this.lastEnergy != this.storage.getEnergyStored() && this.world.getGameTime() % 10 == 0) {
            this.lastEnergy = this.storage.getEnergyStored();
            Utility.sendBlockEntityToClients(this);
        }
    }*/

    @Override
    public ConnectionType getConnectionType(BlockPos pipePos, Direction direction) {
        return ConnectionType.CONNECTED;
    }

    private static class ModifiableEnergyStorage extends EnergyStorage {

        public ModifiableEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        private void setEnergyStored(int energy) {
            this.energy = energy;
        }

        private int extractInternal(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(this.energy, maxExtract);
            if (!simulate)
                this.energy -= energyExtracted;
            return energyExtracted;
        }
    }
}
