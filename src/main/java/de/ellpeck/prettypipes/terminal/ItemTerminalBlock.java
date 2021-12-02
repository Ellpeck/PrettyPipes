package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.Utility;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemTerminalBlock extends ContainerBlock {

    public ItemTerminalBlock() {
        super(Properties.create(Material.ROCK).hardnessAndResistance(3).sound(SoundType.STONE));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult result) {
        ItemTerminalBlockEntity tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, worldIn, pos);
        if (tile == null)
            return ActionResultType.PASS;
        String reason = tile.getInvalidTerminalReason();
        if (reason != null) {
            if (!worldIn.isRemote)
                player.sendMessage(new TranslationTextComponent(reason).mergeStyle(TextFormatting.RED), UUID.randomUUID());
            return ActionResultType.SUCCESS;
        }
        if (!worldIn.isRemote) {
            NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
            tile.updateItems(player);
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            ItemTerminalBlockEntity tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, worldIn, pos);
            if (tile != null)
                Utility.dropInventory(tile, tile.items);
            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new ItemTerminalBlockEntity();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        Utility.addTooltip(this.getRegistryName().getPath(), tooltip);
    }
}
