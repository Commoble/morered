package net.commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.ExperimentalModEvents;
import net.commoble.morered.future.Face;
import net.commoble.morered.future.SignalStrength;
import net.commoble.morered.future.TransmissionNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractPoweredWirePostBlock extends AbstractPostBlock implements EntityBlock
{
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	public static final EnumSet<Direction> NO_DIRECTIONS = EnumSet.noneOf(Direction.class);
	
	protected static final VoxelShape[] POST_SHAPES_DUNSWE = {
		Block.box(6D, 0D, 6D, 10D, 10D, 10D),
		Block.box(6D, 6D, 6D, 10D, 16D, 10D),
		Block.box(6D, 6D, 0D, 10D, 10D, 10D),
		Block.box(6D, 6D, 6D, 10D, 10D, 16D),
		Block.box(0D, 6D, 6D, 10D, 10D, 10D),
		Block.box(6D, 6D, 6D, 16D, 10D, 10D)
	};
	
	// function that gets the connected directions for a given blockstate
	private final Function<BlockState, EnumSet<Direction>> connectionGetter;
	private final boolean connectsToAttachedBlock;

	public boolean connectsToAttachedBlock() { return this.connectsToAttachedBlock; }

	public AbstractPoweredWirePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter, boolean connectsToAttachedBlock)
	{
		super(properties);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(POWER, 0));
		this.connectionGetter = connectionGetter;
		this.connectsToAttachedBlock = connectsToAttachedBlock;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().redwirePostBeType.get().create(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(POWER);
	}
	
	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		world.gameEvent(ExperimentalModEvents.WIRE_UPDATE, pos, Context.of(state));
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		// direct-connection posts need to update nodes if neighbor cube updates
		if (this.connectsToAttachedBlock
			&& pos.relative(state.getValue(DIRECTION_OF_ATTACHMENT)).equals(fromPos)
			&& world.getBlockEntity(pos) instanceof WirePostBlockEntity post)
		{
			post.clearTransmissionNodes();
		}
		super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
		world.gameEvent(ExperimentalModEvents.WIRE_UPDATE, pos, Context.of(state));

	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if state is replacing without changing the block, clear node cache
		if (newState.is(this) && newState != state && level.getBlockEntity(pos) instanceof WirePostBlockEntity post)
		{
			post.clearTransmissionNodes();
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/**
	 * Can this block provide power. Only wire currently seems to have this change
	 * based on its state.
	 * 
	 * @param state A blockstate of this block
	 */
	@Deprecated
	@Override
	public boolean isSignalSource(BlockState state)
	{
		return true;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side)
	{
		return side != null && this.getParallelDirections(state).contains(side.getOpposite());
	}
	
	@Deprecated
	@Override
	public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction directionOfThisBlockFromCaller)
	{
		if (this.getParallelDirections(blockState).contains(directionOfThisBlockFromCaller.getOpposite()))
		{
			return blockState.getValue(POWER);
		}
		else if (this.connectsToAttachedBlock && directionOfThisBlockFromCaller.getOpposite() == blockState.getValue(DIRECTION_OF_ATTACHMENT))
		{
			return blockState.getValue(POWER);
		}
		else
		{
			return 0;
		}
	}

	// get the power that can be conducted through solid blocks
	@Deprecated
	@Override
	public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction directionFromNeighbor)
	{
		if (this.connectsToAttachedBlock && directionFromNeighbor.getOpposite() == state.getValue(DIRECTION_OF_ATTACHMENT))
		{
			return state.getValue(POWER);
		}
		else
		{
			return 0;
		}
	}
	
	public EnumSet<Direction> getParallelDirections(BlockState state)
	{
		return this.connectionGetter.apply(state);
	}
	
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		if (face != state.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
			return Map.of();
		if (!(level.getBlockEntity(pos) instanceof WirePostBlockEntity post))
			return Map.of();
		
		return post.getTransmissionNodes(level, pos, state, this, face);
	}
}
