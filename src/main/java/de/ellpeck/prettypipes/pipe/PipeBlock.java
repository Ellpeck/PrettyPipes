package de.ellpeck.prettypipes.pipe;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.types.Func;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
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
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PipeBlock extends ContainerBlock {

    public static final Map<Direction, EnumProperty<ConnectionType>> DIRECTIONS = new HashMap<>();
    private static final Map<Pair<BlockState, BlockState>, VoxelShape> SHAPE_CACHE = new HashMap<>();
    private static final Map<Pair<BlockState, BlockState>, VoxelShape> COLL_SHAPE_CACHE = new HashMap<>();
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
            DIRECTIONS.put(dir, EnumProperty.create(dir.getName2(), ConnectionType.class));
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
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
        if (tile == null)
            return ActionResultType.PASS;
        if (!tile.canHaveModules())
            return ActionResultType.PASS;
        ItemStack stack = player.getHeldItem(handIn);
        if (stack.getItem() instanceof IModule) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            ItemStack remain = ItemHandlerHelper.insertItem(tile.modules, copy, false);
            if (remain.isEmpty()) {
                stack.shrink(1);
                return ActionResultType.SUCCESS;
            }
        } else if (handIn == Hand.MAIN_HAND && stack.isEmpty()) {
            if (!worldIn.isRemote)
                NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONS.values().toArray(new EnumProperty[0]));
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
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
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return this.cacheAndGetShape(state, worldIn, pos, s -> s.getShape(worldIn, pos, context), SHAPE_CACHE, null);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return this.cacheAndGetShape(state, worldIn, pos, s -> s.getCollisionShape(worldIn, pos, context), COLL_SHAPE_CACHE, s -> {
            // make the shape a bit higher so we can jump up onto a higher block
            MutableObject<VoxelShape> newShape = new MutableObject<>(VoxelShapes.empty());
            s.forEachBox((x1, y1, z1, x2, y2, z2) -> newShape.setValue(VoxelShapes.combine(VoxelShapes.create(x1, y1, z1, x2, y2 + 3 / 16F, z2), newShape.getValue(), IBooleanFunction.OR)));
            return newShape.getValue().simplify();
        });
    }

    private VoxelShape cacheAndGetShape(BlockState state, IBlockReader worldIn, BlockPos pos, Function<BlockState, VoxelShape> coverShapeSelector, Map<Pair<BlockState, BlockState>, VoxelShape> cache, Function<VoxelShape, VoxelShape> shapeModifier) {
        VoxelShape coverShape = null;
        BlockState cover = null;
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
        if (tile != null && tile.cover != null) {
            cover = tile.cover;
            // try catch since the block might expect to find itself at the position
            try {
                coverShape = coverShapeSelector.apply(cover);
            } catch (Exception ignored) {
            }
        }
        Pair<BlockState, BlockState> key = Pair.of(state, cover);
        VoxelShape shape = cache.get(key);
        if (shape == null) {
            shape = CENTER_SHAPE;
            for (Map.Entry<Direction, EnumProperty<ConnectionType>> entry : DIRECTIONS.entrySet()) {
                if (state.get(entry.getValue()).isConnected())
                    shape = VoxelShapes.or(shape, DIR_SHAPES.get(entry.getKey()));
            }
            if (shapeModifier != null)
                shape = shapeModifier.apply(shape);
            if (coverShape != null)
                shape = VoxelShapes.or(shape, coverShape);
            cache.put(key, shape);
        }
        return shape;
    }

    private BlockState createState(World world, BlockPos pos, BlockState curr) {
        BlockState state = this.getDefaultState();
        FluidState fluid = world.getFluidState(pos);
        if (fluid.isTagged(FluidTags.WATER) && fluid.getLevel() == 8)
            state = state.with(BlockStateProperties.WATERLOGGED, true);

        for (Direction dir : Direction.values()) {
            EnumProperty<ConnectionType> prop = DIRECTIONS.get(dir);
            ConnectionType type = this.getConnectionType(world, pos, dir, state);
            // don't reconnect on blocked faces
            if (type.isConnected() && curr.get(prop) == ConnectionType.BLOCKED)
                type = ConnectionType.BLOCKED;
            state = state.with(prop, type);
        }
        return state;
    }

    protected ConnectionType getConnectionType(World world, BlockPos pos, Direction direction, BlockState state) {
        BlockPos offset = pos.offset(direction);
        if (!world.isBlockLoaded(offset))
            return ConnectionType.DISCONNECTED;
        Direction opposite = direction.getOpposite();
        TileEntity tile = world.getTileEntity(offset);
        if (tile != null) {
            IPipeConnectable connectable = tile.getCapability(Registry.pipeConnectableCapability, opposite).orElse(null);
            if (connectable != null)
                return connectable.getConnectionType(pos, direction);
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, opposite).orElse(null);
            if (handler != null)
                return ConnectionType.CONNECTED;
        }
        IItemHandler blockHandler = Utility.getBlockItemHandler(world, offset, opposite);
        if (blockHandler != null)
            return ConnectionType.CONNECTED;
        BlockState offState = world.getBlockState(offset);
        if (hasLegsTo(world, offState, offset, direction)) {
            if (DIRECTIONS.values().stream().noneMatch(d -> state.get(d) == ConnectionType.LEGS))
                return ConnectionType.LEGS;
        }
        return ConnectionType.DISCONNECTED;
    }

    protected static boolean hasLegsTo(World world, BlockState state, BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof WallBlock || state.getBlock() instanceof FenceBlock)
            return direction == Direction.DOWN;
        if (state.getMaterial() == Material.ROCK || state.getMaterial() == Material.IRON)
            return hasEnoughSolidSide(world, pos, direction.getOpposite());
        return false;
    }

    public static void onStateChanged(World world, BlockPos pos, BlockState newState) {
        // wait a few ticks before checking if we have to drop our modules, so that things like iron -> gold chest work
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, world, pos);
        if (tile != null)
            tile.moduleDropCheck = 5;

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
            PipeNetwork network = PipeNetwork.get(worldIn);
            network.removeNode(pos);
            network.onPipeChanged(pos, state);
            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
        if (tile != null) {
            Utility.dropInventory(tile, tile.modules);
            for (IPipeItem item : tile.getItems())
                item.drop(worldIn, item.getContent());
            if (tile.cover != null)
                tile.removeCover(player, Hand.MAIN_HAND);
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
        PipeTileEntity pipe = Utility.getTileEntity(PipeTileEntity.class, worldIn, pos);
        if (pipe == null)
            return 0;
        return Math.min(15, pipe.getItems().size());
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
}
