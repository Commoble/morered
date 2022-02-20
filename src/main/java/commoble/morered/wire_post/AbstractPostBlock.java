package commoble.morered.wire_post;

import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractPostBlock extends Block {
    public static final DirectionProperty DIRECTION_OF_ATTACHMENT = BlockStateProperties.FACING;

    public AbstractPostBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DIRECTION_OF_ATTACHMENT, Direction.DOWN));
    }

    protected abstract void notifyNeighbors(Level world, BlockPos pos, BlockState state);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DIRECTION_OF_ATTACHMENT);
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        // we override this to ensure the correct context is used instead of the dummy context
        return this.hasCollision ? state.getShape(worldIn, pos, context) : Shapes.empty();
    }

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        this.updatePostSet(world, pos, Set::add);
        super.onPlace(state, world, pos, oldState, isMoving);
        this.notifyNeighbors(world, pos, state);
    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock()) {
            // only thing super.onReplaced does is remove the tile entity
            // if the block stays the same, we specifically do NOT remove the tile entity
            // so don't do anything here
        } else {
            this.updatePostSet(world, pos, Set::remove);
            super.onRemove(state, world, pos, newState, isMoving);
        }
        this.notifyNeighbors(world, pos, state);
    }

    public void updatePostSet(Level world, BlockPos pos, BiConsumer<Set<BlockPos>, BlockPos> consumer) {
        LevelChunk chunk = world.getChunkAt(pos);
        chunk.getCapability(PostsInChunkCapability.INSTANCE)
                .ifPresent(posts -> {
                    Set<BlockPos> set = posts.getPositions();
                    consumer.accept(set, pos);
                    posts.setPositions(set);
                });
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState defaultState = this.defaultBlockState();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState checkState = defaultState.setValue(DIRECTION_OF_ATTACHMENT, direction);
            if (checkState != null && checkState.canSurvive(world, pos)) {
                return world.isUnobstructed(checkState, pos, CollisionContext.empty())
                        ? checkState
                        : null;
            }
        }

        return null;
    }

    @Deprecated
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(DIRECTION_OF_ATTACHMENT, rot.rotate(state.getValue(DIRECTION_OF_ATTACHMENT)));
    }

    @Deprecated
    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(DIRECTION_OF_ATTACHMENT)));
    }

}
