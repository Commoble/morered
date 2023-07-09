package commoble.morered.plate_blocks;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

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
	
	public static Direction getOutputDirection(BlockState state)
	{
		if (!state.hasProperty(ATTACHMENT_DIRECTION) || !state.hasProperty(ROTATION))
		{
			return Direction.DOWN;
		}
		
		Direction attachmentDirection = state.getValue(ATTACHMENT_DIRECTION);
		int rotationIndex = state.getValue(ROTATION);
		
		return BlockStateUtil.getOutputDirection(attachmentDirection, rotationIndex);
	}
	
	public static BlockState getStateForPlacedGatePlate(BlockState state, BlockPlaceContext context)
	{
		// how do we want to orient the block when we place it?
		// attachment face should be the face that was clicked
		// what about the rotation?
		
		// option A: output faces away from player, inverted when sneaking
		// empirical observations: this is super annoying to get the player in the right standing position,
		// especially when placing the thing on a wall
		
		// option B: the orientation depends on which part of the face was clicked, not the player's facing
		// this gives more control to the player but might be confusing
		// we may want to render a preview of the placement somehow

		BlockPos placePos = context.getClickedPos();
		Direction faceOfAdjacentBlock = context.getClickedFace();
		Direction directionTowardAdjacentBlock = faceOfAdjacentBlock.getOpposite();
		Vec3 relativeHitVec = context.getClickLocation().subtract(Vec3.atLowerCornerOf(placePos));
		return getStateForPlacedGatePlate(state, placePos, directionTowardAdjacentBlock, relativeHitVec); 
	}
	
	public static BlockState getStateForPlacedGatePlate(BlockState state, BlockPos placePos, Direction directionTowardAdjacentBlock, Vec3 relativeHitVec)
	{
		Direction outputDirection = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, directionTowardAdjacentBlock);
		int rotationIndex = BlockStateUtil.getRotationIndexForDirection(directionTowardAdjacentBlock, outputDirection);
		
		if (state.hasProperty(ATTACHMENT_DIRECTION) && state.hasProperty(ROTATION))
		{
			return state.setValue(ATTACHMENT_DIRECTION, directionTowardAdjacentBlock).setValue(ROTATION, rotationIndex);
		}
		else
		{
			return state;
		}
	}
}
