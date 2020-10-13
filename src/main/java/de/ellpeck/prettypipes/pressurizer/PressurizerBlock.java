package de.ellpeck.prettypipes.pressurizer;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class PressurizerBlock extends ContainerBlock implements IPipeConnectable {
    public PressurizerBlock() {
        super(Properties.create(Material.ROCK).hardnessAndResistance(3).sound(SoundType.STONE));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult result) {
        PressurizerTileEntity tile = Utility.getTileEntity(PressurizerTileEntity.class, worldIn, pos);
        if (tile == null)
            return ActionResultType.PASS;
        if (!worldIn.isRemote)
            NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
        return ActionResultType.SUCCESS;
    }

    @Override
    public ConnectionType getConnectionType(World world, BlockPos pipePos, Direction direction) {
        return ConnectionType.CONNECTED;
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new PressurizerTileEntity();
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
