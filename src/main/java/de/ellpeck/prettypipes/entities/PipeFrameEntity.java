package de.ellpeck.prettypipes.entities;

import de.ellpeck.prettypipes.network.NetworkLocation;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

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
        this.dataManager.register(AMOUNT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote)
            return;
        if (this.ticksExisted % 40 != 0)
            return;
        PipeNetwork network = PipeNetwork.get(this.world);
        BlockPos hangingPos = this.getHangingPosition().offset(this.getHorizontalFacing().getOpposite());
        BlockPos node = network.getNodeFromPipe(hangingPos);
        if (node == null)
            return;
        ItemStack stack = this.getDisplayedItem();
        List<NetworkLocation> items = network.getOrderedNetworkItems(node);
        int amount = items.stream().mapToInt(i -> i.getItemAmount(stack)).sum();
        this.dataManager.set(AMOUNT, amount);
    }

    public int getAmount() {
        return this.dataManager.get(AMOUNT);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
