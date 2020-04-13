package de.ellpeck.prettypipes.blocks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PipeBlock extends Block {

    public static final Map<Direction, EnumProperty<ConnectionType>> DIRECTIONS = new HashMap<>();
    private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new HashMap<>();
    private static final VoxelShape CENTER_SHAPE = makeCuboidShape(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> DIR_SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
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

        BlockState state = this.getDefaultState();
        for (EnumProperty<ConnectionType> prop : DIRECTIONS.values())
            state = state.with(prop, ConnectionType.DISCONNECTED);
        this.setDefaultState(state);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(DIRECTIONS.values().toArray(new EnumProperty[0]));
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        BlockState newState = this.createState(worldIn, pos);
        if (newState != state)
            worldIn.setBlockState(pos, newState);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.createState(context.getWorld(), context.getPos());
    }

    private BlockState createState(World world, BlockPos pos) {
        BlockState state = this.getDefaultState();
        for (Map.Entry<Direction, EnumProperty<ConnectionType>> entry : DIRECTIONS.entrySet()) {
            BlockPos neighborPos = pos.offset(entry.getKey());
            boolean canConnect = this.canConnect(world, neighborPos);
            state = state.with(entry.getValue(), canConnect ? ConnectionType.CONNECTED : ConnectionType.DISCONNECTED);
        }
        return state;
    }

    private boolean canConnect(World world, BlockPos offset) {
        if (!world.isBlockLoaded(offset))
            return false;
        BlockState state = world.getBlockState(offset);
        return state.getBlock() == this;
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
            if (state.get(entry.getValue()) == ConnectionType.CONNECTED)
                shape = VoxelShapes.or(shape, DIR_SHAPES.get(entry.getKey()));
        }
        SHAPE_CACHE.put(state, shape);
        return shape;
    }

    public enum ConnectionType implements IStringSerializable {
        CONNECTED,
        DISCONNECTED;

        private final String name;

        ConnectionType() {
            this.name = this.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
