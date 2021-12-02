package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Optional;

public class CraftingTerminalContainer extends ItemTerminalContainer {

    public CraftingContainer craftInventory;
    public ResultContainer craftResult;
    private final Player player;

    public CraftingTerminalContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id, player, pos);
        this.player = player;
        this.slotsChanged(this.craftInventory);
    }

    @Override
    protected void addDataSlots(ContainerData data) {
        this.craftInventory = new WrappedCraftingInventory(this.getTile().craftItems, this, 3, 3);
        this.craftResult = new ResultContainer();
        this.addSlot(new ResultSlot(this.player, this.craftInventory, this.craftResult, 0, 25, 77));
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                this.addSlot(new Slot(this.craftInventory, j + i * 3, 7 + j * 18, 18 + i * 18));
        super.addDataSlots(data);
    }

    @Override
    public void slotsChanged(Container inventoryIn) {
        if (!this.player.level.isClientSide) {
            ItemStack ret = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = this.player.level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftInventory, this.player.level);
            if (optional.isPresent())
                ret = optional.get().assemble(this.craftInventory);
            this.craftResult.setItem(0, ret);
            ((ServerPlayer) this.player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, 0, 0, ret));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return Utility.transferStackInSlot(this, this::moveItemStackTo, player, slotIndex, stack -> Pair.of(6 + 10, 12 + 10));
    }

    @Override
    protected int getSlotXOffset() {
        return 65;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        if (slotId > 0 && clickTypeIn == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotId);
            if (slot.container == this.craftInventory && !slot.hasItem())
                this.getTile().ghostItems.setStackInSlot(slot.getSlotIndex(), ItemStack.EMPTY);
        }
        super.clicked(slotId, dragType, clickTypeIn, player);
    }

    public CraftingTerminalBlockEntity getTile() {
        return (CraftingTerminalBlockEntity) this.tile;
    }
}
