package commoble.morered.plate_blocks;

import java.util.EnumSet;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

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
		BlockState baseState = this.getDefaultState();
		this.setDefaultState(baseState
			.with(INPUT_A, false)
			.with(INPUT_C, false)
			.with(POWERED, false));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(INPUT_A, INPUT_C, POWERED);
		
	}
	

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		BlockState state = super.getStateForPlacement(context);
		if (state.get(INPUT_C))
		{
			state = state.with(POWERED, true);
		}
		return state;
	}

	/**
	 * @deprecated call via
	 *             {@link IBlockState#getWeakPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getWeakPower(BlockState state, IBlockReader blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		// if both inputs are active, both outputs are off
		if (state.get(INPUT_A) && state.get(INPUT_C))
		{
			return 0;
		}
		
		boolean powered = state.get(POWERED);
		Direction outputDirectionWhenPowered = PlateBlockStateProperties.getOutputDirection(state);
		return (powered && sideOfAdjacentBlock == outputDirectionWhenPowered.getOpposite()
			|| !powered && sideOfAdjacentBlock == outputDirectionWhenPowered)
			? 15
			: 0;
	}

	@Override
	public void tick(BlockState oldBlockState, ServerWorld world, BlockPos pos, Random rand)
	{
		BlockState stateWithNewInput = InputState.getUpdatedBlockState(world, oldBlockState, pos);
		InputState newInputState = InputState.getWorldPowerState(world, stateWithNewInput, pos);
		boolean wasPowered = oldBlockState.get(POWERED);
		boolean isPowered = ((wasPowered && !newInputState.c) || (!wasPowered && newInputState.a));
		BlockState newBlockState = stateWithNewInput.with(POWERED, isPowered);
		if (newBlockState != oldBlockState)
		{
			world.setBlockState(pos, newBlockState, 2);
		}
	}
	
	@Override
	public InputSide[] getInputSides()
	{
		return INPUT_SIDES;
	}

	
	@Override
	public void notifyNeighbors(World world, BlockPos pos, BlockState state)
	{
		Direction primaryDirection = PlateBlockStateProperties.getOutputDirection(state);
		Direction oppositeDirection = primaryDirection.getOpposite();
		EnumSet<Direction> outputDirections = EnumSet.of(primaryDirection, oppositeDirection);
		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), outputDirections, false).isCanceled())
		{
			for (Direction outputDirection : outputDirections)
			{
				BlockPos outputPos = pos.offset(outputDirection);
				
				{
					world.neighborChanged(outputPos, this, pos);
				}
				world.notifyNeighborsOfStateExcept(outputPos, this, outputDirection);
			}
		}
	}
}
