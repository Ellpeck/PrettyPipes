package de.ellpeck.prettypipes.entities;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class PipeFrameEntity extends ItemFrameEntity {

    private static final DataParameter<Integer> AMOUNT = EntityDataManager.createKey(PipeFrameEntity.class, DataSerializers.VARINT);

    public PipeFrameEntity(EntityType<PipeFrameEntity> type, World world) {
        super(type, world);
    }

    public PipeFrameEntity(EntityType<PipeFrameEntity> type, World world, BlockPos pos, Direction dir) {
        this(type, world);
        this.hangingPosition = pos;
        this.updateFacingWithBoundingBox(dir);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(AMOUNT, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote)
            return;
        if (this.ticksExisted % 40 != 0)
            return;
        PipeNetwork network = PipeNetwork.get(this.world);
        BlockPos attached = getAttachedPipe(this.world, this.hangingPosition, this.facingDirection);
        if (attached != null) {
            BlockPos node = network.getNodeFromPipe(attached);
            if (node != null) {
                ItemStack stack = this.getDisplayedItem();
                if (!stack.isEmpty()) {
                    List<NetworkLocation> items = network.getOrderedNetworkItems(node);
                    int amount = items.stream().mapToInt(i -> i.getItemAmount(stack)).sum();
                    this.dataManager.set(AMOUNT, amount);
                    return;
                }
            }
        }
        this.dataManager.set(AMOUNT, -1);
    }

    @Override
    public boolean onValidSurface() {
        return super.onValidSurface() && canPlace(this.world, this.hangingPosition, this.facingDirection);
    }

    private static BlockPos getAttachedPipe(World world, BlockPos pos, Direction direction) {
        for (int i = 1; i <= 2; i++) {
            BlockPos offset = pos.offset(direction.getOpposite(), i);
            BlockState state = world.getBlockState(offset);
            if (state.getBlock() instanceof PipeBlock)
                return offset;
        }
        return null;
    }

    public static boolean canPlace(World world, BlockPos pos, Direction direction) {
        return getAttachedPipe(world, pos, direction) != null;
    }

    public int getAmount() {
        return this.dataManager.get(AMOUNT);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!source.isExplosion() && !this.getDisplayedItem().isEmpty()) {
            if (!this.world.isRemote) {
                this.dropItemOrSelf(source.getTrueSource(), false);
                this.playSound(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.attackEntityFrom(source, amount);
        }
    }

    @Override
    public void onBroken(@Nullable Entity brokenEntity) {
        this.playSound(SoundEvents.ENTITY_ITEM_FRAME_BREAK, 1.0F, 1.0F);
        this.dropItemOrSelf(brokenEntity, true);
    }

    private void dropItemOrSelf(@Nullable Entity entityIn, boolean b) {
        if (!this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            if (entityIn == null)
                this.getDisplayedItem().setItemFrame(null);
        } else {
            ItemStack itemstack = this.getDisplayedItem();
            this.setDisplayedItem(ItemStack.EMPTY);
            if (entityIn instanceof PlayerEntity) {
                PlayerEntity playerentity = (PlayerEntity) entityIn;
                if (playerentity.abilities.isCreativeMode) {
                    itemstack.setItemFrame(null);
                    return;
                }
            }

            if (b)
                this.entityDropItem(Registry.pipeFrameItem);

            if (!itemstack.isEmpty()) {
                itemstack = itemstack.copy();
                itemstack.setItemFrame(null);
                this.entityDropItem(itemstack);
            }

        }
    }

    @Override
    public boolean processInitialInteract(PlayerEntity player, Hand hand) {
        if (this.getDisplayedItem().isEmpty())
            return super.processInitialInteract(player, hand);
        return false;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
