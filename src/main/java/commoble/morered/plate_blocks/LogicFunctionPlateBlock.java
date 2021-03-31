package commoble.morered.plate_blocks;

import java.util.Random;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public abstract class LogicFunctionPlateBlock extends RedstonePlateBlock
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final int OUTPUT_STRENGTH = RedstonePlateBlock.OUTPUT_STRENGTH;
	public static final int TICK_DELAY = RedstonePlateBlock.TICK_DELAY;
	
	@FunctionalInterface
	public static interface LogicFunctionPlateBlockFactory
	{	
		public LogicFunctionPlateBlock makeBlock(LogicFunction function, AbstractBlock.Properties properties);
	}
	
	public static final LogicFunctionPlateBlockFactory THREE_INPUTS = getBlockFactory(InputSide.A, InputSide.B, InputSide.C);
	public static final LogicFunctionPlateBlockFactory T_INPUTS = getBlockFactory(InputSide.A, InputSide.C);
	public static final LogicFunctionPlateBlockFactory LINEAR_INPUT = getBlockFactory(InputSide.B);

	
	public static LogicFunctionPlateBlockFactory getBlockFactory(InputSide... inputs)
	{
		return (properties, function) -> new LogicFunctionPlateBlock(function, properties)
		{
			// fillStateContainer in LogicGatePlateBlock needs to know which blockstate properties to use
			// but fillStateContainer gets called in the superconstructor, before any information about
			// our block is available.
			// The only safe way to handle this (aside from just making subclasses) is this
			// cursed closure class
			@Override
			public InputSide[] getInputSides()
			{
				return inputs;
			}
		};
	}
	
	private final LogicFunction function;
	
	public LogicFunctionPlateBlock(Properties properties, LogicFunction function)
	{
		super(properties);
		this.function = function;
		
		BlockState baseState = this.defaultBlockState();
		
		for (InputSide side : this.getInputSides())
		{
			baseState = baseState.setValue(side.property, false);
		}
		
		this.registerDefaultState(baseState);
	}
	
	/**
	 * We *have* to make this a method instead of a constructor arg,
	 * because fillStateContainer gets called in the superconstructor
	 * so we can't set any fields in this class before it gets called
	 * 
	 * @return The InputSides this block uses to get redstone input from
	 */
	@Override
	public abstract InputSide[] getInputSides();

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		for (InputSide side : this.getInputSides())
		{
			builder.add(side.property);
		}
	}
	
	@Deprecated
	@Override
	public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		if (InputState.getInput(blockState).applyLogic(this.function)
			&& PlateBlockStateProperties.getOutputDirection(blockState) == sideOfAdjacentBlock.getOpposite())
		{
			return OUTPUT_STRENGTH;
		}
		else
		{
			return 0;
		}
	}

	@Override
	public void tick(BlockState oldBlockState, ServerWorld world, BlockPos pos, Random rand)
	{
		BlockState newBlockState = InputState.getUpdatedBlockState(world, oldBlockState, pos);
		if (newBlockState != oldBlockState)
		{
			world.setBlock(pos, newBlockState, 2);
		}
	}



	
	@Override
	public void notifyNeighbors(World world, BlockPos pos, BlockState state)
	{
		Direction outputDirection = PlateBlockStateProperties.getOutputDirection(state);
		BlockPos outputPos = pos.relative(outputDirection);
		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), java.util.EnumSet.of(outputDirection), false).isCanceled())
		{
			world.neighborChanged(outputPos, this, pos);
			world.updateNeighborsAtExceptFromFacing(outputPos, this, outputDirection.getOpposite());
		}
	}

	
}
