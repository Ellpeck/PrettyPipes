package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ItemFilter extends ItemStackHandler {

    private final ItemStack stack;
    private final PipeTileEntity pipe;
    public boolean isWhitelist;

    public boolean canPopulateFromInventories;
    public boolean canModifyWhitelist = true;

    public ItemFilter(int size, ItemStack stack, PipeTileEntity pipe) {
        super(size);
        this.stack = stack;
        this.pipe = pipe;
        if (stack.hasTag())
            this.deserializeNBT(stack.getTag().getCompound("filter"));
    }

    public List<Slot> getSlots(int x, int y) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < this.getSlots(); i++)
            slots.add(new SlotFilter(this, i, x + i % 9 * 18, y + i / 9 * 18));
        return slots;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Widget> getButtons(Screen gui, int x, int y) {
        List<Widget> buttons = new ArrayList<>();
        if (this.canModifyWhitelist) {
            Supplier<String> whitelistText = () -> I18n.format("info." + PrettyPipes.ID + "." + (this.isWhitelist ? "whitelist" : "blacklist"));
            buttons.add(new Button(x, y, 70, 20, whitelistText.get(), button -> {
                PacketButton.sendAndExecute(this.pipe.getPos(), PacketButton.ButtonResult.FILTER_CHANGE, 0);
                button.setMessage(whitelistText.get());
            }));
        }
        if (this.canPopulateFromInventories) {
            buttons.add(new Button(x + 72, y, 70, 20, I18n.format("info." + PrettyPipes.ID + ".populate"), button -> PacketButton.sendAndExecute(this.pipe.getPos(), PacketButton.ButtonResult.FILTER_CHANGE, 1)) {
                @Override
                public void renderToolTip(int x, int y) {
                    gui.renderTooltip(TextFormatting.GRAY + I18n.format("info." + PrettyPipes.ID + ".populate.description"), x, y);
                }
            });
        }
        return buttons;
    }

    public void onButtonPacket(int id) {
        if (id == 0 && this.canModifyWhitelist) {
            this.isWhitelist = !this.isWhitelist;
        } else if (id == 1 && this.canPopulateFromInventories) {
            // populate filter from inventories
            for (Direction direction : Direction.values()) {
                IItemHandler handler = this.pipe.getItemHandler(direction);
                if (handler == null)
                    continue;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (stack.isEmpty() || this.isFiltered(stack))
                        continue;
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    ItemHandlerHelper.insertItem(this, copy, false);
                }
            }
        }
        this.save();
    }

    public boolean isAllowed(ItemStack stack) {
        return this.isFiltered(stack) == this.isWhitelist;
    }

    private boolean isFiltered(ItemStack stack) {
        for (int i = 0; i < this.getSlots(); i++) {
            ItemStack other = this.getStackInSlot(i);
            if (ItemHandlerHelper.canItemStacksStack(stack, other))
                return true;
        }
        return false;
    }

    public void save() {
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

    public interface IFilteredContainer {
        ItemFilter getFilter();
    }
}
