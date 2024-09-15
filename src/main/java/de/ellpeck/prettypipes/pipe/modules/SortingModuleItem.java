package de.ellpeck.prettypipes.pipe.modules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SortingModuleItem extends ModuleItem {

    private final Type type;

    public SortingModuleItem(String name, Type type) {
        super(name, new Properties());
        this.type = type;
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof SortingModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return false;
    }

    @Override
    public Integer getCustomNextNode(ItemStack module, PipeBlockEntity tile, List<BlockPos> nodes, int index) {
        switch (this.type) {
            case ROUND_ROBIN:
                // store an ever-increasing index and choose destinations based on that
                var prevData = module.get(Data.TYPE);
                var next = prevData != null ? prevData.last + 1 : 0;
                module.set(Data.TYPE, new Data(next));
                return next % nodes.size();
            case RANDOM:
                return tile.getLevel().random.nextInt(nodes.size());
        }
        return null;
    }

    public enum Type {
        ROUND_ROBIN,
        RANDOM
    }

    public record Data(int last) {

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("last").forGetter(f -> f.last)).apply(i, Data::new));
        public static final DataComponentType<Data> TYPE = DataComponentType.<Data>builder().persistent(Data.CODEC).cacheEncoding().build();

    }

}
