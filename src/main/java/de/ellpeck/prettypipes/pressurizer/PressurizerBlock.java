package de.ellpeck.prettypipes.pressurizer;

import com.mojang.serialization.MapCodec;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
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

public class PressurizerBlock extends BaseEntityBlock {

    public static final MapCodec<PressurizerBlock> CODEC = BlockBehaviour.simpleCodec(PressurizerBlock::new);

    public PressurizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return PressurizerBlock.CODEC;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player, BlockHitResult result) {
        var tile = Utility.getBlockEntity(PressurizerBlockEntity.class, worldIn, pos);
        if (tile == null)
            return InteractionResult.PASS;
        if (!worldIn.isClientSide)
            player.openMenu(tile, pos);
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
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        Utility.addTooltip(BuiltInRegistries.BLOCK.getKey(this).getPath(), pTooltipComponents);
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
