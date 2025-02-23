package net.commoble.morered.bitwise_logic;

import java.util.List;

import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.BitwiseLogicFunction;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SingleInputBitwiseGateBlock extends BitwiseGateBlock
{
	public SingleInputBitwiseGateBlock(Properties properties, BitwiseLogicFunction operator)
	{
		super(properties, operator);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().singleInputBitwiseGateBeType.get().create(pos, state);
	}

	@Override
	protected List<Direction> getInputDirections(BlockState thisState)
	{
		return List.of(PlateBlockStateProperties.getOutputDirection(thisState).getOpposite());
	}
}
