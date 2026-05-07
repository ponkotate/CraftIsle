package org.ponkotate.craftisle.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PebbleBlock extends Block {

    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);

    private static final VoxelShape SHAPE_1 = Block.box(3, 0, 4, 9, 2, 9);
    private static final VoxelShape SHAPE_2 = Shapes.or(
        Block.box(4, 0, 4, 8, 2, 9),
        Block.box(9, 0, 8, 13, 2, 12)
    );
    private static final VoxelShape SHAPE_3 = Shapes.or(
        Block.box(3, 0, 4, 8, 2, 9),
        Block.box(9, 0, 7, 13, 2, 12),
        Block.box(6, 2, 6, 10, 4, 10)
    );
    private static final VoxelShape SHAPE_4 = Shapes.or(
        Block.box(1, 0, 3, 7, 3, 8),
        Block.box(9, 0, 4, 13, 2, 8),
        Block.box(5, 0, 9, 12, 2, 14),
        Block.box(4, 2, 5, 10, 4, 10)
    );
    private static final VoxelShape[] SHAPES = { SHAPE_1, SHAPE_2, SHAPE_3, SHAPE_4 };

    public PebbleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COUNT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COUNT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(COUNT) - 1];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
