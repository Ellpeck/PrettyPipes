package de.ellpeck.prettypipes.pipe;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<PipeBlock> CODEC = BlockBehaviour.simpleCodec(PipeBlock::new);

    public static final Map<Direction, EnumProperty<ConnectionType>> DIRECTIONS = new HashMap<>();
    private static final Map<Pair<BlockState, BlockState>, VoxelShape> SHAPE_CACHE = new HashMap<>();
    private static final Map<Pair<BlockState, BlockState>, VoxelShape> COLL_SHAPE_CACHE = new HashMap<>();
    private static final VoxelShape CENTER_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    public static final Map<Direction, VoxelShape> DIR_SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
        .put(Direction.UP, Block.box(5, 10, 5, 11, 16, 11))
        .put(Direction.DOWN, Block.box(5, 0, 5, 11, 6, 11))
        .put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 6))
        .put(Direction.SOUTH, Block.box(5, 5, 10, 11, 11, 16))
        .put(Direction.EAST, Block.box(10, 5, 5, 16, 11, 11))
        .put(Direction.WEST, Block.box(0, 5, 5, 6, 11, 11))
        .build();

    static {
        for (var dir : Direction.values())
            PipeBlock.DIRECTIONS.put(dir, EnumProperty.create(dir.getName(), ConnectionType.class));
    }

    public PipeBlock(Block.Properties properties) {
        super(properties);

        var state = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        for (var prop : PipeBlock.DIRECTIONS.values())
            state = state.setValue(prop, ConnectionType.DISCONNECTED);
        this.registerDefaultState(state);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack pStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult result) {
        var tile = Utility.getBlockEntity(PipeBlockEntity.class, worldIn, pos);
        if (tile == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!tile.canHaveModules())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var stack = player.getItemInHand(handIn);
        if (stack.getItem() instanceof IModule) {
            var copy = stack.copy();
            copy.setCount(1);
            var remain = ItemHandlerHelper.insertItem(tile.modules, copy, false);
            if (remain.isEmpty()) {
                stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (handIn == InteractionHand.MAIN_HAND && stack.isEmpty()) {
            if (!worldIn.isClientSide)
                player.openMenu(tile, pos);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.DIRECTIONS.values().toArray(new EnumProperty[0]));
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        var newState = this.createState(worldIn, pos, state);
        if (newState != state) {
            worldIn.setBlockAndUpdate(pos, newState);
            PipeBlock.onStateChanged(worldIn, pos, newState);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.createState(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.getValue(BlockStateProperties.WATERLOGGED))
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        PipeBlock.onStateChanged(worldIn, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return this.cacheAndGetShape(state, worldIn, pos, s -> s.getShape(worldIn, pos, context), PipeBlock.SHAPE_CACHE, null);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return this.cacheAndGetShape(state, worldIn, pos, s -> s.getCollisionShape(worldIn, pos, context), PipeBlock.COLL_SHAPE_CACHE, s -> {
            // make the shape a bit higher so we can jump up onto a higher block
            var newShape = new MutableObject<VoxelShape>(Shapes.empty());
            s.forAllBoxes((x1, y1, z1, x2, y2, z2) -> newShape.setValue(Shapes.join(Shapes.create(x1, y1, z1, x2, y2 + 3 / 16F, z2), newShape.getValue(), BooleanOp.OR)));
            return newShape.getValue().optimize();
        });
    }

    private VoxelShape cacheAndGetShape(BlockState state, BlockGetter worldIn, BlockPos pos, Function<BlockState, VoxelShape> coverShapeSelector, Map<Pair<BlockState, BlockState>, VoxelShape> cache, Function<VoxelShape, VoxelShape> shapeModifier) {
        VoxelShape coverShape = null;
        BlockState cover = null;
        var tile = Utility.getBlockEntity(PipeBlockEntity.class, worldIn, pos);
        if (tile != null && tile.cover != null) {
            cover = tile.cover;
            // try catch since the block might expect to find itself at the position
            try {
                coverShape = coverShapeSelector.apply(cover);
            } catch (Exception ignored) {
            }
        }
        var key = Pair.of(state, cover);
        var shape = cache.get(key);
        if (shape == null) {
            shape = PipeBlock.CENTER_SHAPE;
            for (var entry : PipeBlock.DIRECTIONS.entrySet()) {
                if (state.getValue(entry.getValue()).isConnected())
                    shape = Shapes.or(shape, PipeBlock.DIR_SHAPES.get(entry.getKey()));
            }
            if (shapeModifier != null)
                shape = shapeModifier.apply(shape);
            if (coverShape != null)
                shape = Shapes.or(shape, coverShape);
            cache.put(key, shape);
        }
        return shape;
    }

    private BlockState createState(Level world, BlockPos pos, BlockState curr) {
        var state = this.defaultBlockState();
        var fluid = world.getFluidState(pos);
        if (fluid.is(FluidTags.WATER) && fluid.getAmount() == 8)
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);

        for (var dir : Direction.values()) {
            var prop = PipeBlock.DIRECTIONS.get(dir);
            var type = this.getConnectionType(world, pos, dir, state);
            // don't reconnect on blocked faces
            if (type.isConnected() && curr.getValue(prop) == ConnectionType.BLOCKED)
                type = ConnectionType.BLOCKED;
            state = state.setValue(prop, type);
        }
        return state;
    }

    protected ConnectionType getConnectionType(Level world, BlockPos pos, Direction direction, BlockState state) {
        var offset = pos.relative(direction);
        if (!world.isLoaded(offset))
            return ConnectionType.DISCONNECTED;
        var opposite = direction.getOpposite();
        var tile = world.getBlockEntity(offset);
        if (tile != null) {
            var connectable = world.getCapability(Registry.pipeConnectableCapability, offset, tile.getBlockState(), tile, opposite);
            if (connectable != null)
                return connectable.getConnectionType(pos, direction);
            var handler = world.getCapability(Capabilities.ItemHandler.BLOCK, offset, tile.getBlockState(), tile, opposite);
            if (handler != null)
                return ConnectionType.CONNECTED;
        }
        var blockHandler = Utility.getBlockItemHandler(world, offset, opposite);
        if (blockHandler != null)
            return ConnectionType.CONNECTED;
        var offState = world.getBlockState(offset);
        if (PipeBlock.hasLegsTo(world, offState, offset, direction)) {
            if (PipeBlock.DIRECTIONS.values().stream().noneMatch(d -> state.getValue(d) == ConnectionType.LEGS))
                return ConnectionType.LEGS;
        }
        return ConnectionType.DISCONNECTED;
    }

    protected static boolean hasLegsTo(Level world, BlockState state, BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof WallBlock || state.getBlock() instanceof FenceBlock)
            return direction == Direction.DOWN;
        var mapColor = state.getMapColor(world, pos);
        if (mapColor == MapColor.STONE || mapColor == MapColor.METAL)
            return Block.canSupportCenter(world, pos, direction.getOpposite());
        return false;
    }

    public static void onStateChanged(Level world, BlockPos pos, BlockState newState) {
        // wait a few ticks before checking if we have to drop our modules, so that things like iron -> gold chest work
        var tile = Utility.getBlockEntity(PipeBlockEntity.class, world, pos);
        if (tile != null)
            tile.moduleDropCheck = 5;

        var network = PipeNetwork.get(world);
        var connections = 0;
        var force = false;
        for (var dir : Direction.values()) {
            var value = newState.getValue(PipeBlock.DIRECTIONS.get(dir));
            if (!value.isConnected())
                continue;
            connections++;
            var otherState = world.getBlockState(pos.relative(dir));
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
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            var network = PipeNetwork.get(worldIn);
            network.removeNode(pos);
            network.onPipeChanged(pos, state);
            if (worldIn.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
                pipe.getItems().clear();
                for (var lock : pipe.craftIngredientRequests)
                    network.resolveNetworkLock(lock.getRight());
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        PipeBlock.dropItems(worldIn, pos, player);
        return super.playerWillDestroy(worldIn, pos, state, player);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        var pipe = Utility.getBlockEntity(PipeBlockEntity.class, world, pos);
        if (pipe == null)
            return 0;
        return Math.min(15, pipe.getItems().size());
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return PipeBlock.CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, Registry.pipeBlockEntity, PipeBlockEntity::tick);
    }

    public static void dropItems(Level worldIn, BlockPos pos, Player player) {
        var tile = Utility.getBlockEntity(PipeBlockEntity.class, worldIn, pos);
        if (tile != null) {
            Utility.dropInventory(tile, tile.modules);
            for (var item : tile.getItems())
                item.drop(worldIn, item.getContent());
            if (tile.cover != null)
                tile.removeCover(player, InteractionHand.MAIN_HAND);
        }
    }

}
