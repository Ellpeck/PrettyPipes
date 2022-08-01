package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class ItemTerminalBlock extends BaseEntityBlock {

    public ItemTerminalBlock() {
        super(Properties.of(Material.STONE).strength(3).sound(SoundType.STONE));
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
            NetworkHooks.openScreen((ServerPlayer) player, tile, pos);
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
        Utility.addTooltip(ForgeRegistries.BLOCKS.getKey(this).getPath(), tooltip);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, Registry.itemTerminalBlockEntity, ItemTerminalBlockEntity::tick);
    }

}
