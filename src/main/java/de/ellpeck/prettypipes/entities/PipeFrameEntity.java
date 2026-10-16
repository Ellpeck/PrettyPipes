package de.ellpeck.prettypipes.entities;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class PipeFrameEntity extends ItemFrame implements IEntityWithComplexSpawn {

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
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(PipeFrameEntity.AMOUNT, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide())
            return;
        if (this.tickCount % 40 != 0)
            return;
        var network = PipeNetwork.get(this.level());
        var attached = PipeFrameEntity.getAttachedPipe(this.level(), this.pos, this.getDirection());
        if (attached != null) {
            var node = network.getNodeFromPipe(attached);
            if (node != null) {
                var stack = this.getItem();
                if (!stack.isEmpty()) {
                    var items = network.getOrderedNetworkItems(node);
                    var amount = items.stream().mapToInt(i -> i.getItemAmount(this.level(), stack)).sum();
                    this.entityData.set(PipeFrameEntity.AMOUNT, amount);
                    return;
                }
            }
        }
        this.entityData.set(PipeFrameEntity.AMOUNT, -1);
    }

    @Override
    public boolean survives() {
        return super.survives() && PipeFrameEntity.canPlace(this.level(), this.pos, this.getDirection());
    }

    private static BlockPos getAttachedPipe(Level world, BlockPos pos, Direction direction) {
        for (var i = 1; i <= 2; i++) {
            var offset = pos.relative(direction.getOpposite(), i);
            var state = world.getBlockState(offset);
            if (state.getBlock() instanceof PipeBlock)
                return offset;
        }
        return null;
    }

    public static boolean canPlace(Level world, BlockPos pos, Direction direction) {
        return PipeFrameEntity.getAttachedPipe(world, pos, direction) != null;
    }

    public int getAmount() {
        return this.entityData.get(PipeFrameEntity.AMOUNT);
    }

    @Override
    protected ItemStack getFrameItemStack() {
        return new ItemStack(Registry.pipeFrameItem);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.getItem().isEmpty())
            return super.interact(player, hand, location);
        return InteractionResult.FAIL;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.getDirection().ordinal());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.pos = additionalData.readBlockPos();
        this.setDirectionRaw(Direction.values()[additionalData.readInt()]);
    }

}
