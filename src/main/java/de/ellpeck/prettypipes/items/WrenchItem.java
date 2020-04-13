package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.blocks.PipeBlock;
import de.ellpeck.prettypipes.blocks.PipeBlock.ConnectionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;

import java.util.Map;

public class WrenchItem extends Item {
    public WrenchItem() {
        super(new Item.Properties().maxStackSize(1).group(Registry.GROUP));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock))
            return ActionResultType.PASS;

        if (context.getPlayer().isShiftKeyDown()) {
            if (!world.isRemote) {
                Block.spawnDrops(state, world, pos, world.getTileEntity(pos), null, ItemStack.EMPTY);
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.PLAYERS, 1, 1);
                world.removeBlock(pos, false);
            }
            return ActionResultType.SUCCESS;
        }

        // Blocking
        for (Map.Entry<Direction, VoxelShape> entry : PipeBlock.DIR_SHAPES.entrySet()) {
            AxisAlignedBB box = entry.getValue().getBoundingBox().offset(pos).grow(0.001F);
            if (!box.contains(context.getHitVec()))
                continue;
            EnumProperty<ConnectionType> prop = PipeBlock.DIRECTIONS.get(entry.getKey());
            ConnectionType curr = state.get(prop);
            if (curr == ConnectionType.DISCONNECTED)
                continue;

            if (!world.isRemote) {
                ConnectionType newType = curr == ConnectionType.BLOCKED ? ConnectionType.CONNECTED : ConnectionType.BLOCKED;
                BlockState newState = state.with(prop, newType);
                world.setBlockState(pos, newState);

                BlockPos otherPos = pos.offset(entry.getKey());
                world.setBlockState(otherPos, world.getBlockState(otherPos).with(PipeBlock.DIRECTIONS.get(entry.getKey().getOpposite()), newType));
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
}
