package net.commoble.morered.plate_blocks;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.ticks.TickPriority;

public class PulseGateBlock extends RedstonePlateBlock
{
	public static final BooleanProperty INPUT_B = PlateBlockStateProperties.INPUT_B;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final InputSide[] INPUT_SIDES = {InputSide.B};
	public static final int PULSE_DURATION = 2;

	public PulseGateBlock(Properties properties)
	{
		super(properties);
		BlockState baseState = this.defaultBlockState();
		this.registerDefaultState(baseState
			.setValue(INPUT_B, false)
			.setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(INPUT_B, POWERED);
	}

	@Override
	public InputSide[] getInputSides()
	{
		return INPUT_SIDES;
	}

	@Override
	public EnumSet<Direction> getOutputSides(Level level, BlockPos pos, BlockState state)
	{
		return EnumSet.of(PlateBlockStateProperties.getOutputDirection(state));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockState state = super.getStateForPlacement(context);
		if (state.getValue(INPUT_B))
		{
			state = state.setValue(POWERED, true);
		}
		return state;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (this.hasInputPower(level, state, pos))
		{
			level.scheduleTick(pos, this, PULSE_DURATION);
		}
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		// if any inputs changed, schedule a tick
		boolean oldHasInput = state.getValue(INPUT_B);
		boolean newHasInput = InputSide.B.isBlockReceivingPower(worldIn, state, pos);
		// if we started receiving power, activate and schedule a tick
		if (!oldHasInput && newHasInput)
		{
			BlockState newState = state
				.setValue(INPUT_B, true)
				.setValue(POWERED, true);
			worldIn.setBlockAndUpdate(pos, newState);
			if (!worldIn.getBlockTicks().willTickThisTick(pos, this))
			{
				worldIn.scheduleTick(pos, this, PULSE_DURATION, TickPriority.HIGH);
			}
		}
		// otherwise, if we stopped receiving power, cut the input state
		// (wait for the tick to stop outputting power)
		else if (oldHasInput && !newHasInput)
		{
			BlockState newState = state.setValue(INPUT_B, false);
			worldIn.setBlockAndUpdate(pos, newState);
		}
	}

	@Deprecated
	@Override
	public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		boolean powered = state.getValue(POWERED);
		Direction outputDirectionWhenPowered = PlateBlockStateProperties.getOutputDirection(state);
		return (powered && sideOfAdjacentBlock.getOpposite() == outputDirectionWhenPowered)
			? 15
			: 0;
	}

	@Override
	public void tick(BlockState oldBlockState, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		BlockState newBlockState = oldBlockState.setValue(POWERED, false);
		if (newBlockState != oldBlockState)
		{
			level.setBlockAndUpdate(pos, newBlockState);
		}
	}
}
