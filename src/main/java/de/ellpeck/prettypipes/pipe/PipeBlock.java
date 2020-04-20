package de.ellpeck.prettypipes.pipe;

import com.google.common.collect.ImmutableMap;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PipeBlock extends ContainerBlock implements IPipeConnectable {

    public static final Map<Direction, EnumProperty<ConnectionType>> DIRECTIONS = new HashMap<>();
    private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new HashMap<>();
    private static final VoxelShape CENTER_SHAPE = makeCuboidShape(5, 5, 5, 11, 11, 11);
    public static final Map<Direction, VoxelShape> DIR_SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
            .put(Direction.UP, makeCuboidShape(5, 10, 5, 11, 16, 11))
            .put(Direction.DOWN, makeCuboidShape(5, 0, 5, 11, 6, 11))
            .put(Direction.NORTH, makeCuboidShape(5, 5, 0, 11, 11, 6))
            .put(Direction.SOUTH, makeCuboidShape(5, 5, 10, 11, 11, 16))
            .put(Direction.EAST, makeCuboidShape(10, 5, 5, 16, 11, 11))
            .put(Direction.WEST, makeCuboidShape(0, 5, 5, 6, 11, 11))
            .build();

    static {
        for (Direction dir : Direction.values())
            DIRECTIONS.put(dir, EnumProperty.create(dir.getName(), ConnectionType.class));
    }

    public PipeBlock() {
        super(Block.Properties.create(Material.ROCK).hardnessAndResistance(2).sound(SoundType.STONE).notSolid());

        BlockState state = this.getDefaultState().with(BlockStateProperties.WATERLOGGED, false);
        for (EnumProperty<ConnectionType> prop : DIRECTIONS.values())
            state = state.with(prop, ConnectionType.DISCONNECTED);
        this.setDefaultState(state);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult result) {
        if (!player.getHeldItem(handIn).isEmpty())
            return ActionResultType.PASS;
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
        if (tile == null)
            return ActionResultType.PASS;
        if (!tile.isConnectedInventory())
            return ActionResultType.PASS;
        if (!worldIn.isRemote)
            NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
        return ActionResultType.SUCCESS;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONS.values().toArray(new EnumProperty[0]));
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public IFluidState getFluidState(BlockState state) {
        return state.get(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        BlockState newState = this.createState(worldIn, pos, state);
        if (newState != state) {
            worldIn.setBlockState(pos, newState);
            onStateChanged(worldIn, pos, newState);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.createState(context.getWorld(), context.getPos(), this.getDefaultState());
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.get(BlockStateProperties.WATERLOGGED))
            worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        onStateChanged(worldIn, pos, state);
    }

    @Override
    public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        VoxelShape shape = SHAPE_CACHE.get(state);
        if (shape != null)
            return shape;

        shape = CENTER_SHAPE;
        for (Map.Entry<Direction, EnumProperty<ConnectionType>> entry : DIRECTIONS.entrySet()) {
            if (state.get(entry.getValue()).isConnected())
                shape = VoxelShapes.or(shape, DIR_SHAPES.get(entry.getKey()));
        }
        SHAPE_CACHE.put(state, shape);
        return shape;
    }

    private BlockState createState(World world, BlockPos pos, BlockState curr) {
        BlockState state = this.getDefaultState();
        IFluidState fluid = world.getFluidState(pos);
        if (fluid.isTagged(FluidTags.WATER) && fluid.getLevel() == 8)
            state = state.with(BlockStateProperties.WATERLOGGED, true);

        for (Direction dir : Direction.values()) {
            EnumProperty<ConnectionType> prop = DIRECTIONS.get(dir);
            ConnectionType type = getConnectionType(world, pos, dir, state);
            // don't reconnect on blocked faces
            if (type.isConnected() && curr.get(prop) == ConnectionType.BLOCKED)
                type = ConnectionType.BLOCKED;
            state = state.with(prop, type);
        }
        return state;
    }

    private static ConnectionType getConnectionType(World world, BlockPos pos, Direction direction, BlockState state) {
        BlockPos offset = pos.offset(direction);
        if (!world.isBlockLoaded(offset))
            return ConnectionType.DISCONNECTED;
        BlockState offState = world.getBlockState(offset);
        Block block = offState.getBlock();
        if (block instanceof IPipeConnectable)
            return ((IPipeConnectable) block).getConnectionType(world, offset, offState, pos, direction.getOpposite());
        TileEntity tile = world.getTileEntity(offset);
        if (tile != null) {
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).orElse(null);
            if (handler != null)
                return ConnectionType.CONNECTED;
        }
        if (hasLegsTo(world, offState, offset, direction)) {
            if (DIRECTIONS.values().stream().noneMatch(d -> state.get(d) == ConnectionType.LEGS))
                return ConnectionType.LEGS;
        }
        return ConnectionType.DISCONNECTED;
    }

    private static boolean hasLegsTo(World world, BlockState state, BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof WallBlock || state.getBlock() instanceof FenceBlock)
            return direction == Direction.DOWN;
        if (state.getMaterial() == Material.ROCK || state.getMaterial() == Material.IRON)
            return hasSolidSide(state, world, pos, direction.getOpposite());
        return false;
    }

    public static void onStateChanged(World world, BlockPos pos, BlockState newState) {
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, world, pos);
        if (tile != null && !tile.isConnectedInventory())
            Utility.dropInventory(tile, tile.modules);

        PipeNetwork network = PipeNetwork.get(world);
        int connections = 0;
        boolean force = false;
        for (Direction dir : Direction.values()) {
            ConnectionType value = newState.get(DIRECTIONS.get(dir));
            if (!value.isConnected())
                continue;
            connections++;
            BlockState otherState = world.getBlockState(pos.offset(dir));
            // force a node if we're connecting to a different block (inventory etc.)
            if (otherState.getBlock() != newState.getBlock()) {
                force = true;
                break;
            }
        }
        if (force || connections > 2) {
            network.addNode(pos, newState);
        } else {
            network.removeNode(pos);
        }
        network.onPipeChanged(pos, newState);
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
            if (tile != null) {
                Utility.dropInventory(tile, tile.modules);
                for (PipeItem item : tile.getItems())
                    item.drop(worldIn);
            }
            PipeNetwork network = PipeNetwork.get(worldIn);
            network.removeNode(pos);
            network.onPipeChanged(pos, state);
            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new PipeTileEntity();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ConnectionType getConnectionType(World world, BlockPos pos, BlockState state, BlockPos pipePos, Direction direction) {
        if (state.get(DIRECTIONS.get(direction)) == ConnectionType.BLOCKED)
            return ConnectionType.BLOCKED;
        return ConnectionType.CONNECTED;
    }
}
