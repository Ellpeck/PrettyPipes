package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class PipeFrameItem extends Item {

    public PipeFrameItem() {
        super(new Properties());
    }

    // HangingEntityItem copypasta mostly, since it hardcodes the entities bleh
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var blockpos = context.getClickedPos();
        var direction = context.getClickedFace();
        var blockpos1 = blockpos.relative(direction);
        var playerentity = context.getPlayer();
        var itemstack = context.getItemInHand();
        if (playerentity != null && !this.canPlace(playerentity, direction, itemstack, blockpos1)) {
            return InteractionResult.FAIL;
        } else {
            var world = context.getLevel();
            HangingEntity hangingentity = new PipeFrameEntity(Registry.pipeFrameEntity, world, blockpos1, direction);

            var customdata = itemstack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
            if (!customdata.isEmpty())
                EntityType.updateCustomEntityTag(world, playerentity, hangingentity, customdata);

            if (hangingentity.survives()) {
                if (!world.isClientSide) {
                    hangingentity.playPlacementSound();
                    world.addFreshEntity(hangingentity);
                }

                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.CONSUME;
            }
        }
    }

    protected boolean canPlace(Player playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn) && PipeFrameEntity.canPlace(playerIn.level(), posIn, directionIn);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        Utility.addTooltip(BuiltInRegistries.ITEM.getKey(this).getPath(), pTooltipComponents);
    }

}
