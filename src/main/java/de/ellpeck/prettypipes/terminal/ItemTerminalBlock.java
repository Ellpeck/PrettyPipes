package de.ellpeck.prettypipes.terminal;

import com.mojang.serialization.MapCodec;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, pLevel, pPos);
        if (tile == null)
            return InteractionResult.PASS;
        var reason = tile.getInvalidTerminalReason();
        if (reason != null) {
            if (!pLevel.isClientSide)
                pPlayer.sendSystemMessage(Component.translatable(reason).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!pLevel.isClientSide) {
            pPlayer.openMenu(tile, pPos);
            tile.updateItems(pPlayer);
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
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        Utility.addTooltip(BuiltInRegistries.BLOCK.getKey(this).getPath(), pTooltipComponents);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, Registry.itemTerminalBlockEntity, ItemTerminalBlockEntity::tick);
    }

}
