package de.ellpeck.prettypipes.pipe.modules.modifier;

import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterModifierModuleContainer extends AbstractPipeContainer<FilterModifierModuleItem> {

    public FilterModifierModuleContainer(@Nullable ContainerType<?> type, int id, PlayerEntity player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    public List<ResourceLocation> getTags() {
        Set<ResourceLocation> unsortedTags = new HashSet<>();
        for (ItemFilter filter : this.tile.getFilters()) {
            for (int i = 0; i < filter.getSlots(); i++) {
                ItemStack stack = filter.getStackInSlot(i);
                unsortedTags.addAll(stack.getItem().getTags());
            }
        }
        return unsortedTags.stream().sorted().collect(Collectors.toList());
    }

    @Override
    protected void addSlots() {

    }
}
