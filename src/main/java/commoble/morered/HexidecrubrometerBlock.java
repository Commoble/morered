package commoble.morered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class HexidecrubrometerBlock extends Block
{
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	public static final EnumProperty<AttachFace> READING_FACE = BlockStateProperties.ATTACH_FACE;
	
	// when horizontal, this is the direction that the display faces
	// when vertical, this rotates the display face
	// facing up => north => bottom of number is north
	// facing down => north => top of number is north
	public static final EnumProperty<Direction> ROTATION = BlockStateProperties.HORIZONTAL_FACING;
	

	public HexidecrubrometerBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(POWER,0)
			.setValue(READING_FACE, AttachFace.WALL)
			.setValue(ROTATION, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(POWER, READING_FACE, ROTATION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction attachDirection = context.getNearestLookingDirection();
		Direction horizontalDirectionAwayFromPlayer = context.getHorizontalDirection();
		AttachFace attachFace = attachDirection == Direction.DOWN ? AttachFace.FLOOR
			: attachDirection == Direction.UP ? AttachFace.CEILING
			: AttachFace.WALL;
		Direction readingDirection = getReadingDirection(attachFace, horizontalDirectionAwayFromPlayer);
		int power = getInputValue(world, pos, readingDirection);
		return this.defaultBlockState()
			.setValue(READING_FACE, attachFace)
			.setValue(ROTATION, horizontalDirectionAwayFromPlayer.getOpposite())
			.setValue(POWER, power);
	}

	// called on the client and server when this block is added to the world
	// we want to make sure we update our power after this to react to changes in nearby blocks
	@Override
	@Deprecated
	public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		if (oldState.getBlock() != this)
		{
			int oldPower = state.getValue(POWER);
			int newPower = getInputValue(worldIn,pos,state);
			if (newPower != oldPower)
			{
				worldIn.setBlockAndUpdate(pos, state.setValue(POWER, newPower));
			}
		}
		super.onPlace(state, worldIn, pos, oldState, isMoving);
	}

	// forge method, called when a neighbor calls updateComparatorOutputLevel
	@Override
	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor)
	{
		if (world instanceof ServerLevel serverLevel && pos.relative(getReadingDirection(state)).equals(neighbor))
		{
	          state.handleNeighborChanged(serverLevel, pos, world.getBlockState(neighbor).getBlock(), neighbor, false);
		}
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving)
	{
		int newValue = getInputValue(world,pos,state);
		int oldValue = state.getValue(POWER);
		if (newValue != oldValue)
		{
			world.setBlockAndUpdate(pos, state.setValue(POWER, newValue));
		}
	}
	
	public static int getInputValue(Level world, BlockPos pos, BlockState state)
	{
	      Direction direction = getReadingDirection(state);
	      return getInputValue(world,pos,direction);
	}
	
	public static int getInputValue(Level world, BlockPos pos, Direction direction)
	{
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = world.getBlockState(neighborPos);
		int comparatorPower = neighborState.getAnalogOutputSignal(world, neighborPos);
		int canonicalRedstonePower = world.getSignal(neighborPos, direction);
		int redstonePower = canonicalRedstonePower > 0 ? canonicalRedstonePower
			: neighborState.getBlock() instanceof RedStoneWireBlock ? neighborState.getValue(BlockStateProperties.POWER)
			: 0;
		return Math.max(redstonePower, comparatorPower);
	      
	}
	
	// assumes state has the correct properties
	public static Direction getReadingDirection(BlockState state)
	{
		AttachFace face = state.getValue(READING_FACE);
		Direction directionAwayFromDisplay = state.getValue(ROTATION).getOpposite();
		return getReadingDirection(face, directionAwayFromDisplay);
	}
	
	public static Direction getReadingDirection(AttachFace attachFace, Direction directionAwayFromDisplay)
	{
		switch(attachFace)
		{
			case CEILING:
				return Direction.UP;
			case FLOOR:
				return Direction.DOWN;
			case WALL:
				return directionAwayFromDisplay;
			default:
				return Direction.DOWN;
		}
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side)
	{
		return side != null && side.getOpposite() == getReadingDirection(state);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.setValue(ROTATION, rot.rotate(state.getValue(ROTATION)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION)));
	}

}
