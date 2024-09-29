package net.commoble.morered.bitwise_logic;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.Face;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.PlateBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
	public Map<Channel, BiConsumer<LevelAccessor, Integer>> getReceiverEndpoints(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		Direction attachmentDir = receiverState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		if (receiverSide != attachmentDir || connectedFace.attachmentSide() != attachmentDir || !(level.getBlockEntity(receiverPos) instanceof SingleInputBitwiseGateBlockEntity gate))
		{
			return Map.of();
		}
		Direction inputDirection = PlateBlockStateProperties.getOutputDirection(receiverState).getOpposite();
		BlockPos wirePos = connectedFace.pos();
		@Nullable Direction directionToNeighbor = DirectionHelper.getDirectionToNeighborPos(receiverPos, wirePos);
		if (directionToNeighbor != inputDirection)
		{
			return Map.of();
		}
		
		return gate.getReceiverEndpoints();
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

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack)
	{
		super.setPlacedBy(level, pos, state, entity, stack);
		if (level.getBlockEntity(pos) instanceof SingleInputBitwiseGateBlockEntity gate)
		{
			// force output to set to initial value even if we have no graphable blocks at the inputs
			// (if we do have graphable blocks then they'll update the input at the end of the tick)
			gate.setInput(0, true);
		}
	}
}
