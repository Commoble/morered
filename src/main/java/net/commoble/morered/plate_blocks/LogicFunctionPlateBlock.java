package net.commoble.morered.plate_blocks;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public abstract class LogicFunctionPlateBlock extends RedstonePlateBlock
{
	public static final EnumProperty<Direction> ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final int OUTPUT_STRENGTH = RedstonePlateBlock.OUTPUT_STRENGTH;
	public static final int TICK_DELAY = RedstonePlateBlock.TICK_DELAY;
	
	@FunctionalInterface
	public static interface LogicFunctionPlateBlockFactory
	{	
		public LogicFunctionPlateBlock makeBlock(LogicFunction function, BlockBehaviour.Properties properties);
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		for (InputSide side : this.getInputSides())
		{
			builder.add(side.property);
		}
	}
	
	@Deprecated
	@Override
	public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
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
	public void tick(BlockState oldBlockState, ServerLevel world, BlockPos pos, RandomSource rand)
	{
		BlockState newBlockState = InputState.getUpdatedBlockState(world, oldBlockState, pos);
		if (newBlockState != oldBlockState)
		{
			world.setBlock(pos, newBlockState, 2);
		}
	}
	
	@Override
	public EnumSet<Direction> getOutputSides(Level level, BlockPos pos, BlockState state)
	{
		return EnumSet.of(PlateBlockStateProperties.getOutputDirection(state));
	}
}
