package net.commoble.morered.wire_post;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.mojang.math.OctahedralGroup;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaGameEvents;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.util.EightGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class AbstractPostBlock extends Block implements SimpleWaterloggedBlock
{
	// this gives the block 96 states (6*8*2)
	// we could optimize it to 48 (3*8*2) by having the facing be up/down/sideways and using the transform to handle sideways facing,
	// but that makes the logic a lot grittier and we'd have to refactor the blockstate files
	public static final EnumProperty<Direction> DIRECTION_OF_ATTACHMENT = BlockStateProperties.FACING;
	public static final EnumProperty<OctahedralGroup> TRANSFORM = EightGroup.TRANSFORM;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public AbstractPostBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(DIRECTION_OF_ATTACHMENT, Direction.DOWN)
			.setValue(TRANSFORM, OctahedralGroup.IDENTITY)
			.setValue(WATERLOGGED, true));
	}
	
	protected abstract Map<Channel, Collection<TransmissionNode>> createTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, WirePostBlockEntity post);	

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DIRECTION_OF_ATTACHMENT, TRANSFORM, WATERLOGGED);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		// we override this to ensure the correct context is used instead of the dummy context
		return this.hasCollision ? state.getShape(worldIn, pos, context) : Shapes.empty();
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		updatePostSet(world, pos, Set<BlockPos>::add);
		super.onPlace(state, world, pos, oldState, isMoving);
		ExMachinaGameEvents.scheduleSignalGraphUpdate(world, pos);
	}
	
	

	@Override
	protected void affectNeighborsAfterRemoval(BlockState oldState, ServerLevel level, BlockPos pos, boolean isMoving)
	{
		super.affectNeighborsAfterRemoval(oldState, level, pos, isMoving);
		if (!(level.getBlockState(pos).getBlock() instanceof AbstractPostBlock))
		{
			updatePostSet(level, pos, Set<BlockPos>::remove);
		}
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, @Nullable Orientation orientation, boolean isMoving)
	{
		super.neighborChanged(state, world, pos, blockIn, orientation, isMoving);
		ExMachinaGameEvents.scheduleSignalGraphUpdate(world, pos);
	}
	
	public static void updatePostSet(Level world, BlockPos pos, BiPredicate<Set<BlockPos>, BlockPos> setFunction)
	{
		LevelChunk chunk = world.getChunkAt(pos);
		if (chunk != null)
		{
			Set<BlockPos> set = chunk.getData(MoreRed.POSTS_IN_CHUNK_ATTACHMENT.get());
			boolean setUpdated = setFunction.test(set, pos);
			if (setUpdated && world instanceof ServerLevel serverLevel)
			{
				chunk.markUnsaved();
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
			BlockState checkState = defaultState.setValue(DIRECTION_OF_ATTACHMENT, direction)
				.setValue(
					WATERLOGGED,
					context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
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
	
	public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		if (!(level.getBlockEntity(pos) instanceof WirePostBlockEntity post))
			return List.of();
		
		return post.getTransmissionNodes(levelKey, level, pos, state, channel, () -> this.createTransmissionNodes(levelKey, level, pos, state, post));
	}
	
	@Override
	protected FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState thisState, LevelReader level, ScheduledTickAccess ticks, BlockPos thisPos, Direction directionToNeighbor, BlockPos neighborPos,
		BlockState neighborState, RandomSource rand)
	{
		if (thisState.getValue(WATERLOGGED))
		{
			ticks.scheduleTick(thisPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(thisState, level, ticks, thisPos, directionToNeighbor, neighborPos, neighborState, rand);
	}
}
