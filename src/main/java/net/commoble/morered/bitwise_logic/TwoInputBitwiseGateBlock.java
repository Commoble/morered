package net.commoble.morered.bitwise_logic;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.Face;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
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
	public Map<Channel, BiConsumer<LevelAccessor, Integer>> getReceiverEndpoints(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		if (!(level.getBlockEntity(receiverPos) instanceof TwoInputBitwiseGateBlockEntity gate))
			return Map.of();
		
		Direction attachmentDir = receiverState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		if (receiverSide != attachmentDir || connectedFace.attachmentSide() != attachmentDir)
			return Map.of();
		
		BlockPos wirePos = connectedFace.pos();
		@Nullable Direction directionToNeighbor = DirectionHelper.getDirectionToNeighborPos(receiverPos, wirePos);
		int rotationIndex = receiverState.getValue(PlateBlockStateProperties.ROTATION);
		Direction inputSideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction inputSideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		
		if (directionToNeighbor == inputSideA)
		{
			return gate.getReceiverEndpoints(InputSide.A);
		}
		else if (directionToNeighbor == inputSideC)
		{
			return gate.getReceiverEndpoints(InputSide.C);
		}
		else
			return Map.of();
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
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack)
	{
		super.setPlacedBy(level, pos, state, entity, stack);
		if (level.getBlockEntity(pos) instanceof TwoInputBitwiseGateBlockEntity gate)
		{
			// force output to set to initial value even if we have no graphable blocks at the inputs
			// (if we do have graphable blocks then they'll update the input at the end of the tick)
			gate.setClockwiseInput(0, true);
			gate.setCounterclockwiseInput(0, true);
		}
	}
}
