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

public class LogicFunctionPlateBlock extends RedstonePlateBlock
{
	public static final EnumProperty<Direction> ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final int OUTPUT_STRENGTH = RedstonePlateBlock.OUTPUT_STRENGTH;
	public static final int TICK_DELAY = RedstonePlateBlock.TICK_DELAY;
	
	@FunctionalInterface
	public static interface LogicFunctionPlateBlockFactory
	{	
		public LogicFunctionPlateBlock makeBlock(BlockBehaviour.Properties properties, LogicFunction function);
	}
	
	public static final LogicFunctionPlateBlockFactory THREE_INPUTS = getBlockFactory(InputSide.A, InputSide.B, InputSide.C);
	public static final LogicFunctionPlateBlockFactory T_INPUTS = getBlockFactory(InputSide.A, InputSide.C);
	public static final LogicFunctionPlateBlockFactory LINEAR_INPUT = getBlockFactory(InputSide.B);

	
	public static LogicFunctionPlateBlockFactory getBlockFactory(InputSide... inputs)
	{
		return (properties, function) -> new LogicFunctionPlateBlock(properties, function, inputs);
	}
	
	private final InputSide[] inputs;
	private final LogicFunction function;
	
	public LogicFunctionPlateBlock(Properties properties, LogicFunction function, InputSide[] inputs)
	{
		this.function = function;
		this.inputs = inputs;
		super(properties);
		
		BlockState baseState = this.defaultBlockState();
		
		for (InputSide side : this.getInputSides())
		{
			baseState = baseState.setValue(side.property, false);
		}
		
		this.registerDefaultState(baseState);
	}
	
	@Override
	public InputSide[] getInputSides()
	{
		return this.inputs;
	}

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
