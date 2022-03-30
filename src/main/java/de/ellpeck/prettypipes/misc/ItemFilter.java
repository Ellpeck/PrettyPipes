package de.ellpeck.prettypipes.misc;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleItem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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
    private boolean modified;

    public ItemFilter(int size, ItemStack stack, PipeTileEntity pipe) {
        super(size);
        this.stack = stack;
        this.pipe = pipe;
        this.load();
    }

    public List<Slot> getSlots(int x, int y) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < this.getSlots(); i++)
            slots.add(new FilterSlot(this, i, x + i % 9 * 18, y + i / 9 * 18, true));
        return slots;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Widget> getButtons(Screen gui, int x, int y) {
        List<Widget> buttons = new ArrayList<>();
        if (this.canModifyWhitelist) {
            Supplier<TranslationTextComponent> whitelistText = () -> new TranslationTextComponent("info." + PrettyPipes.ID + "." + (this.isWhitelist ? "whitelist" : "blacklist"));
            buttons.add(new Button(x, y, 70, 20, whitelistText.get(), button -> {
                PacketButton.sendAndExecute(this.pipe.getPos(), PacketButton.ButtonResult.FILTER_CHANGE, 0);
                button.setMessage(whitelistText.get());
            }));
        }
        if (this.canPopulateFromInventories) {
            buttons.add(new Button(x + 72, y, 70, 20, new TranslationTextComponent("info." + PrettyPipes.ID + ".populate"), button -> PacketButton.sendAndExecute(this.pipe.getPos(), PacketButton.ButtonResult.FILTER_CHANGE, 1)) {
                @Override
                public void renderToolTip(MatrixStack matrix, int x, int y) {
                    gui.renderTooltip(matrix, new TranslationTextComponent("info." + PrettyPipes.ID + ".populate.description").mergeStyle(TextFormatting.GRAY), x, y);
                }
            });
        }
        return buttons;
    }

    public void onButtonPacket(IFilteredContainer menu, int id) {
        if (id == 0 && this.canModifyWhitelist) {
            this.isWhitelist = !this.isWhitelist;
            this.modified = true;
            this.save();
        } else if (id == 1 && this.canPopulateFromInventories) {
            boolean changed = false;
            // populate filter from inventories
            List<ItemFilter> filters = this.pipe.getFilters();
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
                    // try inserting into ourselves and any filter increase modifiers
                    for (ItemFilter filter : filters) {
                        if (ItemHandlerHelper.insertItem(filter, copy, false).isEmpty()) {
                            changed = true;
                            filter.save();
                            break;
                        }
                    }
                }
            }
            if (changed)
                menu.onFilterPopulated();
        }
    }

    public boolean isAllowed(ItemStack stack) {
        return this.isFiltered(stack) == this.isWhitelist;
    }

    private boolean isFiltered(ItemStack stack) {
        ItemEquality[] types = getEqualityTypes(this.pipe);
        // also check if any filter increase modules have the item we need
        for (ItemStackHandler handler : this.pipe.getFilters()) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack filter = handler.getStackInSlot(i);
                if (filter.isEmpty())
                    continue;
                if (ItemEquality.compareItems(stack, filter, types))
                    return true;
            }
        }
        return false;
    }

    public void save() {
        if (this.modified) {
            this.stack.getOrCreateTag().put("filter", this.serializeNBT());
            this.modified = false;
        }
    }

    public void load() {
        if (this.stack.hasTag())
            this.deserializeNBT(this.stack.getTag().getCompound("filter"));
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = super.serializeNBT();
        if (this.canModifyWhitelist)
            nbt.putBoolean("whitelist", this.isWhitelist);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        super.deserializeNBT(nbt);
        if (this.canModifyWhitelist)
            this.isWhitelist = nbt.getBoolean("whitelist");
    }

    @Override
    protected void onContentsChanged(int slot) {
        this.modified = true;
    }

    public static ItemEquality[] getEqualityTypes(PipeTileEntity pipe) {
        return pipe.streamModules()
                .filter(m -> m.getRight() instanceof FilterModifierModuleItem)
                .map(m -> ((FilterModifierModuleItem) m.getRight()).getEqualityType(m.getLeft()))
                .toArray(ItemEquality[]::new);
    }

    public interface IFilteredContainer {

        ItemFilter getFilter();

        default void onFilterPopulated() {}

    }
}
