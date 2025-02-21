package niv.heater;

import java.util.ArrayList;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class HeatPipeBlock extends Block implements Waterloggable {

    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final BooleanProperty[] FACING_PROPERTIES = new BooleanProperty[] {
            DOWN, UP, NORTH, SOUTH, WEST, EAST };

    private static final VoxelShape CORE;
    private static final VoxelShape[] PIPE_ARM;
    // private static final VoxelShape[] PIPE_CONNECTOR;

    static {
        CORE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
        PIPE_ARM = new VoxelShape[] {
                Block.createCuboidShape(5, 0, 5, 11, 5, 11),
                Block.createCuboidShape(5, 11, 5, 11, 16, 11),
                Block.createCuboidShape(5, 5, 0, 11, 11, 5),
                Block.createCuboidShape(5, 5, 11, 11, 11, 16),
                Block.createCuboidShape(0, 5, 5, 5, 11, 11),
                Block.createCuboidShape(11, 5, 5, 16, 11, 11),
        };
        // PIPE_CONNECTOR = new VoxelShape[] {
        //         Block.createCuboidShape(4, 0, 4, 12, 2, 12),
        //         Block.createCuboidShape(4, 14, 4, 12, 16, 12),
        //         Block.createCuboidShape(4, 4, 0, 12, 12, 2),
        //         Block.createCuboidShape(4, 4, 14, 12, 12, 16),
        //         Block.createCuboidShape(0, 4, 4, 2, 12, 12),
        //         Block.createCuboidShape(14, 4, 4, 2, 12, 12)
        // };
    }

    public HeatPipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(stateManager.getDefaultState()
                .with(DOWN, false)
                .with(UP, false)
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(WEST, false)
                .with(EAST, false)
                .with(WATERLOGGED, false));
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return !state.get(WATERLOGGED).booleanValue();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        var shapes = new ArrayList<VoxelShape>(6);
        for (var direction : Direction.values()) {
            if (isConnected(state, direction)) {
                shapes.add(PIPE_ARM[direction.getId()]);
            }
        }
        return VoxelShapes.union(CORE, shapes.toArray(VoxelShape[]::new));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.get(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getStill(false);
        }
        return super.getFluidState(state);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        var state = super.getPlacementState(ctx);
        if (world.getFluidState(pos).getFluid() == Fluids.WATER) {
            state = state.with(WATERLOGGED, true);
        }
        for (var direction : Direction.values()) {
            if (canConnect(world, pos.offset(direction))) {
                state = state.with(getProperty(direction), true);
            }
        }
        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED).booleanValue()) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return state.with(getProperty(direction), canConnect(world, neighborPos));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    private boolean canConnect(WorldAccess world, BlockPos pos) {
        var block = world.getBlockState(pos).getBlock();
        return block instanceof HeatPipeBlock || block instanceof AbstractFurnaceBlock;
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.get(getProperty(direction)).booleanValue();
    }

    public static Direction[] getConnected(BlockState state) {
        var directions = new ArrayList<>(6);
        for (var direction : Direction.values()) {
            if (isConnected(state, direction)) {
                directions.add(direction);
            }
        }
        return directions.toArray(Direction[]::new);
    }

    public static BooleanProperty getProperty(Direction direction) {
        return FACING_PROPERTIES[direction.getId()];
    }

}
