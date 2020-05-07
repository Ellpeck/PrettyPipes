package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class ItemTerminalBlock extends ContainerBlock implements IPipeConnectable {
    public ItemTerminalBlock() {
        super(Properties.create(Material.ROCK).hardnessAndResistance(3).sound(SoundType.STONE));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult result) {
        if (!player.getHeldItem(handIn).isEmpty())
            return ActionResultType.PASS;
        ItemTerminalTileEntity tile = Utility.getTileEntity(ItemTerminalTileEntity.class, worldIn, pos);
        if (tile == null)
            return ActionResultType.PASS;
        if (!worldIn.isRemote) {
            NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
            tile.updateItems(player);
        }
        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new ItemTerminalTileEntity();
    }

    @Override
    public ConnectionType getConnectionType(World world, BlockPos pos, BlockState state, BlockPos pipePos, Direction direction) {
        return ConnectionType.CONNECTED;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
