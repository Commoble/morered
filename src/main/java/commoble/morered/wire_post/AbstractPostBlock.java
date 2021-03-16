package commoble.morered.wire_post;

import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public abstract class AbstractPostBlock extends Block
{
	public static final DirectionProperty DIRECTION_OF_ATTACHMENT = BlockStateProperties.FACING;

	public AbstractPostBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(DIRECTION_OF_ATTACHMENT, Direction.DOWN));
	}
	
	protected abstract void notifyNeighbors(World world, BlockPos pos, BlockState state);

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DIRECTION_OF_ATTACHMENT);
	}

	@Override
	@Deprecated
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		// we override this to ensure the correct context is used instead of the dummy context
		return this.hasCollision ? state.getShape(worldIn, pos, context) : VoxelShapes.empty();
	}

	@Override
	@Deprecated
	public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		this.updatePostSet(world, pos, Set<BlockPos>::add);
		super.onPlace(state, world, pos, oldState, isMoving);
		this.notifyNeighbors(world, pos, state);
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (state.getBlock() == newState.getBlock())
		{
			// only thing super.onReplaced does is remove the tile entity
			// if the block stays the same, we specifically do NOT remove the tile entity
			// so don't do anything here
		}
		else
		{
			this.updatePostSet(world, pos, Set<BlockPos>::remove);
			super.onRemove(state, world, pos, newState, isMoving);
		}
		this.notifyNeighbors(world, pos, state);
	}
	
	public void updatePostSet(World world, BlockPos pos, BiConsumer<Set<BlockPos>, BlockPos> consumer)
	{
		Chunk chunk = world.getChunkAt(pos);
		if (chunk != null)
		{
			chunk.getCapability(PostsInChunkCapability.INSTANCE)
				.ifPresent(posts -> {
					Set<BlockPos> set = posts.getPositions();
					consumer.accept(set, pos);
					posts.setPositions(set);
				});
		}
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		BlockState defaultState = this.defaultBlockState();
		World world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		
		for (Direction direction : context.getNearestLookingDirections())
		{
			BlockState checkState = defaultState.setValue(DIRECTION_OF_ATTACHMENT, direction);
			if (checkState != null && checkState.canSurvive(world, pos))
			{
				return world.isUnobstructed(checkState, pos, ISelectionContext.empty())
					? checkState
					: null;
			}
		}

		return null;
	}

	@Deprecated
	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.setValue(DIRECTION_OF_ATTACHMENT, rot.rotate(state.getValue(DIRECTION_OF_ATTACHMENT)));
	}

	@Deprecated
	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		return state.rotate(mirrorIn.getRotation(state.getValue(DIRECTION_OF_ATTACHMENT)));
	}

}
