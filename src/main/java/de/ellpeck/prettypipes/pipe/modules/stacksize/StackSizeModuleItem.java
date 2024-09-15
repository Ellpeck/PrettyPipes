package de.ellpeck.prettypipes.pipe.modules.stacksize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class StackSizeModuleItem extends ModuleItem {

    public StackSizeModuleItem() {
        super("stack_size_module", new Properties());
    }

    @Override
    public int getMaxInsertionAmount(ItemStack module, PipeBlockEntity tile, ItemStack stack, IItemHandler destination) {
        var types = ItemFilter.getEqualityTypes(tile);
        var data = module.getOrDefault(Data.TYPE, Data.DEFAULT);
        var max = data.maxStackSize;
        if (data.limitToMaxStackSize)
            max = Math.min(max, stack.getMaxStackSize());
        var amount = 0;
        for (var i = 0; i < destination.getSlots(); i++) {
            var stored = destination.getStackInSlot(i);
            if (stored.isEmpty())
                continue;
            if (!ItemEquality.compareItems(stored, stack, types))
                continue;
            amount += stored.getCount();
            if (amount >= max)
                return 0;
        }
        return max - amount;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof StackSizeModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex) {
        return new StackSizeModuleContainer(Registry.stackSizeModuleContainer, windowId, player, tile.getBlockPos(), moduleIndex);
    }

    public record Data(int maxStackSize, boolean limitToMaxStackSize) {

        public static final Data DEFAULT = new Data(64, false);

        public static final Codec<StackSizeModuleItem.Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("max_stack_size").forGetter(f -> f.maxStackSize),
            Codec.BOOL.fieldOf("limit_to_max_stack_size").forGetter(f -> f.limitToMaxStackSize)
        ).apply(i, StackSizeModuleItem.Data::new));
        public static final DataComponentType<StackSizeModuleItem.Data> TYPE = DataComponentType.<StackSizeModuleItem.Data>builder().persistent(StackSizeModuleItem.Data.CODEC).cacheEncoding().build();

    }

}
