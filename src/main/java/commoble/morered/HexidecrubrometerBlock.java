package commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

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
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getLevel();
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
	public void onPlace(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
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
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor)
	{
		if (!world.isClientSide() && pos.relative(getReadingDirection(state)).equals(neighbor))
		{
	          state.neighborChanged((World)world, pos, world.getBlockState(neighbor).getBlock(), neighbor, false);
		}
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving)
	{
		int newValue = getInputValue(world,pos,state);
		int oldValue = state.getValue(POWER);
		if (newValue != oldValue)
		{
			world.setBlockAndUpdate(pos, state.setValue(POWER, newValue));
		}
	}
	
	public static int getInputValue(World world, BlockPos pos, BlockState state)
	{
	      Direction direction = getReadingDirection(state);
	      return getInputValue(world,pos,direction);
	}
	
	public static int getInputValue(World world, BlockPos pos, Direction direction)
	{
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = world.getBlockState(neighborPos);
		int comparatorPower = neighborState.getAnalogOutputSignal(world, neighborPos);
		int canonicalRedstonePower = world.getSignal(neighborPos, direction);
		int redstonePower = canonicalRedstonePower > 0 ? canonicalRedstonePower
			: neighborState.getBlock() instanceof RedstoneWireBlock ? neighborState.getValue(BlockStateProperties.POWER)
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
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, Direction side)
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
