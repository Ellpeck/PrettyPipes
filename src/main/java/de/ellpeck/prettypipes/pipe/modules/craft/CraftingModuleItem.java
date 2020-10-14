package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.ItemStackHandler;

public class CraftingModuleItem extends ModuleItem {

    public final int inputSlots;
    public final int outputSlots;

    public CraftingModuleItem(String name, ModuleTier tier) {
        super(name);
        this.inputSlots = tier.forTier(1, 4, 9);
        this.outputSlots = tier.forTier(1, 2, 4);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeTileEntity tile, IModule other) {
        return true;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return new CraftingModuleContainer(Registry.craftingModuleContainer, windowId, player, tile.getPos(), moduleIndex);
    }

    public ItemStackHandler getInput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.inputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("input"));
        return handler;
    }

    public ItemStackHandler getOutput(ItemStack module) {
        ItemStackHandler handler = new ItemStackHandler(this.outputSlots);
        if (module.hasTag())
            handler.deserializeNBT(module.getTag().getCompound("output"));
        return handler;
    }

    public void save(ItemStackHandler input, ItemStackHandler output, ItemStack module) {
        CompoundNBT tag = module.getOrCreateTag();
        if (input != null)
            tag.put("input", input.serializeNBT());
        if (output != null)
            tag.put("output", output.serializeNBT());
    }
}
