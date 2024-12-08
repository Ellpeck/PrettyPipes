package de.ellpeck.prettypipes.pipe.modules.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FilterModifierModuleItem extends ModuleItem {

    public final ItemEquality.Type type;

    public FilterModifierModuleItem(String name, ItemEquality.Type type) {
        super(name, new Properties());
        this.type = type;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return other != this;
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return this.type == ItemEquality.Type.TAG;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex) {
        return new FilterModifierModuleContainer(Registry.filterModifierModuleContainer, windowId, player, tile.getBlockPos(), moduleIndex);
    }

    public ItemEquality getEqualityType(ItemStack stack) {
        if (this.type == ItemEquality.Type.TAG) {
            return ItemEquality.tag(FilterModifierModuleItem.getFilterTag(stack));
        } else {
            return this.type.getDefaultInstance();
        }
    }

    public static ResourceLocation getFilterTag(ItemStack stack) {
        var data = stack.get(Data.TYPE);
        return data != null && data.filterTag != null ? ResourceLocation.parse(data.filterTag) : null;
    }

    public static void setFilterTag(ItemStack stack, ResourceLocation tag) {
        stack.set(Data.TYPE, new Data(tag.toString()));
    }

    public record Data(String filterTag) {

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("filter_tag").forGetter(f -> f.filterTag)
        ).apply(i, Data::new));
        public static final DataComponentType<Data> TYPE = DataComponentType.<Data>builder().persistent(Data.CODEC).cacheEncoding().build();

    }

}
