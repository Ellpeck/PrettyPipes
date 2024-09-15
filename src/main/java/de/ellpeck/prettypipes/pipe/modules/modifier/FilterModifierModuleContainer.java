package de.ellpeck.prettypipes.pipe.modules.modifier;

import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterModifierModuleContainer extends AbstractPipeContainer<FilterModifierModuleItem> {

    public FilterModifierModuleContainer(@Nullable MenuType<?> type, int id, Player player, BlockPos pos, int moduleIndex) {
        super(type, id, player, pos, moduleIndex);
    }

    public List<ResourceLocation> getTags() {
        Set<ResourceLocation> unsortedTags = new HashSet<>();
        for (var filter : this.tile.getFilters(null)) {
            for (var i = 0; i < filter.content.getSlots(); i++) {
                var stack = filter.content.getStackInSlot(i);
                stack.getTags().forEach(t -> unsortedTags.add(t.location()));
            }
        }
        return unsortedTags.stream().sorted().collect(Collectors.toList());
    }

    @Override
    protected void addSlots() {

    }

}
