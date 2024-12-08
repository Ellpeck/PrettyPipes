package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.ConnectionType;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;

import java.util.List;

public class WrenchItem extends Item {

    public WrenchItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock))
            return InteractionResult.PASS;
        var tile = Utility.getBlockEntity(PipeBlockEntity.class, world, pos);
        if (tile == null)
            return InteractionResult.FAIL;

        if (player.isCrouching()) {
            if (!world.isClientSide) {
                if (tile.cover != null) {
                    // remove the cover
                    tile.removeCover();
                    Utility.sendBlockEntityToClients(tile);
                } else {
                    // remove the pipe
                    Block.dropResources(state, world, pos, tile, null, ItemStack.EMPTY);
                    world.removeBlock(pos, false);
                }
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 1, 1);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        // placing covers
        if (tile.cover == null) {
            var offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof BlockItem) {
                if (!world.isClientSide) {
                    var blockContext = new BlockPlaceContext(context);
                    var block = ((BlockItem) offhand.getItem()).getBlock();
                    var cover = block.getStateForPlacement(blockContext);
                    if (cover != null && !(block instanceof EntityBlock)) {
                        tile.cover = cover;
                        Utility.sendBlockEntityToClients(tile);
                        offhand.shrink(1);
                        world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1, 1);
                    }
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        // disabling directions
        for (var entry : PipeBlock.DIR_SHAPES.entrySet()) {
            var box = entry.getValue().bounds().move(pos).inflate(0.001F);
            if (!box.contains(context.getClickLocation()))
                continue;
            var prop = PipeBlock.DIRECTIONS.get(entry.getKey());
            var curr = state.getValue(prop);
            if (curr == ConnectionType.DISCONNECTED)
                continue;

            if (!world.isClientSide) {
                var newType = curr == ConnectionType.BLOCKED ? ConnectionType.CONNECTED : ConnectionType.BLOCKED;
                var otherPos = pos.relative(entry.getKey());
                var otherState = world.getBlockState(otherPos);
                if (otherState.getBlock() instanceof PipeBlock) {
                    otherState = otherState.setValue(PipeBlock.DIRECTIONS.get(entry.getKey().getOpposite()), newType);
                    world.setBlockAndUpdate(otherPos, otherState);
                    PipeBlock.onStateChanged(world, otherPos, otherState);
                }
                var newState = state.setValue(prop, newType);
                world.setBlockAndUpdate(pos, newState);
                PipeBlock.onStateChanged(world, pos, newState);
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.PLAYERS, 1, 1);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        Utility.addTooltip(BuiltInRegistries.ITEM.getKey(this).getPath(), pTooltipComponents);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment == Enchantments.SILK_TOUCH;
    }

}
