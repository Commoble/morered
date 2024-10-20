package net.commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.SignalGraphUpdateGameEvent;
import net.commoble.exmachina.api.SignalStrength;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
		return MoreRed.get().wirePostBeType.get().create(pos, state);
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
		SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(world, pos);
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

	protected Map<Direction, Map<Channel, TransmissionNode>> createTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, WirePostBlockEntity post)
	{
		Map<Direction, Map<Channel, TransmissionNode>> allMaps = new HashMap<>();
		for (Direction face : Direction.values())
		{
			Map<Channel, TransmissionNode> map = new HashMap<>();
			Set<Direction> powerReaders = this.connectsToAttachedBlock()
				? Set.of(face)
				: Set.of();
			Set<Direction> parallelDirections = this.getParallelDirections(state);
			Set<Face> connectableNodes = new HashSet<>();
			// add nodes for parallel nodes
			for (Direction directionToNeighbor : parallelDirections)
			{
				BlockPos neighborPos = pos.relative(directionToNeighbor);
				connectableNodes.add(new Face(neighborPos, face));
			}
			if (this.connectsToAttachedBlock())
			{
				// add strong-connection nodes for the attachment face too
				BlockPos neighborPos = pos.relative(face);
				if (level.getBlockState(neighborPos).isRedstoneConductor(level, pos))
				{
					for (Direction directionToCube : Direction.values())
					{
						if (directionToCube == face)
							continue;
						connectableNodes.add(new Face(neighborPos.relative(directionToCube.getOpposite()), directionToCube));
					}
				}
			}
			for (BlockPos remotePos : post.getRemoteConnections())
			{
				BlockState remoteState = level.getBlockState(remotePos);
				if (remoteState.hasProperty(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
				{
					connectableNodes.add(new Face(remotePos, remoteState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT)));
				}
			}
			BiFunction<LevelAccessor, Integer, Map<Direction, SignalStrength>> graphListener = (levelAccess, power) -> {
				// flag 2 syncs block via sendBlockUpdated but does not invoke neighborChanged
				// the graph will invoke neighborChanged later
				// however this will still invoke updateShape...
				// this shouldn't be an issue since updateShape usually doesn't handle signal changes
				levelAccess.setBlock(pos, state.setValue(AbstractPoweredWirePostBlock.POWER, power), Block.UPDATE_CLIENTS);
				Map<Direction, SignalStrength> updateDirs = new HashMap<>();
				for (Direction dir : powerReaders)
				{
					updateDirs.put(dir, SignalStrength.STRONG);
				}
				for (Direction dir : parallelDirections)
				{
					updateDirs.put(dir, SignalStrength.STRONG);
				}
				return updateDirs;
			};
			TransmissionNode node = new TransmissionNode(powerReaders, connectableNodes, graphListener);
			map.put(Channel.redstone(), node);
			allMaps.put(face, map);
		}
		return allMaps;
	}
}
