package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingTerminalBlock extends ItemTerminalBlock {

    @Override
    public @org.jetbrains.annotations.Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingTerminalBlockEntity(pos, state);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, Registry.craftingTerminalBlockEntity, ItemTerminalBlockEntity::tick);
    }
}
