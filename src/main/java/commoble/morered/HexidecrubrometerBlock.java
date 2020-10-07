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

public class HexidecrubrometerBlock extends Block
{
	public static final IntegerProperty POWER = BlockStateProperties.POWER_0_15;
	public static final EnumProperty<AttachFace> READING_FACE = BlockStateProperties.FACE;
	
	// when horizontal, this is the direction that the display faces
	// when vertical, this rotates the display face
	// facing up => north => bottom of number is north
	// facing down => north => top of number is north
	public static final EnumProperty<Direction> ROTATION = BlockStateProperties.HORIZONTAL_FACING;
	

	public HexidecrubrometerBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState()
			.with(POWER,0)
			.with(READING_FACE, AttachFace.WALL)
			.with(ROTATION, Direction.NORTH));
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(POWER, READING_FACE, ROTATION);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		Direction attachDirection = context.getNearestLookingDirection();
		Direction directionAwayFromDisplay = context.getPlacementHorizontalFacing();
		AttachFace attachFace = attachDirection == Direction.DOWN ? AttachFace.FLOOR
			: attachDirection == Direction.UP ? AttachFace.CEILING
			: AttachFace.WALL;
		int power = getInputValue(world, pos, directionAwayFromDisplay);
		return this.getDefaultState()
			.with(READING_FACE, attachFace)
			.with(ROTATION, directionAwayFromDisplay.getOpposite())
			.with(POWER, power);
	}

	// forge method, called when a neighbor calls updateComparatorOutputLevel
	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor)
	{
		if (!world.isRemote() && pos.offset(getReadingDirection(state)).equals(neighbor))
		{
	          state.neighborChanged((World)world, pos, world.getBlockState(neighbor).getBlock(), neighbor, false);
		}
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving)
	{
		int newValue = getInputValue(world,pos,state);
		int oldValue = state.get(POWER);
		if (newValue != oldValue)
		{
			world.setBlockState(pos, state.with(POWER, newValue));
		}
	}
	
	public static int getInputValue(World world, BlockPos pos, BlockState state)
	{
	      Direction direction = getReadingDirection(state);
	      return getInputValue(world,pos,direction);
	}
	
	public static int getInputValue(World world, BlockPos pos, Direction direction)
	{
		BlockPos neighborPos = pos.offset(direction);
		BlockState neighborState = world.getBlockState(neighborPos);
		int comparatorPower = neighborState.getComparatorInputOverride(world, neighborPos);
		int canonicalRedstonePower = world.getRedstonePower(neighborPos, direction);
		int redstonePower = canonicalRedstonePower > 0 ? canonicalRedstonePower
			: neighborState.getBlock() instanceof RedstoneWireBlock ? neighborState.get(BlockStateProperties.POWER_0_15)
			: 0;
		return Math.max(redstonePower, comparatorPower);
	      
	}
	
	// assumes state has the correct properties
	public static Direction getReadingDirection(BlockState state)
	{
		AttachFace face = state.get(READING_FACE);
		Direction directionAwayFromDisplay = state.get(ROTATION).getOpposite();
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
		return state.with(ROTATION, rot.rotate(state.get(ROTATION)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.with(ROTATION, mirror.mirror(state.get(ROTATION)));
	}

}
