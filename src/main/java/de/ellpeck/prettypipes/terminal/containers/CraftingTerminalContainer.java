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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

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
    protected void addOwnSlots(Player player) {
        this.craftInventory = new WrappedCraftingInventory(this.getTile().craftItems, this);
        this.craftResult = new ResultContainer() {
            @Override
            public void setChanged() {
                for (var player : CraftingTerminalContainer.this.getTile().getLookingPlayers())
                    player.containerMenu.slotsChanged(this);
            }
        };
        this.addSlot(new ResultSlot(player, this.craftInventory, this.craftResult, 0, 25, 77));
        for (var i = 0; i < 3; i++)
            for (var j = 0; j < 3; j++)
                this.addSlot(new Slot(this.craftInventory, j + i * 3, 7 + j * 18, 18 + i * 18));
        super.addOwnSlots(player);
    }

    @Override
    public void slotsChanged(Container inventoryIn) {
        super.slotsChanged(inventoryIn);
        CraftingTerminalContainer.slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftInventory, this.craftResult, null);
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
        if (slotId > 0) {
            var ghostItems = this.getTile().ghostItems;
            if (clickTypeIn == ClickType.PICKUP) {
                var slot = this.slots.get(slotId);
                if (slot.container == this.craftInventory && !slot.hasItem())
                    ghostItems.setStackInSlot(slot.getSlotIndex(), ItemStack.EMPTY);
            } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                // clear the entire grid when holding shift
                for (var i = 0; i < ghostItems.getSlots(); i++)
                    ghostItems.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        super.clicked(slotId, dragType, clickTypeIn, player);
    }

    public CraftingTerminalBlockEntity getTile() {
        return (CraftingTerminalBlockEntity) this.tile;
    }

    // copied from CraftingMenu
    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        if (!level.isClientSide) {
            var craftinginput = craftSlots.asCraftInput();
            var serverplayer = (ServerPlayer) player;
            var itemstack = ItemStack.EMPTY;
            var optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftinginput, level, recipe);
            if (optional.isPresent()) {
                var recipeholder = optional.get();
                var craftingrecipe = recipeholder.value();
                if (resultSlots.setRecipeUsed(level, serverplayer, recipeholder)) {
                    var itemstack1 = craftingrecipe.assemble(craftinginput, level.registryAccess());
                    if (itemstack1.isItemEnabled(level.enabledFeatures())) {
                        itemstack = itemstack1;
                    }
                }
            }

            resultSlots.setItem(0, itemstack);
            menu.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
        }
    }

}
