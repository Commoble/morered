package commoble.morered.plate_blocks;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class LatchBlock extends RedstonePlateBlock
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final BooleanProperty INPUT_A = PlateBlockStateProperties.INPUT_A;
	public static final BooleanProperty INPUT_C = PlateBlockStateProperties.INPUT_C;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED; 
	public static final InputSide[] INPUT_SIDES = {InputSide.A, InputSide.C};
	public static final int OUTPUT_STRENGTH = RedstonePlateBlock.OUTPUT_STRENGTH;
	public static final int TICK_DELAY = RedstonePlateBlock.TICK_DELAY;

	public LatchBlock(Properties properties)
	{
		super(properties);
		BlockState baseState = this.defaultBlockState();
		this.registerDefaultState(baseState
			.setValue(INPUT_A, false)
			.setValue(INPUT_C, false)
			.setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(INPUT_A, INPUT_C, POWERED);
		
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockState state = super.getStateForPlacement(context);
		if (state.getValue(INPUT_C))
		{
			state = state.setValue(POWERED, true);
		}
		return state;
	}

	@Deprecated
	@Override
	public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		// if both inputs are active, both outputs are off
		if (state.getValue(INPUT_A) && state.getValue(INPUT_C))
		{
			return 0;
		}
		
		boolean powered = state.getValue(POWERED);
		Direction outputDirectionWhenPowered = PlateBlockStateProperties.getOutputDirection(state);
		return (powered && sideOfAdjacentBlock == outputDirectionWhenPowered.getOpposite()
			|| !powered && sideOfAdjacentBlock == outputDirectionWhenPowered)
			? 15
			: 0;
	}

	@Override
	public void tick(BlockState oldBlockState, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		BlockState stateWithNewInput = InputState.getUpdatedBlockState(level, oldBlockState, pos);
		InputState newInputState = InputState.getWorldPowerState(level, stateWithNewInput, pos);
		boolean wasPowered = oldBlockState.getValue(POWERED);
		boolean isPowered = ((wasPowered && !newInputState.c) || (!wasPowered && newInputState.a));
		BlockState newBlockState = stateWithNewInput.setValue(POWERED, isPowered);
		if (newBlockState != oldBlockState)
		{
			level.setBlock(pos, newBlockState, 2);
		}
	}
	
	@Override
	public InputSide[] getInputSides()
	{
		return INPUT_SIDES;
	}
	
	@Override
	public EnumSet<Direction> getOutputSides(Level level, BlockPos pos, BlockState state)
	{
		Direction primaryDirection = PlateBlockStateProperties.getOutputDirection(state);
		Direction oppositeDirection = primaryDirection.getOpposite();
		return EnumSet.of(primaryDirection, oppositeDirection);
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side)
	{
		// latch blocks have two outputs
		// the superclass checks side == outputDirection.getOpposite() (because the given side is the side of the neighbor block)
		// so we also check outputDirection here to see if it's the neighbor on the other side
		return super.canConnectRedstone(state, world, pos, side)
			|| (side != null && side == PlateBlockStateProperties.getOutputDirection(state));
	}
}
