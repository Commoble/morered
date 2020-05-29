package com.github.commoble.morered;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * The three input booleanproperties here are the inputs 90, 180, and 270 degrees clockwise from the output
 * e.g.
 * 
 		OUTPUT
		/-----\
		|     |
	C	|     | A
		|     |
		\-----/
		   B
 * For consistency, logic plates with fewer than three inputs still use the input property for the appropriate side
 * (a gate with one input opposite from the output should use the input B property)
 */
public class PlateBlockStateProperties
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = BlockStateProperties.FACING;
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0,3);
	
	/** The first input clockwise from the output **/
	public static final BooleanProperty INPUT_A = BooleanProperty.create("input_a");
	/** The second input clockwise from the output **/
	public static final BooleanProperty INPUT_B = BooleanProperty.create("input_b");
	/** The third input clockwise from the output **/
	public static final BooleanProperty INPUT_C = BooleanProperty.create("input_c");
	
	public static final Material PLATE_MATERIAL = new Material(MaterialColor.STONE, false, false, false, false, true, false, false, PushReaction.NORMAL);

	
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
		boolean placedAgainstPlate = TagWrappers.LOGIC_GATE_PLATES.contains(adjacentState.getBlock()) && adjacentState.has(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		
		Direction attachmentDirection = placedAgainstPlate ? getDirectionWhenPlacedAgainstAnotherPlate(adjacentState.get(PlateBlockStateProperties.ATTACHMENT_DIRECTION),directionTowardAdjacentBlock): directionTowardAdjacentBlock;
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
