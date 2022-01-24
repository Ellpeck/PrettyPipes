package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Optional;

public class CraftingTerminalContainer extends ItemTerminalContainer {

    public CraftingInventory craftInventory;
    public CraftResultInventory craftResult;
    private final PlayerEntity player;

    public CraftingTerminalContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos) {
        super(type, id, player, pos);
        this.player = player;
        this.onCraftMatrixChanged(this.craftInventory);
    }

    @Override
    protected void addOwnSlots(PlayerEntity player) {
        this.craftInventory = new WrappedCraftingInventory(this.getTile().craftItems, this, 3, 3);
        this.craftResult = new CraftResultInventory() {
            @Override
            public void markDirty() {
                for (PlayerEntity player : CraftingTerminalContainer.this.getTile().getLookingPlayers())
                    player.openContainer.onCraftMatrixChanged(this);
            }
        };
        this.addSlot(new CraftingResultSlot(player, this.craftInventory, this.craftResult, 0, 25, 77));
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                this.addSlot(new Slot(this.craftInventory, j + i * 3, 7 + j * 18, 18 + i * 18));
        super.addOwnSlots(player);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        super.onCraftMatrixChanged(inventoryIn);
        if (!this.player.world.isRemote) {
            ItemStack ret = ItemStack.EMPTY;
            Optional<ICraftingRecipe> optional = this.player.world.getServer().getRecipeManager().getRecipe(IRecipeType.CRAFTING, this.craftInventory, this.player.world);
            if (optional.isPresent())
                ret = optional.get().getCraftingResult(this.craftInventory);
            this.craftResult.setInventorySlotContents(0, ret);
            ((ServerPlayerEntity) this.player).connection.sendPacket(new SSetSlotPacket(this.windowId, 0, ret));
        }
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::mergeItemStack, player, slotIndex, stack -> Pair.of(6 + 10, 12 + 10));
    }

    @Override
    protected int getSlotXOffset() {
        return 65;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
        if (slotId > 0 && clickTypeIn == ClickType.PICKUP) {
            Slot slot = this.inventorySlots.get(slotId);
            if (slot.inventory == this.craftInventory && !slot.getHasStack())
                this.getTile().ghostItems.setStackInSlot(slot.getSlotIndex(), ItemStack.EMPTY);
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    public CraftingTerminalTileEntity getTile() {
        return (CraftingTerminalTileEntity) this.tile;
    }
}
