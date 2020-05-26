package com.github.commoble.morered;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class GateBlockStateProperties
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = BlockStateProperties.FACING;
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0,3);

	
	public static Direction getOutputDirection(BlockState state)
	{
		if (!state.has(ATTACHMENT_DIRECTION) || !state.has(ROTATION))
		{
			return Direction.DOWN;
		}
		
		Direction attachmentDirection = state.get(ATTACHMENT_DIRECTION);
		int rotationIndex = state.get(ROTATION);
		
		return BlockStateUtil.getOutputDirection(attachmentDirection, rotationIndex);
	}
	
	public static BlockState getStateForPlacedGatePlate(BlockState state, BlockItemUseContext context)
	{
		Direction faceOfAdjacentBlock = context.getFace();
		// special case: if the player placed this block against the side of another plate block,
		// then place this block facing the same way as that plate block
		// otherwise, place it against the block that was right-clicked0
		Direction directionTowardAdjacentBlock = faceOfAdjacentBlock.getOpposite();
		BlockPos placePos = context.getPos();
		BlockPos adjacentPos = placePos.offset(directionTowardAdjacentBlock);
		BlockState adjacentState = context.getWorld().getBlockState(adjacentPos);
		boolean placedAgainstPlate = TagWrappers.LOGIC_GATE_PLATES.contains(adjacentState.getBlock()) && adjacentState.has(GateBlockStateProperties.ATTACHMENT_DIRECTION);
		
		Direction attachmentDirection = placedAgainstPlate ? getDirectionWhenPlacedAgainstAnotherPlate(adjacentState.get(GateBlockStateProperties.ATTACHMENT_DIRECTION),directionTowardAdjacentBlock): directionTowardAdjacentBlock;
		int rotationIndex = BlockStateUtil.getRotationIndexForPlacement(attachmentDirection, Direction.getFacingDirections(context.getPlayer()));
		if (state.has(ATTACHMENT_DIRECTION) && state.has(ROTATION))
		{
			return state.with(ATTACHMENT_DIRECTION, attachmentDirection).with(ROTATION, rotationIndex);
		}
		else
		{
			return state;
		}
	}
	
	// if we are placing this plate against another plate,
	// and we are NOT placing it against the backside of the other plate,
	// make the plate line up with the plate we are placing against
	public static Direction getDirectionWhenPlacedAgainstAnotherPlate(Direction otherPlateDirection, Direction directionTowardAdjacentBlock)
	{
		if (otherPlateDirection == directionTowardAdjacentBlock.getOpposite())
		{
			return directionTowardAdjacentBlock;
		}
		else
		{
			return otherPlateDirection;
		}
	}
}
