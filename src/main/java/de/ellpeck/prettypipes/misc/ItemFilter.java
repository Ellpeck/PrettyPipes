package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ItemFilter extends ItemStackHandler {

    private final ItemStack stack;
    private boolean isWhitelist;

    public ItemFilter(int size, ItemStack stack) {
        super(size);
        this.stack = stack;
        if (this.stack != null && this.stack.hasTag())
            this.deserializeNBT(this.stack.getTag().getCompound("filter"));
    }

    public List<Slot> getSlots(int x, int y) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < this.getSlots(); i++) {
            slots.add(new SlotFilter(this, i, x, y));
            x += 18;
        }
        return slots;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Widget> getButtons(int x, int y) {
        Supplier<String> whitelistText = () -> I18n.format("info." + PrettyPipes.ID + "." + (this.isWhitelist ? "whitelist" : "blacklist"));
        return Collections.singletonList(
                new Button(x, y, 80, 20, whitelistText.get(), button -> {
                    // TODO actually make whitelist button work
                }));
    }

    public boolean isAllowed(ItemStack stack) {
        for (int i = 0; i < this.getSlots(); i++) {
            ItemStack other = this.getStackInSlot(i);
            if (ItemHandlerHelper.canItemStacksStack(stack, other)) {
                // if we're whitelist, then this is true -> item is allowed
                return this.isWhitelist;
            }
        }
        // if we're whitelist, then this is false -> item is disallowed
        return !this.isWhitelist;
    }

    public void save() {
        if (this.stack != null)
            this.stack.getOrCreateTag().put("filter", this.serializeNBT());
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = super.serializeNBT();
        nbt.putBoolean("whitelist", this.isWhitelist);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        super.deserializeNBT(nbt);
        this.isWhitelist = nbt.getBoolean("whitelist");
    }
}
