package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class PressurizerTileEntity extends TileEntity implements INamedContainerProvider, ITickableTileEntity, IPipeConnectable {

    private final ModifiableEnergyStorage storage = new ModifiableEnergyStorage(64000, 512, 0);
    private int lastEnergy;

    public PressurizerTileEntity() {
        super(Registry.pressurizerTileEntity);
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
    public CompoundNBT write(CompoundNBT compound) {
        compound.putInt("energy", this.getEnergy());
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        this.storage.setEnergyStored(nbt.getInt("energy"));
        super.read(state, nbt);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.read(state, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        this.read(this.getBlockState(), pkt.getNbtCompound());
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container." + PrettyPipes.ID + ".pressurizer");
    }

    @Nullable
    @Override
    public Container createMenu(int window, PlayerInventory inv, PlayerEntity player) {
        return new PressurizerContainer(Registry.pressurizerContainer, window, player, this.pos);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> (T) this.storage);
        } else if (cap == Registry.pipeConnectableCapability) {
            return LazyOptional.of(() -> (T) this);
        } else {
            return LazyOptional.empty();
        }
    }

    @Override
    public void tick() {
        if (this.world.isRemote)
            return;
        // notify pipes in network about us
        if (this.world.getGameTime() % 10 == 0) {
            PipeNetwork network = PipeNetwork.get(this.world);
            for (Direction dir : Direction.values()) {
                BlockPos offset = this.pos.offset(dir);
                for (BlockPos node : network.getOrderedNetworkNodes(offset)) {
                    PipeTileEntity pipe = network.getPipe(node);
                    if (pipe != null)
                        pipe.pressurizer = this;
                }
            }
        }

        // send energy update
        if (this.lastEnergy != this.storage.getEnergyStored() && this.world.getGameTime() % 10 == 0) {
            this.lastEnergy = this.storage.getEnergyStored();
            Utility.sendTileEntityToClients(this);
        }
    }

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
