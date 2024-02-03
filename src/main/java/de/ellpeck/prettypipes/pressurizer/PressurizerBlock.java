package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class PressurizerBlock extends BaseEntityBlock {

    public PressurizerBlock() {
        super(BlockBehaviour.Properties.of().strength(3).sound(SoundType.STONE));
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult result) {
        var tile = Utility.getBlockEntity(PressurizerBlockEntity.class, worldIn, pos);
        if (tile == null)
            return InteractionResult.PASS;
        if (!worldIn.isClientSide)
            NetworkHooks.openScreen((ServerPlayer) player, tile, pos);
        return InteractionResult.SUCCESS;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressurizerBlockEntity(pos, state);
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
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper(type, Registry.pressurizerBlockEntity, PressurizerBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        var pipe = Utility.getBlockEntity(PressurizerBlockEntity.class, world, pos);
        if (pipe == null)
            return 0;
        return (int) (pipe.getEnergy() / (float) pipe.getMaxEnergy() * 15);
    }
}
