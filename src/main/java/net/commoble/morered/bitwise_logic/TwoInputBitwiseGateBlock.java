package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.Receiver;
import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TwoInputBitwiseGateBlock extends BitewiseGateBlock
{
	public TwoInputBitwiseGateBlock(Properties properties, LogicFunction operator)
	{
		super(properties, operator);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().twoInputBitwiseGateBeType.get().create(pos, state);
	}

	@Override
	public @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace, Channel channel)
	{
		if (!(level.getBlockEntity(receiverPos) instanceof TwoInputBitwiseGateBlockEntity gate))
			return null;
		
		Direction attachmentDir = receiverState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		if (receiverSide != attachmentDir || connectedFace.attachmentSide() != attachmentDir)
			return null;
		
		BlockPos wirePos = connectedFace.pos();
		@Nullable Direction directionToNeighbor = DirectionHelper.getDirectionToNeighborPos(receiverPos, wirePos);
		int rotationIndex = receiverState.getValue(PlateBlockStateProperties.ROTATION);
		Direction inputSideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction inputSideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		
		if (directionToNeighbor == inputSideA)
		{
			return gate.getReceiverEndpoints(InputSide.A, channel);
		}
		else if (directionToNeighbor == inputSideC)
		{
			return gate.getReceiverEndpoints(InputSide.C, channel);
		}
		else
			return null;
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

	@Override
	public Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel)
	{
		return level.getBlockEntity(receiverPos) instanceof TwoInputBitwiseGateBlockEntity gate
			? gate.getAllReceivers(channel)
			: List.of();
	}
}
