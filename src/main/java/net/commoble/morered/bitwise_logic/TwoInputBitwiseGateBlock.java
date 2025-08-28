package net.commoble.morered.bitwise_logic;

import java.util.List;

import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.BitwiseLogicFunction;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TwoInputBitwiseGateBlock extends BitwiseGateBlock
{
	public TwoInputBitwiseGateBlock(Properties properties, BitwiseLogicFunction operator)
	{
		super(properties, operator);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.TWO_INPUT_BITWISE_GATE_BLOCK_ENTITY.get().create(pos, state);
	}

	@Override
	protected List<Direction> getInputDirections(BlockState thisState)
	{
		Direction attachmentDir = thisState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		int rotationIndex = thisState.getValue(PlateBlockStateProperties.ROTATION);
		Direction inputSideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction inputSideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		return List.of(inputSideA, inputSideC);
	}
}
