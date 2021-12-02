package de.ellpeck.prettypipes.entities;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.GameRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class PipeFrameEntity extends ItemFrame implements IEntityAdditionalSpawnData {

    private static final EntityDataAccessor<Integer> AMOUNT = SynchedEntityData.defineId(PipeFrameEntity.class, EntityDataSerializers.INT);

    public PipeFrameEntity(EntityType<PipeFrameEntity> type, Level world) {
        super(type, world);
    }

    public PipeFrameEntity(EntityType<PipeFrameEntity> type, Level world, BlockPos pos, Direction dir) {
        this(type, world);
        this.pos = pos;
        this.setDirection(dir);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMOUNT, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide)
            return;
        if (this.tickCount % 40 != 0)
            return;
        PipeNetwork network = PipeNetwork.get(this.level);
        BlockPos attached = getAttachedPipe(this.level, this.pos, this.direction);
        if (attached != null) {
            BlockPos node = network.getNodeFromPipe(attached);
            if (node != null) {
                ItemStack stack = this.getItem();
                if (!stack.isEmpty()) {
                    List<NetworkLocation> items = network.getOrderedNetworkItems(node);
                    int amount = items.stream().mapToInt(i -> i.getItemAmount(this.level, stack)).sum();
                    this.entityData.set(AMOUNT, amount);
                    return;
                }
            }
        }
        this.entityData.set(AMOUNT, -1);
    }

    @Override
    public boolean survives() {
        return super.survives() && canPlace(this.level, this.pos, this.direction);
    }

    private static BlockPos getAttachedPipe(Level world, BlockPos pos, Direction direction) {
        for (int i = 1; i <= 2; i++) {
            BlockPos offset = pos.relative(direction.getOpposite(), i);
            BlockState state = world.getBlockState(offset);
            if (state.getBlock() instanceof PipeBlock)
                return offset;
        }
        return null;
    }

    public static boolean canPlace(Level world, BlockPos pos, Direction direction) {
        return getAttachedPipe(world, pos, direction) != null;
    }

    public int getAmount() {
        return this.entityData.get(AMOUNT);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!source.isExplosion() && !this.getItem().isEmpty()) {
            if (!this.level.isClientSide) {
                this.dropItemOrSelf(source.getDirectEntity(), false);
                this.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.hurt(source, amount);
        }
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        this.playSound(SoundEvents.ITEM_FRAME_BREAK, 1.0F, 1.0F);
        this.dropItemOrSelf(brokenEntity, true);
    }

    private void dropItemOrSelf(@Nullable Entity entityIn, boolean b) {
        if (!this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            if (entityIn == null)
                this.getItem().setEntityRepresentation(null);
        } else {
            ItemStack itemstack = this.getItem();
            this.setItem(ItemStack.EMPTY);
            if (entityIn instanceof Player playerentity) {
                if (playerentity.isCreative()) {
                    itemstack.setEntityRepresentation(null);
                    return;
                }
            }

            if (b)
                this.spawnAtLocation(Registry.pipeFrameItem);

            if (!itemstack.isEmpty()) {
                itemstack = itemstack.copy();
                itemstack.setEntityRepresentation(null);
                this.spawnAtLocation(itemstack);
            }

        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.getItem().isEmpty())
            return super.interact(player, hand);
        return InteractionResult.FAIL;
    }


    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(Registry.pipeFrameItem);
    }

    @Override
    public ICustomPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBlockPos(this.hangingPosition);
        buffer.writeInt(this.facingDirection.getIndex());
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        this.hangingPosition = additionalData.readBlockPos();
        this.updateFacingWithBoundingBox(Direction.values()[additionalData.readInt()]);
    }
}
