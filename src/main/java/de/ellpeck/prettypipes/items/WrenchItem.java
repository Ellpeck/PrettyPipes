package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.state.EnumProperty;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class WrenchItem extends Item {

    public WrenchItem() {
        super(new Item.Properties().maxStackSize(1).group(Registry.GROUP));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        PlayerEntity player = context.getPlayer();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock))
            return ActionResultType.PASS;
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, world, pos);
        if (tile == null)
            return ActionResultType.FAIL;

        if (player.isSneaking()) {
            if (!world.isRemote) {
                if (tile.cover != null) {
                    // remove the cover
                    tile.removeCover(player, context.getHand());
                    Utility.sendTileEntityToClients(tile);
                } else {
                    // remove the pipe
                    PipeBlock.dropItems(world, pos, player);
                    Block.spawnDrops(state, world, pos, tile, null, ItemStack.EMPTY);
                    world.removeBlock(pos, false);
                }
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.PLAYERS, 1, 1);
            }
            return ActionResultType.func_233537_a_(world.isRemote);
        }

        // placing covers
        if (tile.cover == null) {
            ItemStack offhand = player.getHeldItemOffhand();
            if (offhand.getItem() instanceof BlockItem) {
                if (!world.isRemote) {
                    BlockItemUseContext blockContext = new BlockItemUseContext(context);
                    Block block = ((BlockItem) offhand.getItem()).getBlock();
                    BlockState cover = block.getStateForPlacement(blockContext);
                    if (cover != null && !block.hasTileEntity(cover)) {
                        tile.cover = cover;
                        Utility.sendTileEntityToClients(tile);
                        offhand.shrink(1);
                        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.PLAYERS, 1, 1);
                    }
                }
                return ActionResultType.func_233537_a_(world.isRemote);
            }
        }

        // disabling directions
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
                BlockPos otherPos = pos.offset(entry.getKey());
                BlockState otherState = world.getBlockState(otherPos);
                if (otherState.getBlock() instanceof PipeBlock) {
                    otherState = otherState.with(PipeBlock.DIRECTIONS.get(entry.getKey().getOpposite()), newType);
                    world.setBlockState(otherPos, otherState);
                    PipeBlock.onStateChanged(world, otherPos, otherState);
                }
                BlockState newState = state.with(prop, newType);
                world.setBlockState(pos, newState);
                PipeBlock.onStateChanged(world, pos, newState);
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.PLAYERS, 1, 1);
            }
            return ActionResultType.func_233537_a_(world.isRemote);
        }
        return ActionResultType.PASS;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        Utility.addTooltip(this.getRegistryName().getPath(), tooltip);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.SILK_TOUCH;
    }
}
