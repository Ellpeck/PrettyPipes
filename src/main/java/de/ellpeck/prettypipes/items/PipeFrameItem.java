package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PipeFrameItem extends Item {
    public PipeFrameItem() {
        super(new Properties().group(Registry.GROUP));
    }

    // HangingEntityItem copypasta mostly, since it hardcodes the entities bleh
    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        BlockPos blockpos = context.getPos();
        Direction direction = context.getFace();
        BlockPos blockpos1 = blockpos.offset(direction);
        PlayerEntity playerentity = context.getPlayer();
        ItemStack itemstack = context.getItem();
        if (playerentity != null && !this.canPlace(playerentity, direction, itemstack, blockpos1)) {
            return ActionResultType.FAIL;
        } else {
            World world = context.getWorld();
            HangingEntity hangingentity = new PipeFrameEntity(Registry.pipeFrameEntity, world, blockpos1, direction);

            CompoundNBT compoundnbt = itemstack.getTag();
            if (compoundnbt != null) {
                EntityType.applyItemNBT(world, playerentity, hangingentity, compoundnbt);
            }

            if (hangingentity.onValidSurface()) {
                if (!world.isRemote) {
                    hangingentity.playPlaceSound();
                    world.addEntity(hangingentity);
                }

                itemstack.shrink(1);
                return ActionResultType.SUCCESS;
            } else {
                return ActionResultType.CONSUME;
            }
        }
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        BlockState state = playerIn.world.getBlockState(posIn.offset(directionIn.getOpposite()));
        if (!(state.getBlock() instanceof PipeBlock))
            return false;
        return !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }
}
