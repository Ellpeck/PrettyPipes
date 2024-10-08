package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;

public class PressurizerBlockEntity extends BlockEntity implements MenuProvider, IPipeConnectable {

    public final ModifiableEnergyStorage storage = new ModifiableEnergyStorage(64000, 512, 0);
    private int lastEnergy;

    public PressurizerBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.pressurizerBlockEntity, pos, state);
    }

    public boolean pressurizeItem(ItemStack stack, boolean simulate) {
        var amount = 100 * stack.getCount();
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
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        compound.putInt("energy", this.getEnergy());
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        this.storage.setEnergyStored(nbt.getInt("energy"));
        super.loadAdditional(nbt, provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadWithComponents(tag, provider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        this.loadWithComponents(pkt.getTag(), provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + PrettyPipes.ID + ".pressurizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int window, Inventory inv, Player player) {
        return new PressurizerContainer(Registry.pressurizerContainer, window, player, this.worldPosition);
    }

    @Override
    public ConnectionType getConnectionType(BlockPos pipePos, Direction direction) {
        return ConnectionType.CONNECTED;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PressurizerBlockEntity pressurizer) {
        // notify pipes in network about us
        if (pressurizer.level.getGameTime() % 10 == 0) {
            var network = PipeNetwork.get(pressurizer.level);
            for (var dir : Direction.values()) {
                var offset = pressurizer.worldPosition.relative(dir);
                for (var node : network.getOrderedNetworkNodes(offset)) {
                    if (!pressurizer.level.isLoaded(node))
                        continue;
                    var pipe = network.getPipe(node);
                    if (pipe != null)
                        pipe.pressurizer = pressurizer;
                }
            }
        }

        // send energy update and comparator output
        if (pressurizer.lastEnergy != pressurizer.storage.getEnergyStored() && pressurizer.level.getGameTime() % 10 == 0) {
            pressurizer.lastEnergy = pressurizer.storage.getEnergyStored();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            Utility.sendBlockEntityToClients(pressurizer);
        }
    }

    public static class ModifiableEnergyStorage extends EnergyStorage {

        public ModifiableEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        private void setEnergyStored(int energy) {
            this.energy = energy;
        }

        private int extractInternal(int maxExtract, boolean simulate) {
            var energyExtracted = Math.min(this.energy, maxExtract);
            if (!simulate)
                this.energy -= energyExtracted;
            return energyExtracted;
        }

    }

}
