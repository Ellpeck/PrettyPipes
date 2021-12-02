package de.ellpeck.prettypipes.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ItemFilter extends ItemStackHandler {

    private final ItemStack stack;
    private final PipeBlockEntity pipe;
    public boolean isWhitelist;

    public boolean canPopulateFromInventories;
    public boolean canModifyWhitelist = true;
    private boolean modified;

    public ItemFilter(int size, ItemStack stack, PipeBlockEntity pipe) {
        super(size);
        this.stack = stack;
        this.pipe = pipe;
        if (stack.hasTag())
            this.deserializeNBT(stack.getTag().getCompound("filter"));
    }

    public List<Slot> getSlots(int x, int y) {
        List<Slot> slots = new ArrayList<>();
        for (var i = 0; i < this.getSlots(); i++)
            slots.add(new FilterSlot(this, i, x + i % 9 * 18, y + i / 9 * 18, true));
        return slots;
    }

    @OnlyIn(Dist.CLIENT)
    public List<AbstractWidget> getButtons(Screen gui, int x, int y) {
        List<AbstractWidget> buttons = new ArrayList<>();
        if (this.canModifyWhitelist) {
            var whitelistText = (Supplier<TranslatableComponent>) () -> new TranslatableComponent("info." + PrettyPipes.ID + "." + (this.isWhitelist ? "whitelist" : "blacklist"));
            buttons.add(new Button(x, y, 70, 20, whitelistText.get(), button -> {
                PacketButton.sendAndExecute(this.pipe.getBlockPos(), PacketButton.ButtonResult.FILTER_CHANGE, 0);
                button.setMessage(whitelistText.get());
            }));
        }
        if (this.canPopulateFromInventories) {
            buttons.add(new Button(x + 72, y, 70, 20, new TranslatableComponent("info." + PrettyPipes.ID + ".populate"), button -> PacketButton.sendAndExecute(this.pipe.getBlockPos(), PacketButton.ButtonResult.FILTER_CHANGE, 1)) {
                @Override
                public void renderToolTip(PoseStack matrix, int x, int y) {
                    gui.renderTooltip(matrix, new TranslatableComponent("info." + PrettyPipes.ID + ".populate.description").withStyle(ChatFormatting.GRAY), x, y);
                }
            });
        }
        return buttons;
    }

    public void onButtonPacket(int id) {
        if (id == 0 && this.canModifyWhitelist) {
            this.isWhitelist = !this.isWhitelist;
            this.modified = true;
            this.save();
        } else if (id == 1 && this.canPopulateFromInventories) {
            // populate filter from inventories
            var filters = this.pipe.getFilters();
            for (var direction : Direction.values()) {
                var handler = this.pipe.getItemHandler(direction);
                if (handler == null)
                    continue;
                for (var i = 0; i < handler.getSlots(); i++) {
                    var stack = handler.getStackInSlot(i);
                    if (stack.isEmpty() || this.isFiltered(stack))
                        continue;
                    var copy = stack.copy();
                    copy.setCount(1);
                    // try inserting into ourselves and any filter increase modifiers
                    for (var filter : filters) {
                        if (ItemHandlerHelper.insertItem(filter, copy, false).isEmpty()) {
                            filter.save();
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean isAllowed(ItemStack stack) {
        return this.isFiltered(stack) == this.isWhitelist;
    }

    private boolean isFiltered(ItemStack stack) {
        var types = getEqualityTypes(this.pipe);
        // also check if any filter increase modules have the item we need
        for (ItemStackHandler handler : this.pipe.getFilters()) {
            for (var i = 0; i < handler.getSlots(); i++) {
                var filter = handler.getStackInSlot(i);
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

    @Override
    public CompoundTag serializeNBT() {
        var nbt = super.serializeNBT();
        if (this.canModifyWhitelist)
            nbt.putBoolean("whitelist", this.isWhitelist);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        if (this.canModifyWhitelist)
            this.isWhitelist = nbt.getBoolean("whitelist");
    }

    @Override
    protected void onContentsChanged(int slot) {
        this.modified = true;
    }

    public static ItemEquality[] getEqualityTypes(PipeBlockEntity pipe) {
        return pipe.streamModules()
                .filter(m -> m.getRight() instanceof FilterModifierModuleItem)
                .map(m -> ((FilterModifierModuleItem) m.getRight()).getEqualityType(m.getLeft()))
                .toArray(ItemEquality[]::new);
    }

    public interface IFilteredContainer {

        ItemFilter getFilter();
    }
}
