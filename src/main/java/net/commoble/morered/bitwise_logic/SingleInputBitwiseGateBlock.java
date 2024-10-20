package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.Receiver;
import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.PlateBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SingleInputBitwiseGateBlock extends BitewiseGateBlock
{
	public SingleInputBitwiseGateBlock(Properties properties, LogicFunction operator)
	{
		super(properties, operator);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().singleInputBitwiseGateBeType.get().create(pos, state);
	}
	
	@Override
	public @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace, Channel channel)
	{
		Direction attachmentDir = receiverState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		if (receiverSide != attachmentDir || connectedFace.attachmentSide() != attachmentDir || !(level.getBlockEntity(receiverPos) instanceof SingleInputBitwiseGateBlockEntity gate))
		{
			return null;
		}
		Direction inputDirection = PlateBlockStateProperties.getOutputDirection(receiverState).getOpposite();
		BlockPos wirePos = connectedFace.pos();
		@Nullable Direction directionToNeighbor = DirectionHelper.getDirectionToNeighborPos(receiverPos, wirePos);
		if (directionToNeighbor != inputDirection)
		{
			return null;
		}
		
		return gate.getReceiverEndpoint(channel);
	}

	@Override
	public Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel)
	{
		return level.getBlockEntity(receiverPos) instanceof SingleInputBitwiseGateBlockEntity gate
			? gate.getAllReceivers(channel)
			: List.of();
	}

	public boolean canConnectToAdjacentCable(@Nonnull BlockGetter level, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire)
	{
		Direction plateAttachmentDir = thisState.getValue(PlateBlock.ATTACHMENT_DIRECTION);
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(thisState);
		return plateAttachmentDir == wireFace &&
			(directionToWire == primaryOutputDirection || directionToWire == primaryOutputDirection.getOpposite());
	}

	@Override
	protected List<Direction> getInputDirections(BlockState thisState)
	{
		return List.of(PlateBlockStateProperties.getOutputDirection(thisState).getOpposite());
	}	
}
