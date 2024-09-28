package net.commoble.morered.wire_post;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.ExperimentalModEvents;
import net.commoble.morered.future.TransmissionNode;
import net.commoble.morered.util.EightGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class AbstractPostBlock extends Block
{
	// this gives the block 48 states (6*8)
	// we could optimize it to 24 (3*8) by having the facing be up/down/sideways and using the transform to handle sideways facing,
	// but that makes the logic a lot grittier and we'd have to refactor the blockstate files
	public static final DirectionProperty DIRECTION_OF_ATTACHMENT = BlockStateProperties.FACING;
	public static final EnumProperty<OctahedralGroup> TRANSFORM = EightGroup.TRANSFORM;

	public AbstractPostBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(DIRECTION_OF_ATTACHMENT, Direction.DOWN)
			.setValue(TRANSFORM, OctahedralGroup.IDENTITY));
	}
	
//	protected abstract void notifyNeighbors(Level world, BlockPos pos, BlockState state);
	public abstract Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face);

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DIRECTION_OF_ATTACHMENT, TRANSFORM);
	}

	@Override
	@Deprecated
	public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		// we override this to ensure the correct context is used instead of the dummy context
		return this.hasCollision ? state.getShape(worldIn, pos, context) : Shapes.empty();
	}

	@Override
	@Deprecated
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		this.updatePostSet(world, pos, Set<BlockPos>::add);
		super.onPlace(state, world, pos, oldState, isMoving);
		world.gameEvent(ExperimentalModEvents.WIRE_UPDATE, pos, Context.of(state));
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity()))
		{
			if (level.getBlockEntity(pos) instanceof WirePostBlockEntity be)
			{
				be.clearRemoteConnections();
			}
			this.updatePostSet(level, pos, Set<BlockPos>::remove);
			level.removeBlockEntity(pos);
//			level.gameEvent(ExperimentalModEvents.WIRE_UPDATE, pos, Context.of(state));
		}
	}
	
	public void updatePostSet(Level world, BlockPos pos, BiPredicate<Set<BlockPos>, BlockPos> setFunction)
	{
		LevelChunk chunk = world.getChunkAt(pos);
		if (chunk != null)
		{
			Set<BlockPos> set = chunk.getData(MoreRed.get().postsInChunkAttachment.get());
			boolean setUpdated = setFunction.test(set, pos);
			if (setUpdated && world instanceof ServerLevel serverLevel)
			{
				chunk.setUnsaved(true);
				ChunkPos chunkPos = chunk.getPos();
				PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, new SyncPostsInChunkPacket(chunkPos, set));
			}
		}
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockState defaultState = this.defaultBlockState();
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		
		for (Direction direction : context.getNearestLookingDirections())
		{
			BlockState checkState = defaultState.setValue(DIRECTION_OF_ATTACHMENT, direction);
			if (checkState != null && checkState.canSurvive(world, pos))
			{
				return world.isUnobstructed(checkState, pos, CollisionContext.empty())
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
		BlockState newState = state.setValue(DIRECTION_OF_ATTACHMENT, rot.rotate(state.getValue(DIRECTION_OF_ATTACHMENT)));
		newState = EightGroup.rotate(newState, rot);
		return newState;
	}

	@Deprecated
	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		Direction oldFacing = state.getValue(DIRECTION_OF_ATTACHMENT);
		Direction newFacing = mirrorIn.getRotation(oldFacing).rotate(oldFacing);
		BlockState newState = state.setValue(DIRECTION_OF_ATTACHMENT, newFacing);
		newState = EightGroup.mirror(newState, mirrorIn);
		return newState;
	}
}
