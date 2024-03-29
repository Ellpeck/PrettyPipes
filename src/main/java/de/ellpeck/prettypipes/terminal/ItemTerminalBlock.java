package de.ellpeck.prettypipes.terminal;

import com.mojang.serialization.MapCodec;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public class ItemTerminalBlock extends BaseEntityBlock {

    public static final MapCodec<ItemTerminalBlock> CODEC = BlockBehaviour.simpleCodec(ItemTerminalBlock::new);

    public ItemTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return ItemTerminalBlock.CODEC;
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult result) {
        var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, worldIn, pos);
        if (tile == null)
            return InteractionResult.PASS;
        var reason = tile.getInvalidTerminalReason();
        if (reason != null) {
            if (!worldIn.isClientSide)
                player.sendSystemMessage(Component.translatable(reason).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!worldIn.isClientSide) {
            player.openMenu(tile, pos);
            tile.updateItems(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, worldIn, pos);
            if (tile != null)
                Utility.dropInventory(tile, tile.items);
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemTerminalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        Utility.addTooltip(BuiltInRegistries.BLOCK.getKey(this).getPath(), tooltip);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, Registry.itemTerminalBlockEntity, ItemTerminalBlockEntity::tick);
    }

}
